package mainBot.mainBot.service;

import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import mainBot.mainBot.ads.Ads;
import mainBot.mainBot.ads.AdsRepository;
import mainBot.mainBot.config.BotConfig;
import mainBot.mainBot.user.User;
import mainBot.mainBot.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class TelegramBot extends TelegramLongPollingBot {

    private final BotConfig botConfig;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdsRepository adsRepository;

    static final String HELP_TEXT = "Бот создан для демонстрации. \n\n" +
            "Для выбора команды можно использовать меню в левой нижней части экрана. \n\n" +
            "или напечатать команду текстом: \n\n" +
            "Список доступных команд:\n\n" +
            "/start \n\n" +
            "/register \n\n" +
            "/time \n\n" +
            "/mydata \n\n" +
            "/deletedata \n\n" +
            "/help";

    static final String NO_BUTTON = "NO_BUTTON";
    static final String YES_BUTTON = "YES_BUTTON";
    static final String ERROR_TEXT = "Error occurred: ";

    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }

    @Override
    public String getBotUsername() {
        return botConfig.getBotName();
    }

    public TelegramBot(BotConfig botConfig) {
        this.botConfig = botConfig;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "Начать."));
        listOfCommands.add(new BotCommand("/register", "Зарегистрироваться"));
        listOfCommands.add(new BotCommand("/mydata", "Получить мои данные."));
        listOfCommands.add(new BotCommand("/date", "Дата и время"));
        listOfCommands.add(new BotCommand("/deletedata", "Удалить мои данные."));
        listOfCommands.add(new BotCommand("/help", "Инструкция по использованию бота."));
        listOfCommands.add(new BotCommand("/settings", "Настроить бот."));

        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Ошибка установки меню бота. {}", e.getMessage());
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if (messageText.contains("/send") && botConfig.getOwnerId() == chatId) {
                var textToSend = EmojiParser.parseToUnicode(messageText.substring(messageText.indexOf(" ")));
                var users = userRepository.findAll();
                for (User user : users) {
                    prepareAndSendMessage(user.getId(), textToSend);
                }
            } else {

                switch (messageText) {
                    case "/start":
                        startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                        break;
                    case "/help":
                        prepareAndSendMessage(chatId, HELP_TEXT);
                        break;
                    case "/register":
                        register(chatId);
                        break;
                    case "/time":
                        getDate(chatId);
                        break;
                    default:
                        prepareAndSendMessage(chatId, "Извини, эта команда еще не поддерживатеся.");
                }
            }
        } else if (update.hasCallbackQuery()) {
            String callBackData = update.getCallbackQuery().getData();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            if (callBackData.equals(YES_BUTTON)) {
                String text = "Выбрана кнопка: Да. \n\nРегистрация прошла успешно!";

                registerUser(update.getCallbackQuery().getMessage());
                log.info("Пользователь {} зарегистрировался.",
                        update.getCallbackQuery().getMessage().getChat().getFirstName());

                executeMessageEditText(text, chatId, messageId);

            } else if (callBackData.equals(NO_BUTTON)) {
                String text = "Выбрана кнопка: Нет";
                executeMessageEditText(text, chatId, messageId);
            }
        }
    }

    private void getDate(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);

        LocalDateTime date = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm - dd.MM.yyyy");
        message.setText(date.format(formatter));

        executeMessage(message);
    }

    private void register(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Ты правда хочешь зарегистрироваться?");

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();

        List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        var yesButton = new InlineKeyboardButton();

        yesButton.setText("Да");
        yesButton.setCallbackData(YES_BUTTON);

        var noButton = new InlineKeyboardButton();

        noButton.setText("Нет");
        noButton.setCallbackData(NO_BUTTON);

        rowInLine.add(yesButton);
        rowInLine.add(noButton);

        rowsInLine.add(rowInLine);

        inlineKeyboardMarkup.setKeyboard(rowsInLine);

        message.setReplyMarkup(inlineKeyboardMarkup);

        executeMessage(message);
    }

    private void registerUser(Message message) {

        if (userRepository.findById(message.getChatId()).isEmpty()) {

            var chatId = message.getChatId();
            var chat = message.getChat();

            User user = new User();

            user.setId(chatId);
            user.setFirstName(chat.getFirstName());

            if (chat.getLastName() != null) {
                user.setLastName(chat.getLastName());
            }

            user.setUserName(chat.getUserName());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));

            userRepository.save(user);
            log.info("User saved: {}", user);
        }
    }

    private void startCommandReceived(long chatId, String name) {

        String answer = EmojiParser.parseToUnicode("Привет " + name + "!" + " :blush:");


        log.info("Ответ пользователю {}", name);

        startMessage(chatId, answer);
    }

    private void startMessage(long chatId, String messageText) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(messageText);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();

        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();

        row.add("/help");
        row.add("/time");

        keyboardRows.add(row);

        row = new KeyboardRow();

        row.add("/register");

        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);

        message.setReplyMarkup(keyboardMarkup);

        executeMessage(message);
    }

    private void executeMessageEditText(String text, long chatId, long messageId) {
        EditMessageText message = new EditMessageText();
        message.setChatId(chatId);
        message.setText(text);
        message.setMessageId((int) messageId);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR_TEXT + e.getMessage());
        }
    }

    private void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR_TEXT + e.getMessage());
        }
    }

    private void prepareAndSendMessage(long chatId, String messageText) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(messageText);

        executeMessage(message);
    }
                    //cек., мин., день, неделя, месяц, год
    @Scheduled(cron = "${cron.scheduler}")
    private void sandAds() {
        var ads = adsRepository.findAll();
        var users = userRepository.findAll();

        for (Ads ad : ads) {
            for (User user : users) {
                prepareAndSendMessage(user.getId(), ad.getAdText());
            }
        }
    }
}
