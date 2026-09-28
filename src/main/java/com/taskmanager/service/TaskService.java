package com.taskmanager.service;

import com.taskmanager.exception.PermissionDeniedException;
import com.taskmanager.exception.TaskNotFoundException;
import com.taskmanager.model.*;
import com.taskmanager.storage.TaskStorage;
import com.taskmanager.storage.UserStorage;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TaskService {

    private final TaskStorage taskStorage;
    private final UserStorage userStorage;

    public TaskService(TaskStorage taskStorage, UserStorage userStorage) {
        this.taskStorage = taskStorage;
        this.userStorage = userStorage;
    }


    public Task createTask(String title, Long creatorId) {
        requireUser(creatorId);
        Task task = Task.create(title, creatorId);
        return taskStorage.save(task);
    }

    public Task createTask(String title, String description, Long creatorId) {
        requireUser(creatorId);
        Task task = Task.create(title, description, creatorId);
        return taskStorage.save(task);
    }

    public Task setDescription(Long taskId, String description, Long actorId) {
        Task task = requireTask(taskId);
        User actor = requireUser(actorId);

        if (!actor.role().canCreateTasks()) {
            throw new PermissionDeniedException("Нет прав на изменение описания");
        }

        return taskStorage.save(task.withDescription(description));
    }

    public Task assignTask(Long taskId, Long assigneeId, Long actorId) {
        Task task = requireTask(taskId);
        User actor = requireUser(actorId);

        if (!actor.role().canAssignTasks()) {
            throw new PermissionDeniedException("Только менеджер может назначать задачи");
        }

        if (assigneeId != null) {
            requireUser(assigneeId);
        }

        return taskStorage.save(task.withAssigneeId(assigneeId));
    }


    public Task changeStatus(Long taskId, TaskStatus newStatus, Long actorId) {
        Task task = requireTask(taskId);
        User actor = requireUser(actorId);

        if (!actor.role().canChangeStatus()) {
            throw new PermissionDeniedException("Нет прав на изменение статуса");
        }

        if (!task.status().canTransitionTo(newStatus)) {
            throw new IllegalArgumentException(
                "Нельзя перейти из " + task.status().getDisplayName()
                + " в " + newStatus.getDisplayName());
        }

        if (newStatus == TaskStatus.IN_PROGRESS) {
            checkParentStarted(task);
        }

        if (newStatus == TaskStatus.DONE) {
            checkSubtasksDone(task);
        }

        if (newStatus == TaskStatus.IN_PROGRESS) {
            checkNotBlocked(task);
        }

        Task updated = taskStorage.save(task.withStatus(newStatus));

        if (newStatus == TaskStatus.DONE) {
            unblockDependentTasks(taskId);
        }

        return updated;
    }

    public Task setDeadline(Long taskId, LocalDateTime deadline, Long actorId) {
        Task task = requireTask(taskId);
        User actor = requireUser(actorId);

        if (!actor.role().canSetDeadline()) {
            throw new PermissionDeniedException("Только менеджер может ставить дедлайн");
        }

        return taskStorage.save(task.withDeadline(deadline));
    }

    /**
     * 
     *
     * @throws IllegalArgumentException если:
     *   
     */
    public void addSubtask(Long parentId, Long childId, Long actorId) {
        Task parent = requireTask(parentId);
        Task child = requireTask(childId);
        User actor = requireUser(actorId);

        if (!actor.role().canCreateTasks()) {
            throw new PermissionDeniedException("Нет прав на создание подзадач");
        }

        if (parentId.equals(childId)) {
            throw new IllegalArgumentException("Задача не может быть подзадачей самой себя");
        }

        Long existingParent = findParentId(childId);
        if (existingParent != null && !existingParent.equals(parentId)) {
            throw new IllegalArgumentException(
                "Задача #" + childId + " уже является подзадачей #" + existingParent);
        }

        if (isDescendant(childId, parentId)) {
            throw new IllegalArgumentException(
                "Нельзя добавить: задача #" + parentId
                + " уже является потомком #" + childId);
        }

        taskStorage.save(parent.addSubtask(childId));
    }

    public void addDependency(Long taskId, Long dependsOnId, Long actorId) {
        Task task = requireTask(taskId);
        Task dependsOn = requireTask(dependsOnId);
        User actor = requireUser(actorId);

        if (!actor.role().canAssignTasks()) {
            throw new PermissionDeniedException("Только менеджер может добавлять зависимости");
        }

        if (taskId.equals(dependsOnId)) {
            throw new IllegalArgumentException("Задача не может зависеть от самой себя");
        }

        if (wouldCreateCycle(taskId, dependsOnId)) {
            throw new IllegalArgumentException("Зависимость создаст цикл");
        }

        taskStorage.save(task.addBlockedBy(dependsOnId));
        taskStorage.save(dependsOn.addBlocks(taskId));
    }

    public Task getTask(Long taskId) {
        return requireTask(taskId);
    }

    public List<Task> getMyTasks(Long userId) {
        requireUser(userId);
        return taskStorage.findByAssigneeId(userId);
    }

    public List<Task> getOverdueTasks(LocalDateTime now) {
        return taskStorage.findOverdue(now);
    }

    public List<Task> getAllTasks() {
        return taskStorage.findAll();
    }

    public List<Task> getTasksByStatus(TaskStatus status) {
        return taskStorage.findByStatus(status);
    }

    /**
     * подзадача не может начаться раньше родителя.
     * Если у задачи есть родитель — он должен быть в IN_PROGRESS/REVIEW/DONE.
     */
    private void checkParentStarted(Task task) {
        Long parentId = findParentId(task.id());
        if (parentId == null) {
            return;   // не подзадача
        }

        Task parent = requireTask(parentId);
        TaskStatus ps = parent.status();

        if (ps == TaskStatus.BACKLOG || ps == TaskStatus.TO_DO) {
            throw new IllegalStateException(
                "Нельзя начать подзадачу #" + task.id()
                + ": родительская задача #" + parentId
                + " ещё не начата (статус: " + ps.getDisplayName() + ")");
        }
    }

    /**
     * родитель не может завершиться раньше подзадач.
     * Все подзадачи должны быть в DONE.
     */
    private void checkSubtasksDone(Task task) {
        for (Long subtaskId : task.subtaskIds()) {
            Task subtask = requireTask(subtaskId);
            if (!subtask.isDone()) {
                throw new IllegalStateException(
                    "Нельзя завершить задачу #" + task.id()
                    + ": подзадача #" + subtaskId
                    + " ещё не завершена (статус: "
                    + subtask.status().getDisplayName() + ")");
            }
        }
    }

    /**
     * Находит родителя задачи (если есть).
     */
    private Long findParentId(Long taskId) {
        for (Task t : taskStorage.findAll()) {
            if (t.subtaskIds().contains(taskId)) {
                return t.id();
            }
        }
        return null;
    }

    /**
     * Проверяет, является ли {@code candidateId} потомком {@code ancestorId}.
     */
    private boolean isDescendant(Long ancestorId, Long candidateId) {
        Task ancestor = requireTask(ancestorId);
        for (Long childId : ancestor.subtaskIds()) {
            if (childId.equals(candidateId)) return true;
            if (isDescendant(childId, candidateId)) return true;
        }
        return false;
    }

    private void checkNotBlocked(Task task) {
        for (Long blockingId : task.blockedByIds()) {
            Task blocking = requireTask(blockingId);
            if (!blocking.isDone()) {
                throw new IllegalStateException(
                    "Задача заблокирована задачей #" + blockingId);
            }
        }
    }

    private void unblockDependentTasks(Long doneTaskId) {
        Task doneTask = requireTask(doneTaskId);
        for (Long blockedId : doneTask.blocksIds()) {
            taskStorage.findById(blockedId).ifPresent(blocked -> {
                if (blocked.status() == TaskStatus.BLOCKED) {
                    // ✅ Проверяем правило A: родитель начат?
                    Long parentId = findParentId(blockedId);
                    if (parentId != null) {
                        Task parent = requireTask(parentId);
                        if (parent.status() == TaskStatus.BACKLOG
                                || parent.status() == TaskStatus.TO_DO) {
                            return;   // оставляем BLOCKED
                        }
                    }
                    taskStorage.save(blocked.withStatus(TaskStatus.TO_DO));
                }
            });
        }
    }

    private boolean wouldCreateCycle(Long taskId, Long dependsOnId) {
        Set<Long> visited = new HashSet<>();
        return hasPath(dependsOnId, taskId, visited);
    }

    private boolean hasPath(Long from, Long to, Set<Long> visited) {
        if (from.equals(to)) return true;
        if (!visited.add(from)) return false;

        return taskStorage.findById(from)
                .map(task -> task.blockedByIds().stream()
                        .anyMatch(blockedBy -> hasPath(blockedBy, to, visited)))
                .orElse(false);
    }

    private Task requireTask(Long taskId) {
        return taskStorage.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Задача не найдена: " + taskId));
    }

    private User requireUser(Long userId) {
        return userStorage.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден: " + userId));
    }
}