package com.taskmanager.config;

import com.taskmanager.dialog.DialogManager;
import com.taskmanager.service.TaskService;
import com.taskmanager.service.UserService;
import com.taskmanager.storage.InMemoryTaskStorage;
import com.taskmanager.storage.InMemoryUserStorage;
import com.taskmanager.storage.TaskStorage;
import com.taskmanager.storage.UserStorage;

/**
 * Единая точка сборки зависимостей (ручной DI).
 */
public class AppConfig {

    public static DialogManager createDialogManager() {
        // 1. Хранилища
        TaskStorage taskStorage = new InMemoryTaskStorage();
        UserStorage userStorage = new InMemoryUserStorage();

        // 2. Сервисы
        TaskService taskService = new TaskService(taskStorage, userStorage);
        UserService userService = new UserService(userStorage);

        // 3. Бизнес-логика диалога
        return new DialogManager(taskService, userService);
    }
}