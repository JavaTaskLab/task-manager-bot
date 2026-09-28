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

import java.util.Scanner;

public class ConsoleApp {

    public static void main(String[] args) {
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

        System.out.println("🤖 Task Manager Bot (консоль)");
        System.out.println("Вы вошли как: " + currentUser.username()
                + " (" + currentUser.role().getDisplayName() + ")");
        System.out.println("Введите /help или 'exit'.\n");

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine().strip();

            if (input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("quit")) {
                System.out.println("👋 До свидания!");
                break;
            }

            String response = dialogManager.processCommand(currentUser.id(), input);
            System.out.println(response);
        }
    }
}