package com.taskmanager;

import com.taskmanager.dialog.DialogManager;
import com.taskmanager.model.Role;
import com.taskmanager.model.User;
import com.taskmanager.service.TaskService;
import com.taskmanager.service.UserService;
import com.taskmanager.storage.InMemoryTaskStorage;
import com.taskmanager.storage.InMemoryUserStorage;
import com.taskmanager.storage.TaskStorage;
import com.taskmanager.storage.UserStorage;
import com.taskmanager.util.Messages;

import java.io.InputStream;
import java.util.Properties;
import java.util.Scanner;

public class ConsoleApp {

    public static void main(String[] args) {
        // Читаем настройки и ставим язык
        Properties props = new Properties();
        try (InputStream input = ConsoleApp.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (input != null) props.load(input);
        } catch (Exception e) {
            System.err.println("Не удалось прочитать application.properties");
        }
        
        String localeCode = props.getProperty("bot.locale", "ru");
        Messages.setLocale(localeCode);

        // --- Инициализация ---
        TaskStorage taskStorage = new InMemoryTaskStorage();
        UserStorage userStorage = new InMemoryUserStorage();

        UserService userService = new UserService(userStorage);
        TaskService taskService = new TaskService(taskStorage, userStorage);

        // Демо-пользователи
        if (userStorage.findAll().isEmpty()) {
            userService.createUser("Aleksa", Role.MANAGER);
            userService.createUser("Petrov", Role.EXECUTOR);
            userService.createUser("Sidorov", Role.EXECUTOR);
        }

        DialogManager dialogManager = new DialogManager(taskService, userService);
        User currentUser = userService.findByUsername("Aleksa");

        // Используем ключи из properties
        System.out.println(Messages.get("app.console_title"));
        System.out.println(Messages.get("app.logged_in", currentUser.username(), currentUser.role().getDisplayName()));
        System.out.println(Messages.get("app.enter_help") + "\n");

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print(Messages.get("app.prompt"));
            String input = scanner.nextLine().strip();

            if (input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("quit")) {
                System.out.println(Messages.get("app.exit"));
                break;
            }

            String response = dialogManager.processCommand(currentUser.id(), input);
            System.out.println(response);
        }
    }
}