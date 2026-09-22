package com.taskmanager;

import com.taskmanager.dialog.DialogManager;
import com.taskmanager.model.User;

import java.util.Scanner;

/**
 * Консольный запуск бота для отладки.
 */
public class ConsoleApp {

    public static void main(String[] args) {
        DialogManager dialogManager = new DialogManager();

        // Войдём как (менеджер)
        User currentUser = dialogManager.getUserByUsername("Aleksa");
        if (currentUser == null) {
            System.out.println("Не удалось найти пользователя Aleksa");
            return;
        }

        System.out.println("🤖 Task Manager Bot (консольный режим)");
        System.out.println("Вы вошли как: " + currentUser.getUsername()
                + " (" + currentUser.getRole() + ")");
        System.out.println("Введите /help для списка команд или 'exit'/'quit'  для выхода.\n");

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("quit")) {
                System.out.println("👋 До свидания!");
                break;
            }

            String response = dialogManager.processCommand(currentUser.getId(), input);
            System.out.println(response);
        }
    }
}