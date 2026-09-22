package com.taskmanager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.InputStream;
import java.util.Properties;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {

        // --- 1. Читаем настройки из application.properties ---
        Properties props = new Properties();
        try (InputStream input = Main.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (input == null) {
                logger.error("Файл application.properties не найден в resources");
                return;
            }
            props.load(input);
        } catch (Exception e) {
            logger.error("Ошибка при чтении application.properties", e);
            return;
        }

        String botUsername = props.getProperty("bot.username");
        String botToken = props.getProperty("bot.token");

        if (botUsername == null || botToken == null) {
            logger.error("В application.properties не заданы bot.username или bot.token");
            return;
        }

        // --- 2. Настройка прокси (раскомментируйте, если нужен) ---
        DefaultBotOptions botOptions = new DefaultBotOptions();
        // botOptions.setProxyHost("127.0.0.1");
        // botOptions.setProxyPort(7890);
        // botOptions.setProxyType(DefaultBotOptions.ProxyType.SOCKS5);

        // --- 3. Запуск бота ---
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new TaskManagerBot(botOptions, botUsername, botToken));
            logger.info("Бот успешно запущен!");
        } catch (TelegramApiException e) {
            logger.error("Ошибка при запуске бота", e);
        }
    }
}