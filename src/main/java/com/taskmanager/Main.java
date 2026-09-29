package com.taskmanager;

import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Properties;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {

        // --- 1. Читаем настройки ---
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

        Long groupId;
        String accessToken;
        try {
            groupId = Long.parseLong(props.getProperty("vk.group.id"));
            accessToken = props.getProperty("vk.access.token");
        } catch (NumberFormatException e) {
            logger.error("Некорректный vk.group.id в application.properties", e);
            return;
        }

        if (accessToken == null || accessToken.isBlank()) {
            logger.error("В application.properties не задан vk.access.token");
            return;
        }

        // --- 2. Создаём бота ---
        TaskManagerBot bot = new TaskManagerBot(groupId, accessToken);

        // --- 3. Включаем Long Poll и запускаем обработчик ---
        try {
            bot.getVk().groups()
                    .setLongPollSettings(bot.getActor(), groupId)
                    .enabled(true)
                    .messageNew(true)
                    .execute();

            GroupLongPollHandler handler = new GroupLongPollHandler(
                    bot.getVk(), bot.getActor(), groupId, bot);

            handler.init();     // получаем server, key, ts
            logger.info("VK-бот успешно запущен!");
            handler.run();      // бесконечный цикл

        } catch (ApiException | ClientException e) {
            logger.error("Ошибка при запуске бота", e);
        }
    }
}