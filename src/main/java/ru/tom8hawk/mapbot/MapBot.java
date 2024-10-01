package ru.tom8hawk.mapbot;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.tom8hawk.mapbot.component.FeatureConfig;
import ru.tom8hawk.mapbot.model.Feature;
import ru.tom8hawk.mapbot.model.Geometry;
import ru.tom8hawk.mapbot.model.User;
import ru.tom8hawk.mapbot.repository.FeatureRepository;
import ru.tom8hawk.mapbot.repository.UserRepository;
import ru.tom8hawk.mapbot.service.FeatureService;

import java.io.File;
import java.util.*;

@Component
public class MapBot extends TelegramLongPollingBot {

    @Getter
    private final String botUsername;

    @Getter
    private final String botToken;

    private final FeatureRepository featureRepository;
    private final UserRepository userRepository;
    private final FeatureService featureService;

    private final FeatureConfig featureConfig;

    @Autowired
    public MapBot(
            @Value("${telegram.bot.username}") String botUsername,
            @Value("${telegram.bot.token}") String botToken,
            FeatureRepository featureRepository,
            UserRepository userRepository,
            FeatureService featureService,
            FeatureConfig featureConfig
    ) {

        this.botUsername = botUsername;
        this.botToken = botToken;
        this.featureRepository = featureRepository;
        this.userRepository = userRepository;
        this.featureService = featureService;
        this.featureConfig = featureConfig;
    }

    @PostConstruct
    public void init() throws TelegramApiException {
        new TelegramBotsApi(DefaultBotSession.class).registerBot(this);
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            Message message = update.getMessage();

            if (message.getChat().isUserChat()) {
                String chatId = message.getChatId().toString();

                long userId = message.getFrom().getId();
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

                if (message.hasText()) {
                    String messageText = message.getText();

                    if (messageText.equals("/start")) {
                        SendMessage sendMessage = createSendMessage(chatId, """
                            Привет! С помощью этого бота ты сможешь отмечать листовки на карте 🗺️📍
                            
                            Для отметки листовки просто отправь мне геометку""");

                        try {
                            execute(sendMessage);
                        } catch (TelegramApiException e) {
                            e.printStackTrace();
                        }

                        if (user.isAdmin()) {
                            SendMessage adminMessage = new SendMessage();
                            adminMessage.setChatId(chatId);
                            adminMessage.setText("Админ-панель:");

                            InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
                            List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
                            List<InlineKeyboardButton> rowInline = new ArrayList<>();

                            rowInline.add(createInlineKeyboardButton("Импорт", "import"));
                            rowInline.add(createInlineKeyboardButton("Экспорт", "export"));
                            rowInline.add(createInlineKeyboardButton("Точки", "manage_points"));
                            rowInline.add(createInlineKeyboardButton("ЧС", "bans"));

                            rowsInline.add(rowInline);
                            markupInline.setKeyboard(rowsInline);
                            adminMessage.setReplyMarkup(markupInline);

                            try {
                                execute(adminMessage);
                            } catch (TelegramApiException e) {
                                e.printStackTrace();
                            }
                        }
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

                    Map<String, String> properties = new HashMap<>();
                    properties.put("marker-color", featureConfig.getMarkerColor());
                    feature.setProperties(properties);

                    long featureId = featureRepository.save(feature).getId();
                    featureService.display(feature);

                    SendMessage sendMessage = new SendMessage();
                    sendMessage.setChatId(chatId);
                    sendMessage.setText("Точка сохранена!");

                    InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
                    List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
                    List<InlineKeyboardButton> rowInline = new ArrayList<>();

                    rowInline.add(createInlineKeyboardButton("\uD83D\uDDD1 Удалить️", "delete_" + featureId));

                    rowsInline.add(rowInline);
                    markupInline.setKeyboard(rowsInline);
                    sendMessage.setReplyMarkup(markupInline);

                    try {
                        execute(sendMessage);
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                } else if (message.hasDocument() && user.isAdmin()) {
                    String fileId = message.getDocument().getFileId();
                    String fileName = message.getDocument().getFileName();

                    if (fileName.endsWith(".json") || fileName.endsWith(".geojson")) {
                        try {
                            String filePath = execute(new GetFile(fileId)).getFilePath();
                            File file = downloadFile(filePath);

                            if (fileName.endsWith(".geojson")) {
                                File renamedFile = new File(file.getParent(),
                                        fileName.replace(".geojson", ".json"));
                                if (file.renameTo(renamedFile)) {
                                    file = renamedFile;
                                }
                            }

                            featureService.importFromFile(file);

                            SendMessage sendMessage = createSendMessage(chatId, "Файл успешно обработан!");
                            execute(sendMessage);
                        } catch (Exception e) {
                            e.printStackTrace();
                            SendMessage sendMessage = createSendMessage(chatId, "Ошибка при обработке файла.");

                            try {
                                execute(sendMessage);
                            } catch (TelegramApiException ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                }
            }
        } else if (update.hasCallbackQuery()) {
            CallbackQuery callback = update.getCallbackQuery();

            String callbackData = callback.getData();
            String chatId = callback.getMessage().getChatId().toString();

            if (callbackData.contains("delete_")) {
                try {
                    long featureId = Long.parseLong(callbackData.split("_")[1]);
                    Feature feature = featureRepository.findById(featureId).orElse(null);

                    if (feature != null) {
                        User creator = feature.getCreator();

                        if (creator != null && Objects.equals(creator.getTelegramId(), callback.getFrom().getId())) {
                            featureRepository.deleteById(featureId);
                            featureService.remove(feature);

                            EditMessageText editMessage = createEditMessageText(chatId,
                                    callback.getMessage().getMessageId(), "\uD83D\uDDD1️");

                            try {
                                execute(editMessage);
                            } catch (TelegramApiException e) {
                                e.printStackTrace();
                            }

                            return;
                        }
                    }
                } catch (NumberFormatException ignored) {
                }

                EditMessageText editMessage = createEditMessageText(chatId,
                        callback.getMessage().getMessageId(), "Ошибка: такой точки не существует!");

                try {
                    execute(editMessage);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private SendMessage createSendMessage(String chatId, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(text);
        return sendMessage;
    }

    private InlineKeyboardButton createInlineKeyboardButton(String text, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callbackData);
        return button;
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