package com.taskmanager.dialog;

import com.taskmanager.model.*;
import com.taskmanager.service.TaskService;
import com.taskmanager.service.UserService;
import com.taskmanager.util.Messages;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class DialogManager {

    private final TaskService taskService;
    private final UserService userService;

    public DialogManager(TaskService taskService, UserService userService) {
        this.taskService = taskService;
        this.userService = userService;
    }

    public String processCommand(Long userId, String input) {
        if (input == null || input.isBlank()) {
            return Messages.get("dialog.empty_command");
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
                default -> Messages.get("dialog.unknown_command", command);
            };
        } catch (Exception e) {
            e.printStackTrace();
            String msg = e.getMessage();
            return Messages.get("dialog.error", msg != null ? msg : e.getClass().getSimpleName());
        }
    }

    private String handleCreate(String[] parts, Long userId) {
        String rest = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
        List<String> args = extractQuoted(rest);

        if (args.isEmpty()) {
            return Messages.get("dialog.create_usage");
        }

        String title = args.get(0);
        String description = args.size() > 1 ? args.get(1) : null;

        Task task = (description != null)
                ? taskService.createTask(title, description, userId)
                : taskService.createTask(title, userId);

        return (description != null) 
                ? Messages.get("dialog.create_success_desc", task.id(), task.title(), description)
                : Messages.get("dialog.create_success", task.id(), task.title());
    }

    private String handleDescribe(String[] parts, Long userId) {
        if (parts.length < 3) {
            return Messages.get("dialog.describe_usage");
        }
        Long taskId = Long.parseLong(parts[1]);
        String rest = String.join(" ", Arrays.copyOfRange(parts, 2, parts.length));
        List<String> args = extractQuoted(rest);

        if (args.isEmpty()) {
            return Messages.get("dialog.describe_quotes");
        }

        Task task = taskService.setDescription(taskId, args.get(0), userId);
        return Messages.get("dialog.describe_success", task.id());
    }

    private List<String> extractQuoted(String input) {
        List<String> result = new ArrayList<>();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"([^\"]*)\"").matcher(input);
        while (m.find()) {
            result.add(m.group(1));
        }
        if (result.isEmpty()) {
            String trimmed = input.strip();
            if (!trimmed.isEmpty()) result.add(trimmed);
        }
        return result;
    }

    private String handleAssign(String[] parts, Long userId) {
        if (parts.length < 3) {
            return Messages.get("dialog.assign_usage");
        }
        Long taskId = Long.parseLong(parts[1]);
        String username = parts[2].startsWith("@") ? parts[2].substring(1) : parts[2];

        User assignee = userService.findByUsername(username);
        if (assignee == null) {
            return Messages.get("dialog.assign_user_not_found", username);
        }

        Task task = taskService.assignTask(taskId, assignee.id(), userId);
        return Messages.get("dialog.assign_success", task.title(), username);
    }

    private String handleStatus(String[] parts, Long userId) {
        if (parts.length < 3) {
            return Messages.get("dialog.status_usage", statusList());
        }
        Long taskId = Long.parseLong(parts[1]);
        TaskStatus status = TaskStatus.fromString(parts[2]);

        Task task = taskService.changeStatus(taskId, status, userId);
        return Messages.get("dialog.status_success", task.title(), status.getDisplayName());
    }

    private String handleMyTasks(Long userId) {
        var tasks = taskService.getMyTasks(userId);
        if (tasks.isEmpty()) {
            return Messages.get("dialog.my_tasks_empty");
        }

        StringBuilder sb = new StringBuilder(Messages.get("dialog.my_tasks_header"));
        for (Task t : tasks) {
            sb.append(Messages.get("dialog.my_tasks_item", t.id(), t.title(), t.status().getDisplayName()));
            if (t.deadline() != null) {
                sb.append(Messages.get("dialog.my_tasks_deadline", t.deadline().toLocalDate()));
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String handleDeadline(String[] parts, Long userId) {
        if (parts.length < 3) {
            return Messages.get("dialog.deadline_usage");
        }
        Long taskId = Long.parseLong(parts[1]);
        LocalDateTime deadline = LocalDate.parse(parts[2]).atStartOfDay();

        Task task = taskService.setDeadline(taskId, deadline, userId);
        return Messages.get("dialog.deadline_success", task.title(), parts[2]);
    }

    private String handleOverdue() {
        var tasks = taskService.getOverdueTasks(LocalDateTime.now());
        if (tasks.isEmpty()) {
            return Messages.get("dialog.overdue_empty");
        }

        StringBuilder sb = new StringBuilder(Messages.get("dialog.overdue_header"));
        for (Task t : tasks) {
            sb.append(Messages.get("dialog.overdue_item", t.id(), t.title(), t.deadline().toLocalDate()));
            sb.append("\n");
        }
        return sb.toString();
    }

    private String handleList() {
        var allTasks = taskService.getAllTasks();
        if (allTasks.isEmpty()) return Messages.get("dialog.list_empty");

        Set<Long> subtaskIds = allTasks.stream()
                .flatMap(t -> t.subtaskIds().stream())
                .collect(Collectors.toSet());

        List<Task> roots = allTasks.stream()
                .filter(t -> !subtaskIds.contains(t.id()))
                .sorted(Comparator.comparing(Task::id))
                .toList();

        StringBuilder sb = new StringBuilder(Messages.get("dialog.list_header"));

        for (int i = 0; i < roots.size(); i++) {
            appendTaskTree(sb, roots.get(i), "", true, true, allTasks);
            if (i < roots.size() - 1) sb.append("\n");
        }
        return sb.toString().stripTrailing();
    }

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
                    .ifPresent(child -> appendTaskTree(sb, child, childPrefix, childIsLast, false, allTasks));
        }
    }

    private String handleShow(String[] parts) {
        if (parts.length < 2) {
            return Messages.get("dialog.show_usage");
        }
        Long taskId = Long.parseLong(parts[1]);
        Task task = taskService.getTask(taskId);
        return formatTask(task);
    }

    private String formatTask(Task t) {
        StringBuilder sb = new StringBuilder();
        sb.append(Messages.get("dialog.show_header", 
            t.id(), 
            t.title(), 
            t.description() != null ? t.description() : "-",
            t.status().getDisplayName(),
            usernameOf(t.creatorId())
        ));

        if (t.assigneeId() != null) {
            sb.append(Messages.get("dialog.show_assignee", usernameOf(t.assigneeId())));
        }
        if (t.deadline() != null) {
            sb.append(Messages.get("dialog.show_deadline", t.deadline().toLocalDate()));
        }
        sb.append(Messages.get("dialog.show_created", t.createdAt().toLocalDate()));

        if (t.hasSubtasks()) {
            sb.append(Messages.get("dialog.show_subtasks"));
            for (Long id : t.subtaskIds()) sb.append("#").append(id).append(" ");
            sb.append("\n");
        }
        if (t.isBlocked()) {
            sb.append(Messages.get("dialog.show_blocked"));
            for (Long id : t.blockedByIds()) sb.append("#").append(id).append(" ");
            sb.append("\n");
        }
        return sb.toString();
    }

    private String handleSubtask(String[] parts, Long userId) {
        if (parts.length < 3) {
            return Messages.get("dialog.subtask_usage");
        }
        Long parentId = Long.parseLong(parts[1]);
        Long childId = Long.parseLong(parts[2]);

        taskService.addSubtask(parentId, childId, userId);
        return Messages.get("dialog.subtask_success", childId, parentId);
    }

    private String handleDepend(String[] parts, Long userId) {
        if (parts.length < 3) {
            return Messages.get("dialog.depend_usage");
        }
        Long taskId = Long.parseLong(parts[1]);
        Long dependsOnId = Long.parseLong(parts[2]);

        taskService.addDependency(taskId, dependsOnId, userId);
        return Messages.get("dialog.depend_success", taskId, dependsOnId);
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
        return Messages.get("dialog.help", statusList());
    }
}