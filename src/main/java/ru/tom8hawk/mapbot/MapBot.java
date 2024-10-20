package ru.tom8hawk.mapbot;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.tom8hawk.mapbot.model.Feature;
import ru.tom8hawk.mapbot.model.Geometry;
import ru.tom8hawk.mapbot.model.Properties;
import ru.tom8hawk.mapbot.model.User;
import ru.tom8hawk.mapbot.service.FeaturesMapService;
import ru.tom8hawk.mapbot.service.FeatureService;
import ru.tom8hawk.mapbot.service.UserService;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
public class MapBot extends TelegramLongPollingBot {

    @Getter
    private final String botUsername;

    @Getter
    private final String botToken;

    private final UserService userService;
    private final FeatureService featureService;
    private final FeaturesMapService featuresMapService;

    @Autowired
    public MapBot(
            @Value("${telegram.bot.username}") String botUsername,
            @Value("${telegram.bot.token}") String botToken,
            UserService userService,
            FeatureService featureService,
            FeaturesMapService featuresMapService
    ) {

        this.botUsername = botUsername;
        this.botToken = botToken;
        this.userService = userService;
        this.featureService = featureService;
        this.featuresMapService = featuresMapService;
    }

    @PostConstruct
    public void init() throws TelegramApiException {
        new TelegramBotsApi(DefaultBotSession.class).registerBot(this);
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage()) {
                Message message = update.getMessage();

                if (message.getChat().isUserChat()) {
                    String chatId = message.getChatId().toString();
                    Long userTelegramId = message.getFrom().getId();

                    if (message.hasText()) {
                        String messageText = message.getText();

                        if (messageText.equals("/start")) {
                            execute(createSendMessage(chatId, """
                            Привет! С помощью этого бота ты сможешь отмечать листовки на карте 🗺️📍
                            
                            Для отметки листовки просто отправь мне геометку""")
                            );
                        } else if (messageText.equals("/admin") && isAdmin(userTelegramId)) {
                            SendMessage adminMessage = new SendMessage();

                            adminMessage.setChatId(chatId);
                            adminMessage.setText("Данные бота");

                            adminMessage.setReplyMarkup(createKeyboard(
                                    InlineKeyboardButton.builder().text("Импорт").callbackData("import").build(),
                                    InlineKeyboardButton.builder().text("Экспорт").callbackData("export").build()
                            ));

                            execute(adminMessage);
                        } else if (messageText.equals("/showall")) {
                            String link = "https://t.me/" + botUsername + "?startapp=no_cluster";

                            SendMessage linkMessage = new SendMessage();
                            linkMessage.setChatId(chatId);
                            linkMessage.setText("Просмотр карты без кластеризации точек:");

                            linkMessage.setReplyMarkup(createKeyboard(
                                    InlineKeyboardButton.builder().text("Карта").url(link).build()
                            ));

                            execute(linkMessage);
                        }
                    } else if (message.hasLocation()) {
                        String username = message.getFrom().getUserName();
                        User user = userService.findByTelegramId(userTelegramId).orElse(null);

                        if (user == null) {
                            user = new User();
                            user.setTelegramId(userTelegramId);
                            user.setUsername(username);
                            userService.save(user);
                        } else if (username != null && !username.equals(user.getUsername())) {
                            user.setUsername(username);
                            userService.save(user);
                        }

                        double longitude = message.getLocation().getLongitude();
                        double latitude = message.getLocation().getLatitude();

                        Feature feature = new Feature();
                        feature.setCreator(user);

                        Geometry geometry = new Geometry();
                        geometry.setType("Point");
                        geometry.setCoordinates(new double[]{ longitude, latitude });
                        feature.setGeometry(geometry);

                        Properties properties = new Properties();
                        properties.setMarkerColor(MapConstants.MARKER_COLOR);
                        feature.setProperties(properties);

                        featureService.save(feature);
                        featuresMapService.display(feature);

                        SendMessage sendMessage = new SendMessage();
                        sendMessage.setChatId(chatId);
                        sendMessage.setText("Точка сохранена!");

                        sendMessage.setReplyMarkup(createKeyboard(
                                InlineKeyboardButton.builder()
                                        .text("\uD83D\uDDD1 Удалить️")
                                        .callbackData("delete_" + feature.getId())
                                        .build()
                        ));

                        execute(sendMessage);
                    } else if (message.hasDocument() && isAdmin(userTelegramId)) {
                        String fileId = message.getDocument().getFileId();
                        String fileName = message.getDocument().getFileName();

                        if (fileName.endsWith(".json") || fileName.endsWith(".geojson")) {
                            try {
                                String filePath = execute(new GetFile(fileId)).getFilePath();
                                File file = downloadFile(filePath);
                                featuresMapService.importFeatures(file);

                                execute(createSendMessage(chatId, "Файл успешно обработан!"));
                            } catch (IOException e) {
                                e.printStackTrace();
                                execute(createSendMessage(chatId,
                                        "Ошибка при обработке файла! Изменения не были внесены."));
                            }
                        }
                    }
                }
            } else if (update.hasCallbackQuery()) {
                CallbackQuery callback = update.getCallbackQuery();

                String callbackData = callback.getData();
                Long userTelegramId = callback.getFrom().getId();
                Integer messageId = callback.getMessage().getMessageId();
                String chatId = callback.getMessage().getChatId().toString();

                if (callbackData.contains("delete")) {
                    try {
                        Long featureId = Long.parseLong(callbackData.split("_")[1]);
                        Feature feature = featureService.findById(featureId).orElse(null);

                        if (feature != null) {
                            User creator = feature.getCreator();

                            if (creator != null && callback.getFrom().getId().equals(creator.getTelegramId())) {
                                featureService.delete(feature);
                                featuresMapService.remove(feature);
                                execute(createEditMessageText(chatId, messageId, "\uD83D\uDDD1️"));

                                return;
                            }
                        }
                    } catch (NumberFormatException ignored) {
                    }

                    execute(createEditMessageText(chatId, messageId, "Ошибка: такой точки не существует!"));

                } else if (isAdmin(userTelegramId)) {
                    if (callbackData.equals("import")) {
                        SendMessage sendMessage = createSendMessage(chatId,
                                "Чтобы импортировать точки, отправь мне .geojson файл");

                        sendMessage.setReplyMarkup(createKeyboard(
                                InlineKeyboardButton.builder()
                                        .text("Конструктор карт Яндекса")
                                        .url("https://yandex.ru/map-constructor/")
                                        .build()
                        ));

                        execute(sendMessage);
                    } else if (callbackData.equals("export")) {
                        SendDocument sendDocument = new SendDocument();
                        sendDocument.setChatId(chatId);

                        sendDocument.setDocument(new InputFile(
                                featuresMapService.exportFeatures(), "bot.geojson"));

                        execute(sendDocument);
                    }
                }
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private boolean isAdmin(Long userTelegramId) {
        return MapConstants.ADMINISTRATORS.contains(userTelegramId.toString());
    }

    private SendMessage createSendMessage(String chatId, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(text);
        return sendMessage;
    }

    private InlineKeyboardMarkup createKeyboard(InlineKeyboardButton... buttons) {
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        markupInline.setKeyboard(List.of(Arrays.asList(buttons)));
        return markupInline;
    }

    private EditMessageText createEditMessageText(String chatId, Integer messageId, String newText) {
        EditMessageText editMessage = new EditMessageText();
        editMessage.setChatId(chatId);
        editMessage.setMessageId(messageId);
        editMessage.setText(newText);
        editMessage.setReplyMarkup(null);
        return editMessage;
    }
}