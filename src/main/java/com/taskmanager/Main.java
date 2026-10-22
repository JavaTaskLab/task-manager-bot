package com.taskmanager;

import com.taskmanager.config.AppConfig;
import com.taskmanager.dialog.DialogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Properties;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        // Собираем зависимости (DI)
        DialogManager dialogManager = AppConfig.createDialogManager();

        // Проверяем аргументы командной строки (Единая точка доступа)
        if (args.length > 0 && args[0].equals("--console")) {
            new ConsoleApp(dialogManager).start();
            return;
        }

        // По умолчанию запускаем VK-бота
        startVkBot(dialogManager);
    }

    private static void startVkBot(DialogManager dialogManager) {
        Properties props = new Properties();
        try (InputStream input = Main.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                logger.error("Файл application.properties не найден");
                return;
            }
            props.load(input);
        } catch (Exception e) {
            logger.error("Ошибка при чтении application.properties", e);
            return;
        }

        Long groupId = Long.parseLong(props.getProperty("vk.group.id"));
        String accessToken = props.getProperty("vk.access.token");

        TaskManagerBot bot = new TaskManagerBot(groupId, accessToken, dialogManager);
        GroupLongPollHandler handler = new GroupLongPollHandler(bot);

        try {
            handler.init();
            logger.info("VK-бот успешно запущен!");
            handler.run();
        } catch (Exception e) {
            logger.error("Ошибка при запуске бота", e);
        }
    }
}