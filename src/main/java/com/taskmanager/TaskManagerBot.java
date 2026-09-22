package com.taskmanager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class TaskManagerBot extends TelegramLongPollingBot {

    private static final Logger logger = LoggerFactory.getLogger(TaskManagerBot.class);

    private final String botUsername;

    /**
     * Конструктор.
     *
     * @param options     настройки бота (в т.ч. прокси)
     * @param botUsername имя бота из BotFather
     * @param botToken    токен из BotFather
     */
    public TaskManagerBot(DefaultBotOptions options, String botUsername, String botToken) {
        super(options, botToken); // токен передаётся в родительский класс
        this.botUsername = botUsername;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {

        // Обрабатываем только текстовые сообщения
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        String messageText = update.getMessage().getText();
        long chatId = update.getMessage().getChatId();
        String userName = update.getMessage().getFrom().getUserName();

        logger.info("Получено сообщение от {} (chatId={}): {}", userName, chatId, messageText);

        switch (messageText) {
            case "/start":
                sendMessage(chatId, """
                Привет! Я бот-менеджер задач.
                Напиши /help, чтобы узнать, что я умею.""");
                break;

            case "/help":
                sendMessage(chatId, """
                Доступные команды:
                /start — начать работу
                /help — справка""");
                break;

            default:
                sendMessage(chatId, "Я получил твоё сообщение: " + messageText);
        }
    }

    /**
     * Отправляет текстовое сообщение в указанный чат.
     */
    private void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Не удалось отправить сообщение в чат {}", chatId, e);
        }
    }
}