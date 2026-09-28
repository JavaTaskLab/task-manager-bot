package com.taskmanager.dialog;

import com.taskmanager.model.*;
import com.taskmanager.service.TaskService;
import com.taskmanager.service.UserService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Логика диалога: парсинг команд и формирование ответов.
 *
 * <p>НЕ зависит от способа ввода/вывода (ТЗ п.2).
 * НЕ хранит данные — это делает storage (ТЗ п.1).
 */
public class DialogManager {

    private final TaskService taskService;
    private final UserService userService;

    public DialogManager(TaskService taskService, UserService userService) {
        this.taskService = taskService;
        this.userService = userService;
    }

    /**
     * Обрабатывает команду и возвращает ответ.
     *
     * @param userId ID пользователя (в Telegram — chatId)
     * @param input  текст команды
     */
    public String processCommand(Long userId, String input) {
        if (input == null || input.isBlank()) {
            return "Пустая команда. Введите /help для списка команд.";
        }

        input = input.strip();

        if (userService.findById(userId) == null) {
            userService.getOrCreate(userId, "user_" + userId, null);
        }

        if (input.equals("/start") || input.equals("/help") || input.equals("help")) {
            return getHelp();
        }

        String[] parts = input.split("\\s+");
        String command = parts[0].toLowerCase();

        try {
            return switch (command) {
                case "create"   -> handleCreate(parts, userId);
                case "describe" -> handleDescribe(parts, userId);
                case "assign"   -> handleAssign(parts, userId);
                case "status"   -> handleStatus(parts, userId);
                case "my"       -> handleMyTasks(userId);
                case "deadline" -> handleDeadline(parts, userId);
                case "overdue"  -> handleOverdue();
                case "list"     -> handleList();
                case "show"     -> handleShow(parts);
                case "subtask"  -> handleSubtask(parts, userId);
                case "depend"   -> handleDepend(parts, userId);
                default -> "Неизвестная команда: " + command + "\nВведите /help.";
            };
        } catch (Exception e) {
            e.printStackTrace();
            String msg = e.getMessage();
            return "❌ Ошибка: " + (msg != null ? msg : e.getClass().getSimpleName());
        }
    }

    private String handleCreate(String[] parts, Long userId) {
    String rest = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
    List<String> args = extractQuoted(rest);

    if (args.isEmpty()) {
        return "Использование: create \"Название\" [\"Описание\"]";
    }

    String title = args.get(0);
    String description = args.size() > 1 ? args.get(1) : null;

    Task task = (description != null)
            ? taskService.createTask(title, description, userId)
            : taskService.createTask(title, userId);

    return "✅ Задача создана! ID: " + task.id() + ", Название: " + task.title()
         + (description != null ? ", Описание: " + description : "");
}

    private String handleDescribe(String[] parts, Long userId) {
        if (parts.length < 3) {
            return "Использование: describe <id> \"Описание\"";
        }
        Long taskId = Long.parseLong(parts[1]);
        String rest = String.join(" ", Arrays.copyOfRange(parts, 2, parts.length));
        List<String> args = extractQuoted(rest);

        if (args.isEmpty()) {
            return "❌ Опишите задачу в кавычках: describe 1 \"текст\"";
        }

        Task task = taskService.setDescription(taskId, args.get(0), userId);
        return "✅ Описание задачи #" + task.id() + " обновлено";
    }

    private List<String> extractQuoted(String input) {
        List<String> result = new ArrayList<>();
        Matcher m = Pattern.compile("\"([^\"]*)\"").matcher(input);
        while (m.find()) {
            result.add(m.group(1));
        }
        if (result.isEmpty()) {
            String trimmed = input.strip();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private String handleAssign(String[] parts, Long userId) {
        if (parts.length < 3) {
            return "Использование: assign <id задачи> @<имя>";
        }
        Long taskId = Long.parseLong(parts[1]);
        String username = parts[2].startsWith("@") ? parts[2].substring(1) : parts[2];

        User assignee = userService.findByUsername(username);
        if (assignee == null) {
            return "❌ Пользователь не найден: " + username;
        }

        Task task = taskService.assignTask(taskId, assignee.id(), userId);
        return "✅ Задача \"" + task.title() + "\" назначена на " + username;
    }

    private String handleStatus(String[] parts, Long userId) {
        if (parts.length < 3) {
            return "Использование: status <id> <статус>\nСтатусы: " + statusList();
        }
        Long taskId = Long.parseLong(parts[1]);
        TaskStatus status = TaskStatus.fromString(parts[2]);

        Task task = taskService.changeStatus(taskId, status, userId);
        return "✅ Статус задачи \"" + task.title() + "\" → " + status.getDisplayName();
    }

    private String handleMyTasks(Long userId) {
        var tasks = taskService.getMyTasks(userId);
        if (tasks.isEmpty()) {
            return "📭 У вас нет задач.";
        }

        StringBuilder sb = new StringBuilder("📋 Ваши задачи:\n");
        for (Task t : tasks) {
            sb.append("  ").append(t.id()).append(". ")
              .append(t.title()).append(" [").append(t.status().getDisplayName()).append("]");
            if (t.deadline() != null) {
                sb.append(" ⏰ до ").append(t.deadline().toLocalDate());
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String handleDeadline(String[] parts, Long userId) {
        if (parts.length < 3) {
            return "Использование: deadline <id> <YYYY-MM-DD>";
        }
        Long taskId = Long.parseLong(parts[1]);
        LocalDateTime deadline = LocalDate.parse(parts[2]).atStartOfDay();

        Task task = taskService.setDeadline(taskId, deadline, userId);
        return "✅ Дедлайн задачи \"" + task.title() + "\" → " + parts[2];
    }

    private String handleOverdue() {
        var tasks = taskService.getOverdueTasks(LocalDateTime.now());
        if (tasks.isEmpty()) {
            return "🎉 Нет просроченных задач!";
        }

        StringBuilder sb = new StringBuilder("⚠️ Просроченные задачи:\n");
        for (Task t : tasks) {
            sb.append("  ").append(t.id()).append(". ")
              .append(t.title()).append(" ⏰ до ")
              .append(t.deadline().toLocalDate()).append("\n");
        }
        return sb.toString();
    }

    private String handleList() {
        var allTasks = taskService.getAllTasks();

        if (allTasks.isEmpty()) {
            return "📭 Задач нет.";
        }

        Set<Long> subtaskIds = allTasks.stream()
                .flatMap(t -> t.subtaskIds().stream())
                .collect(Collectors.toSet());

        List<Task> roots = allTasks.stream()
                .filter(t -> !subtaskIds.contains(t.id()))
                .sorted(Comparator.comparing(Task::id))
                .toList();

        StringBuilder sb = new StringBuilder("📋 Все задачи:\n\n");

        for (int i = 0; i < roots.size(); i++) {
            appendTaskTree(sb, roots.get(i), "", true, true, allTasks);
            if (i < roots.size() - 1) {
                sb.append("\n");
            }
        }

        return sb.toString().stripTrailing();
    }

    /**
     * Рекурсивно выводит задачу и её подзадачи.
     *
     * @param sb       буфер
     * @param task     текущая задача
     * @param prefix   префикс для отступа
     * @param isLast   последняя ли задача в списке
     * @param isRoot   корневая ли задача (без веток)
     * @param allTasks все задачи (для поиска подзадач)
     */
    private void appendTaskTree(StringBuilder sb, Task task, String prefix,
                                 boolean isLast, boolean isRoot, List<Task> allTasks) {

        String branch = isRoot ? "📌 " : (isLast ? "└── " : "├── ");

        sb.append(prefix).append(branch)
          .append("#").append(task.id()).append(". ")
          .append(task.title())
          .append(" [").append(task.status().getDisplayName()).append("]");

        if (task.assigneeId() != null) {
            User u = userService.findById(task.assigneeId());
            if (u != null) sb.append(" → ").append(u.username());
        }
        if (task.deadline() != null) {
            sb.append(" ⏰ ").append(task.deadline().toLocalDate());
        }
        sb.append("\n");

        List<Long> subtaskIdList = task.subtaskIds();
        if (subtaskIdList.isEmpty()) return;

        String childPrefix = isRoot ? "" : prefix + (isLast ? "    " : "│   ");

        for (int i = 0; i < subtaskIdList.size(); i++) {
            Long childId = subtaskIdList.get(i);
            boolean childIsLast = (i == subtaskIdList.size() - 1);

            allTasks.stream()
                    .filter(t -> t.id().equals(childId))
                    .findFirst()
                    .ifPresent(child -> appendTaskTree(
                            sb, child, childPrefix, childIsLast, false, allTasks));
        }
    }

    private String handleShow(String[] parts) {
        if (parts.length < 2) {
            return "Использование: show <id>";
        }
        Long taskId = Long.parseLong(parts[1]);
        Task task = taskService.getTask(taskId);
        return formatTask(task);
    }

    private String formatTask(Task t) {
        StringBuilder sb = new StringBuilder();
        sb.append("📌 Задача #").append(t.id()).append("\n");
        sb.append("  Название: ").append(t.title()).append("\n");

        if (t.description() != null && !t.description().isBlank()) {
            sb.append("  Описание: ").append(t.description()).append("\n");
        }

        sb.append("  Статус: ").append(t.status().getDisplayName()).append("\n");
        sb.append("  Создатель: ").append(usernameOf(t.creatorId())).append("\n");

        if (t.assigneeId() != null) {
            sb.append("  Исполнитель: ").append(usernameOf(t.assigneeId())).append("\n");
        }
        if (t.deadline() != null) {
            sb.append("  Дедлайн: ").append(t.deadline().toLocalDate()).append("\n");
        }
        sb.append("  Создана: ").append(t.createdAt().toLocalDate()).append("\n");

        if (t.hasSubtasks()) {
            sb.append("  Подзадачи: ");
            for (Long id : t.subtaskIds()) sb.append("#").append(id).append(" ");
            sb.append("\n");
        }
        if (t.isBlocked()) {
            sb.append("  ⚠️ Заблокирована: ");
            for (Long id : t.blockedByIds()) sb.append("#").append(id).append(" ");
            sb.append("\n");
        }
        return sb.toString();
    }

    private String handleSubtask(String[] parts, Long userId) {
        if (parts.length < 3) {
            return "Использование: subtask <id родителя> <id подзадачи>";
        }
        Long parentId = Long.parseLong(parts[1]);
        Long childId = Long.parseLong(parts[2]);

        taskService.addSubtask(parentId, childId, userId);
        return "✅ Задача #" + childId + " теперь подзадача #" + parentId;
    }

    private String handleDepend(String[] parts, Long userId) {
        if (parts.length < 3) {
            return "Использование: depend <id задачи> <id блокирующей>";
        }
        Long taskId = Long.parseLong(parts[1]);
        Long dependsOnId = Long.parseLong(parts[2]);

        taskService.addDependency(taskId, dependsOnId, userId);
        return "✅ Задача #" + taskId + " зависит от #" + dependsOnId;
    }

    private String usernameOf(Long userId) {
        User u = userService.findById(userId);
        return u != null ? u.username() : "?";
    }

    private String statusList() {
        StringBuilder sb = new StringBuilder();
        for (TaskStatus s : TaskStatus.values()) {
            sb.append(s.name().toLowerCase()).append(", ");
        }
        return sb.substring(0, sb.length() - 2);
    }
    
    private String getHelp() {
        return """
            🤖 Task Manager Bot — команды:

            СОЗДАНИЕ:
            create "Название"              — создать задачу
            create "Название" "Описание"   — создать задачу с описанием
            describe 5 "Описание"          — добавить описание к задаче #5

            РАБОТА С ЗАДАЧАМИ:
            assign 5 @Petrov               — назначить на Petrov
            status 5 in_progress           — изменить статус
            deadline 5 2026-10-15          — установить дедлайн
            my tasks                       — мои задачи

            ПРОСМОТР:
            list                           — все задачи (деревом)
            show 5                         — детали задачи #5
            overdue                        — просроченные задачи

            СВЯЗИ:
            subtask 1 5                    — сделать #5 подзадачей #1
            depend 5 3                     — #5 зависит от #3

            ПРОЧЕЕ:
            /help                          — эта справка

            Статусы: %s

            Примеры:
            create "Написать отчёт" "Отчёт за сентябрь"
            describe 1 "Добавить графики"
            assign 1 @Petrov
            status 1 in_progress
            """.formatted(statusList());
    }

}