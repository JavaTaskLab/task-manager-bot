package com.taskmanager.dialog;

import com.taskmanager.model.Role;
import com.taskmanager.model.User;
import com.taskmanager.service.TaskService;
import com.taskmanager.service.UserService;
import com.taskmanager.storage.InMemoryTaskStorage;
import com.taskmanager.storage.InMemoryUserStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DialogManager — команды")
class DialogManagerTest {

    private DialogManager dialogManager;
    private UserService userService;
    private TaskService taskService;
    private Long userId;

    @BeforeEach
    void setUp() {
        var taskStorage = new InMemoryTaskStorage();
        var userStorage = new InMemoryUserStorage();
        userService = new UserService(userStorage);
        taskService = new TaskService(taskStorage, userStorage);
        dialogManager = new DialogManager(taskService, userService);

        userId = userService.createUser("Aleksa", Role.MANAGER).id();
    }

    @Nested
    @DisplayName("create")
    class CreateTests {

        @Test
        @DisplayName("создаёт задачу")
        void shouldCreate() {
            String response = dialogManager.processCommand(userId, "create \"Отчёт\"");
            assertTrue(response.contains("Задача создана"));
            assertTrue(response.contains("Отчёт"));
        }

        @Test
        @DisplayName("ошибка при пустой команде")
        void emptyCommand() {
            String response = dialogManager.processCommand(userId, "");
            assertTrue(response.contains("Пустая команда"));
        }

        @Test
        @DisplayName("ошибка при отсутствии названия")
        void missingTitle() {
            String response = dialogManager.processCommand(userId, "create");
            assertTrue(response.contains("Использование"));
        }
    }

    @Nested
    @DisplayName("/help")
    class HelpTests {

        @Test
        @DisplayName("показывает справку")
        void shouldShowHelp() {
            String response = dialogManager.processCommand(userId, "/help");
            assertTrue(response.contains("create"));
            assertTrue(response.contains("assign"));
            assertTrue(response.contains("list"));
        }
    }

    @Nested
    @DisplayName("list")
    class ListTests {

        @Test
        @DisplayName("пустой список")
        void empty() {
            String response = dialogManager.processCommand(userId, "list");
            assertTrue(response.contains("Задач нет"));
        }

        @Test
        @DisplayName("древовидный вывод")
        void treeOutput() {
            dialogManager.processCommand(userId, "create \"Родитель\"");
            dialogManager.processCommand(userId, "create \"Ребёнок\"");
            dialogManager.processCommand(userId, "subtask 1 2");

            String response = dialogManager.processCommand(userId, "list");
            assertTrue(response.contains("#1"));
            assertTrue(response.contains("#2"));
            assertTrue(response.contains("└──") || response.contains("├──"));
        }
    }

    @Nested
    @DisplayName("Обработка ошибок")
    class ErrorTests {

        @Test
        @DisplayName("неизвестная команда")
        void unknownCommand() {
            String response = dialogManager.processCommand(userId, "foobar");
            assertTrue(response.contains("Неизвестная команда"));
        }

        @Test
        @DisplayName("ошибка при неизвестной задаче")
        void unknownTask() {
            String response = dialogManager.processCommand(userId, "show 999");
            assertTrue(response.contains("Ошибка"));
        }
    }
}