package com.taskmanager;

import com.taskmanager.service.TaskService;
import com.taskmanager.service.UserService;
import com.taskmanager.storage.InMemoryTaskStorage;
import com.taskmanager.storage.InMemoryUserStorage;
import com.taskmanager.storage.TaskStorage;
import com.taskmanager.storage.UserStorage;
import com.taskmanager.util.Messages;
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

        // Устанавливаем язык
        String localeCode = props.getProperty("bot.locale", "ru");
        Messages.setLocale(localeCode);
        logger.info("Установлен язык бота: {}", localeCode);

        // --- 2. VK Настройки ---
        Long groupId = Long.parseLong(props.getProperty("vk.group.id"));
        String accessToken = props.getProperty("vk.access.token");

        if (accessToken == null || accessToken.isBlank()) {
            logger.error("В application.properties не задан vk.access.token");
            return;
        }

        // --- 3. Создаём хранилища ---
        TaskStorage taskStorage = new InMemoryTaskStorage();
        UserStorage userStorage = new InMemoryUserStorage();

        // --- 4. Создаём сервисы ---
        TaskService taskService = new TaskService(taskStorage, userStorage);
        UserService userService = new UserService(userStorage);

        // --- 5. Создаём бота ---
        TaskManagerBot bot = new TaskManagerBot(groupId, accessToken, taskService, userService);
        GroupLongPollHandler handler = new GroupLongPollHandler(bot);

        // --- 6. Запускаем ---
        try {
            handler.init();
            logger.info("VK-бот успешно запущен!");
            handler.run();
        } catch (Exception e) {
            logger.error("Ошибка при запуске бота", e);
        }
    }
}