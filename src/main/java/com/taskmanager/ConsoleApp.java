package com.taskmanager;

import com.taskmanager.dialog.DialogManager;

import java.util.Scanner;

public class ConsoleApp {

    private final DialogManager dialogManager;

    public ConsoleApp(DialogManager dialogManager) {
        this.dialogManager = dialogManager;
    }

    public void start() {
        System.out.println("=== Консольный режим Task Manager ===");
        System.out.println("Введите /help для списка команд. Для выхода введите exit.");

        Scanner scanner = new Scanner(System.in);
        Long userId = 1L; // В консоли считаем, что это первый пользователь

        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine();

            if (input.equalsIgnoreCase("exit")) {
                break;
            }

            String response = dialogManager.processCommand(userId, input);
            System.out.println(response);
        }
    }
}