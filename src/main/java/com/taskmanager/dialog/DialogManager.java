package com.taskmanager.dialog;

import com.taskmanager.model.Role;
import com.taskmanager.model.Task;
import com.taskmanager.model.TaskStatus;
import com.taskmanager.model.User;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Логика диалога. Общая для консоли и Telegram.
 * Не зависит от способа ввода/вывода.
 */
public class DialogManager {

    // Пока задачи храним в памяти (позже заменим на TaskStorage)
    private final Map<Long, Task> tasks = new HashMap<>();
    private final Map<String, User> users = new HashMap<>();
    private long nextTaskId = 1;
    private long nextUserId = 1;

    // Сессии пользователей (для будущих многошаговых диалогов)
    private final Map<Long, UserSession> sessions = new HashMap<>();

    public DialogManager() {
        // Создадим демо-пользователей
        createUser("Pazych", Role.MANAGER);
        createUser("Petrov", Role.EXECUTOR);
        createUser("Sidorov", Role.EXECUTOR);
    }

    /**
     * Обрабатывает команду пользователя и возвращает ответ.
     *
     * @param userId ID пользователя (в Telegram — chatId)
     * @param input  текст команды
     * @return ответ бота
     */
    public String processCommand(Long userId, String input) {
        if (input == null || input.isBlank()) {
            return "Пустая команда. Введите /help для списка команд.";
        }

        input = input.trim();

        if (input.equals("/start") || input.equals("/help") || input.equals("help")) {
            return getHelp();
        }

        String[] parts = input.split("\\s+", 3);
        String command = parts[0].toLowerCase();

        try {
            switch (command) {
                case "create":
                    return handleCreate(parts, userId);
                case "assign":
                    return handleAssign(parts, userId);
                case "status":
                    return handleStatus(parts, userId);
                case "my":
                    return handleMyTasks(userId);
                case "deadline":
                    return handleDeadline(parts, userId);
                case "overdue":
                    return handleOverdue();
                case "list":
                    return handleList(parts);
                case "show":
                    return handleShow(parts);
                default:
                    return "Неизвестная команда: " + command + "\nВведите /help для списка команд.";
            }
        } catch (Exception e) {
            return "❌ Ошибка: " + e.getMessage();
        }
    }

    // --- Обработчики команд ---

    private String handleCreate(String[] parts, Long userId) {
    if (parts.length < 2) {
        return "Использование: create \"Название задачи\"";
    }
    // Собираем всё, что после "create", и убираем кавычки
    String rawTitle = String.join(" ", 
            java.util.Arrays.copyOfRange(parts, 1, parts.length));
    String title = rawTitle.replace("\"", "").trim();
    
    if (title.isEmpty()) {
        return "❌ Название задачи не может быть пустым";
    }
    
    Task task = new Task(title, userId);
    task.setId(nextTaskId++);
    tasks.put(task.getId(), task);
    return "✅ Задача создана! ID: " + task.getId() + ", Название: " + task.getTitle();
}

    private String handleAssign(String[] parts, Long userId) {
        if (parts.length < 3) {
            return "Использование: assign <id задачи> @<имя пользователя>";
        }
        Long taskId = Long.parseLong(parts[1]);
        String username = parts[2].startsWith("@") ? parts[2].substring(1) : parts[2];

        Task task = tasks.get(taskId);
        if (task == null) {
            return "❌ Задача с ID " + taskId + " не найдена";
        }

        User user = users.get(username.toLowerCase());
        if (user == null) {
            return "❌ Пользователь " + username + " не найден";
        }

        task.setAssigneeId(user.getId());
        task.setUpdatedAt(LocalDate.now());
        return "✅ Задача \"" + task.getTitle() + "\" назначена на " + username;
    }

    private String handleStatus(String[] parts, Long userId) {
        if (parts.length < 3) {
            return "Использование: status <id задачи> <статус>\nСтатусы: backlog, todo, in_progress, review, done, blocked";
        }
        Long taskId = Long.parseLong(parts[1]);
        Task task = tasks.get(taskId);
        if (task == null) {
            return "❌ Задача с ID " + taskId + " не найдена";
        }

        try {
            TaskStatus status = TaskStatus.valueOf(parts[2].toUpperCase());
            task.setStatus(status);
            task.setUpdatedAt(LocalDate.now());
            return "✅ Статус задачи \"" + task.getTitle() + "\" изменён на " + status;
        } catch (IllegalArgumentException e) {
            return "❌ Неизвестный статус: " + parts[2];
        }
    }

    private String handleMyTasks(Long userId) {
        List<Task> myTasks = new ArrayList<>();
        for (Task t : tasks.values()) {
            if (userId.equals(t.getAssigneeId())) {
                myTasks.add(t);
            }
        }
        if (myTasks.isEmpty()) {
            return "📭 У вас нет задач.";
        }

        StringBuilder sb = new StringBuilder("📋 Ваши задачи:\n");
        for (Task t : myTasks) {
            sb.append("  ").append(t.getId()).append(". ")
              .append(t.getTitle()).append(" [").append(t.getStatus()).append("]");
            if (t.getDeadline() != null) {
                sb.append(" ⏰ до ").append(t.getDeadline());
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String handleDeadline(String[] parts, Long userId) {
        if (parts.length < 3) {
            return "Использование: deadline <id задачи> <YYYY-MM-DD>";
        }
        Long taskId = Long.parseLong(parts[1]);
        Task task = tasks.get(taskId);
        if (task == null) {
            return "❌ Задача с ID " + taskId + " не найдена";
        }

        try {
            LocalDate deadline = LocalDate.parse(parts[2]);
            task.setDeadline(deadline);
            task.setUpdatedAt(LocalDate.now());
            return "✅ Дедлайн для задачи \"" + task.getTitle() + "\" установлен на " + deadline;
        } catch (Exception e) {
            return "❌ Неверный формат даты. Используйте YYYY-MM-DD";
        }
    }

    private String handleOverdue() {
        List<Task> overdue = new ArrayList<>();
        for (Task t : tasks.values()) {
            if (t.isOverdue()) {
                overdue.add(t);
            }
        }
        if (overdue.isEmpty()) {
            return "🎉 Нет просроченных задач!";
        }

        StringBuilder sb = new StringBuilder("⚠️ Просроченные задачи:\n");
        for (Task t : overdue) {
            sb.append("  ").append(t.getId()).append(". ")
              .append(t.getTitle()).append(" [").append(t.getStatus()).append("]")
              .append(" ⏰ было до ").append(t.getDeadline()).append("\n");
        }
        return sb.toString();
    }

    private String handleList(String[] parts) {
        if (tasks.isEmpty()) {
            return "📭 Задач нет.";
        }
        StringBuilder sb = new StringBuilder("📋 Все задачи:\n");
        for (Task t : tasks.values()) {
            sb.append("  ").append(t.getId()).append(". ")
              .append(t.getTitle()).append(" [").append(t.getStatus()).append("]");
            if (t.getAssigneeId() != null) {
                sb.append(" → ").append(getUsernameById(t.getAssigneeId()));
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String handleShow(String[] parts) {
        if (parts.length < 2) {
            return "Использование: show <id задачи>";
        }
        Long taskId = Long.parseLong(parts[1]);
        Task task = tasks.get(taskId);
        if (task == null) {
            return "❌ Задача с ID " + taskId + " не найдена";
        }
        return formatTask(task);
    }

    // --- Вспомогательные ---

    private User createUser(String username, Role role) {
        User user = new User(nextUserId++, username, role);
        users.put(username.toLowerCase(), user);
        return user;
    }

    private String getUsernameById(Long id) {
        for (User u : users.values()) {
            if (u.getId().equals(id)) return u.getUsername();
        }
        return "?";
    }

    private String formatTask(Task t) {
        StringBuilder sb = new StringBuilder();
        sb.append("📌 Задача #").append(t.getId()).append("\n");
        sb.append("  Название: ").append(t.getTitle()).append("\n");
        sb.append("  Статус: ").append(t.getStatus()).append("\n");
        sb.append("  Создатель: ").append(getUsernameById(t.getCreatorId())).append("\n");
        if (t.getAssigneeId() != null) {
            sb.append("  Исполнитель: ").append(getUsernameById(t.getAssigneeId())).append("\n");
        }
        if (t.getDeadline() != null) {
            sb.append("  Дедлайн: ").append(t.getDeadline()).append("\n");
        }
        sb.append("  Создана: ").append(t.getCreatedAt()).append("\n");
        return sb.toString();
    }

    private String getHelp() {
        return """
            🤖 Task Manager Bot — команды:
            
            create "Название"          — создать задачу
            assign 5 @Petrov           — назначить задачу №5 на Petrov
            status 5 in_progress       — изменить статус задачи №5
            my tasks                   — показать мои задачи
            deadline 5 2026-10-15      — установить дедлайн
            overdue                    — показать просроченные задачи
            list                       — показать все задачи
            show 5                     — детали задачи №5
            /help                      — эта справка
            
            Статусы: backlog, todo, in_progress, review, done, blocked
            """;
    }

    /**
     * Возвращает пользователя по его ID (для входа в консольном режиме).
     */
    public User getUserById(Long id) {
        for (User u : users.values()) {
            if (u.getId().equals(id)) return u;
        }
        return null;
    }

    /**
     * Возвращает пользователя по username.
     */
    public User getUserByUsername(String username) {
        return users.get(username.toLowerCase());
    }
}