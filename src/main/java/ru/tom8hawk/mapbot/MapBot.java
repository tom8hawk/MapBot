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
import ru.tom8hawk.mapbot.repository.FeatureRepository;
import ru.tom8hawk.mapbot.repository.UserRepository;
import ru.tom8hawk.mapbot.service.FeatureService;

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

    private final FeatureRepository featureRepository;
    private final UserRepository userRepository;
    private final FeatureService featureService;

    @Autowired
    public MapBot(
            @Value("${telegram.bot.username}") String botUsername,
            @Value("${telegram.bot.token}") String botToken,
            FeatureRepository featureRepository,
            UserRepository userRepository,
            FeatureService featureService
    ) {

        this.botUsername = botUsername;
        this.botToken = botToken;
        this.featureRepository = featureRepository;
        this.userRepository = userRepository;
        this.featureService = featureService;
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
                    Long userId = message.getFrom().getId();
                    String username = message.getFrom().getUserName();
                    User user = userRepository.findByTelegramId(userId);

                    if (user == null) {
                        user = new User();
                        user.setTelegramId(userId);
                        user.setTelegramUsername(username);
                        userRepository.save(user);
                    } else if (username != null && !username.equals(user.getTelegramUsername())) {
                        user.setTelegramUsername(username);
                        userRepository.save(user);
                    }

                    boolean isUserAdmin = MapConstants.ADMINISTRATORS.contains(String.valueOf(userId));

                    if (message.hasText()) {
                        String messageText = message.getText();

                        if (messageText.equals("/start")) {
                            execute(createSendMessage(chatId, """
                            Привет! С помощью этого бота ты сможешь отмечать листовки на карте 🗺️📍
                            
                            Для отметки листовки просто отправь мне геометку""")
                            );
                        } else if (!messageText.equals("/admin")) {
                            return;
                        }

                        if (isUserAdmin) {
                            SendMessage adminMessage = new SendMessage();
                            adminMessage.setChatId(chatId);
                            adminMessage.setText("Данные бота");

                            adminMessage.setReplyMarkup(createKeyboard(
                                    InlineKeyboardButton.builder().text("Импорт").callbackData("import").build(),
                                    InlineKeyboardButton.builder().text("Экспорт").callbackData("export").build()
                            ));

                            execute(adminMessage);
                        }
                    } else if (message.hasLocation()) {
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

                        Long featureId = featureRepository.save(feature).getId();
                        featureService.display(feature);

                        SendMessage sendMessage = new SendMessage();
                        sendMessage.setChatId(chatId);
                        sendMessage.setText("Точка сохранена!");

                        sendMessage.setReplyMarkup(createKeyboard(
                                InlineKeyboardButton.builder()
                                        .text("\uD83D\uDDD1 Удалить️")
                                        .callbackData("delete_" + featureId)
                                        .build()
                        ));

                        execute(sendMessage);
                    } else if (message.hasDocument() && isUserAdmin) {
                        String fileId = message.getDocument().getFileId();
                        String fileName = message.getDocument().getFileName();

                        if (fileName.endsWith(".json") || fileName.endsWith(".geojson")) {
                            try {
                                String filePath = execute(new GetFile(fileId)).getFilePath();
                                File file = downloadFile(filePath);
                                featureService.importFeatures(file);

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
                Integer messageId = callback.getMessage().getMessageId();
                String chatId = callback.getMessage().getChatId().toString();

                if (callbackData.contains("delete")) {
                    try {
                        long featureId = Long.parseLong(callbackData.split("_")[1]);
                        Feature feature = featureRepository.findById(featureId).orElse(null);

                        if (feature != null) {
                            User creator = feature.getCreator();

                            if (creator != null && callback.getFrom().getId() == creator.getTelegramId()) {
                                featureRepository.deleteById(featureId);
                                featureService.remove(feature);

                                execute(createEditMessageText(chatId, messageId, "\uD83D\uDDD1️"));

                                return;
                            }
                        }
                    } catch (NumberFormatException ignored) {
                    }

                    execute(createEditMessageText(chatId, messageId, "Ошибка: такой точки не существует!"));

                } else if (callbackData.equals("import")) {
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
                    sendDocument.setDocument(new InputFile(featureService.exportFeatures(), "bot.geojson"));

                    execute(sendDocument);
                }
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
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