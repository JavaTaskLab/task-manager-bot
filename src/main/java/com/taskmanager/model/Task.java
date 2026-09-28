package com.taskmanager.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public record Task(
        Long id,
        String title,
        String description,
        TaskStatus status,
        Long creatorId,
        Long assigneeId,
        LocalDateTime deadline,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<Long> subtaskIds,
        List<Long> blockedByIds,
        List<Long> blocksIds
) {
    public static final int MAX_TITLE_LENGTH = 200;
    public static final int MAX_DESCRIPTION_LENGTH = 2000;


    /**
     * @throws IllegalArgumentException если title null/пустой/длиннее {@value MAX_TITLE_LENGTH},
     *                                  description длиннее {@value MAX_DESCRIPTION_LENGTH},
     *                                  creatorId == null,
     *                                  id <= 0,
     *                                  updatedAt < createdAt
     */
    public Task {
        if (title == null) {
            throw new IllegalArgumentException("Название задачи не может быть null");
        }
        String trimmedTitle = title.strip();
        if (trimmedTitle.isEmpty()) {
            throw new IllegalArgumentException("Название задачи не может быть пустым");
        }
        if (trimmedTitle.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException(
                "Название задачи не может быть длиннее " + MAX_TITLE_LENGTH + " символов");
        }
        title = trimmedTitle;

        if (description != null) {
            String trimmedDesc = description.strip();
            if (trimmedDesc.length() > MAX_DESCRIPTION_LENGTH) {
                throw new IllegalArgumentException(
                    "Описание не может быть длиннее " + MAX_DESCRIPTION_LENGTH + " символов");
            }
            description = trimmedDesc.isEmpty() ? null : trimmedDesc;
        }

        if (creatorId == null) {
            throw new IllegalArgumentException("creatorId не может быть null");
        }

        if (id != null && id <= 0) {
            throw new IllegalArgumentException("ID должен быть положительным");
        }

        if (status == null) {
            status = TaskStatus.BACKLOG;
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = createdAt;
        }

        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("updatedAt не может быть раньше createdAt");
        }

        subtaskIds = safeCopy(subtaskIds, "subtaskIds");
        blockedByIds = safeCopy(blockedByIds, "blockedByIds");
        blocksIds = safeCopy(blocksIds, "blocksIds");
    }

    private static List<Long> safeCopy(List<Long> src, String fieldName) {
    if (src == null) {
        return List.of();
    }
    if (src.stream().anyMatch(Objects::isNull)) {
        throw new IllegalArgumentException(fieldName + " не может содержать null");
    }
    return List.copyOf(src);
}

    /**
     * Создаёт новую задачу (id = null, будет присвоен storage).
     * updatedAt = createdAt.
     *
     * @throws IllegalArgumentException если title null/пустой/длиннее {@value MAX_TITLE_LENGTH},
     *                                  creatorId == null
     */
    public static Task create(String title, Long creatorId) {
        return create(title, null, creatorId);
    }

    /**
     * Создаёт новую задачу с описанием.
     *
     * @throws IllegalArgumentException если title null/пустой/длиннее {@value MAX_TITLE_LENGTH},
     *                                  description длиннее {@value MAX_DESCRIPTION_LENGTH},
     *                                  creatorId == null
     */
    public static Task create(String title, String description, Long creatorId) {
        LocalDateTime now = LocalDateTime.now();
        return new Task(
                null, title, description, TaskStatus.BACKLOG,
                creatorId, null, null,
                now, now,
                List.of(), List.of(), List.of()
        );
    }

    /**
     * ⚠️ Использовать ТОЛЬКО из storage.save() при первом сохранении.
     * После присвоения id — НЕ менять.
     *
     * @throws IllegalStateException    если id уже присвоен
     * @throws IllegalArgumentException если newId == null или newId <= 0
     */
    public Task withId(Long newId) {
        if (this.id != null) {
            throw new IllegalStateException("id уже присвоен и не может быть изменён");
        }
        if (newId == null) {
            throw new IllegalArgumentException("newId не может быть null");
        }
        if (newId <= 0) {
            throw new IllegalArgumentException("newId должен быть положительным");
        }
        return new Task(newId, title, description, status, creatorId,
                assigneeId, deadline, createdAt, updatedAt,
                subtaskIds, blockedByIds, blocksIds);
    }

    /**
     * @throws IllegalArgumentException если newTitle null/пустой/длиннее {@value MAX_TITLE_LENGTH}
     */
    public Task withTitle(String newTitle) {
        return new Task(id, newTitle, description, status, creatorId,
                assigneeId, deadline, createdAt, updatedAt,
                subtaskIds, blockedByIds, blocksIds);
    }

    /**
     * @param newDescription может быть null (очистить описание)
     * @throws IllegalArgumentException если newDescription длиннее {@value MAX_DESCRIPTION_LENGTH}
     */
    public Task withDescription(String newDescription) {
        return new Task(id, title, newDescription, status, creatorId,
                assigneeId, deadline, createdAt, updatedAt,
                subtaskIds, blockedByIds, blocksIds);
    }

    /**
     * @throws IllegalArgumentException если newStatus == null
     */
    public Task withStatus(TaskStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("Статус не может быть null");
        }
        return new Task(id, title, description, newStatus, creatorId,
                assigneeId, deadline, createdAt, updatedAt,
                subtaskIds, blockedByIds, blocksIds);
    }

    /**
     * @param newAssigneeId может быть null (снять исполнителя)
     */
    public Task withAssigneeId(Long newAssigneeId) {
        return new Task(id, title, description, status, creatorId,
                newAssigneeId, deadline, createdAt, updatedAt,
                subtaskIds, blockedByIds, blocksIds);
    }

    /**
     * @param newDeadline может быть null (снять дедлайн)
     */
    public Task withDeadline(LocalDateTime newDeadline) {
        return new Task(id, title, description, status, creatorId,
                assigneeId, newDeadline, createdAt, updatedAt,
                subtaskIds, blockedByIds, blocksIds);
    }

    /**
     *
     * @param now текущее время
     * @throws IllegalArgumentException если now == null,
     *                                  now < createdAt,
     *                                  или now <= updatedAt
     */
    public Task touchedAt(LocalDateTime now) {
        if (now == null) {
            throw new IllegalArgumentException("now не может быть null");
        }
        if (now.isBefore(createdAt)) {
            throw new IllegalArgumentException("updatedAt не может быть раньше createdAt");
        }
        if (!now.isAfter(updatedAt)) {
            throw new IllegalArgumentException("updatedAt должен быть строго позже предыдущего");
        }
        return new Task(id, title, description, status, creatorId,
                assigneeId, deadline, createdAt, now,
                subtaskIds, blockedByIds, blocksIds);
    }

    private Task withSubtaskIds(List<Long> newSubtaskIds) {
        return new Task(id, title, description, status, creatorId,
                assigneeId, deadline, createdAt, updatedAt,
                newSubtaskIds, blockedByIds, blocksIds);
    }

    private Task withBlockedByIds(List<Long> newBlockedByIds) {
        return new Task(id, title, description, status, creatorId,
                assigneeId, deadline, createdAt, updatedAt,
                subtaskIds, newBlockedByIds, blocksIds);
    }

    private Task withBlocksIds(List<Long> newBlocksIds) {
        return new Task(id, title, description, status, creatorId,
                assigneeId, deadline, createdAt, updatedAt,
                subtaskIds, blockedByIds, newBlocksIds);
    }

    /**
     * @throws IllegalArgumentException если subtaskId == null или == id
     */
    public Task addSubtask(Long subtaskId) {
        if (Objects.equals(id, subtaskId)) {
            throw new IllegalArgumentException("Задача не может быть подзадачей самой себя");
        }
        return withSubtaskIds(addToList(subtaskIds, subtaskId, "subtaskId"));
    }

    /**
     * @throws IllegalArgumentException если subtaskId == null
     */
    public Task removeSubtask(Long subtaskId) {
        if (subtaskId == null) {
            throw new IllegalArgumentException("subtaskId не может быть null");
        }
        return withSubtaskIds(removeFromList(subtaskIds, subtaskId, "subtaskId"));
    }

    /**
     * @throws IllegalArgumentException если blockingId == null или == id
     */
    public Task addBlockedBy(Long blockingId) {
        if (Objects.equals(id, blockingId)) {
            throw new IllegalArgumentException("Задача не может блокировать саму себя");
        }
        return withBlockedByIds(addToList(blockedByIds, blockingId, "blockingId"));
    }

    /**
     * @throws IllegalArgumentException если blockingId == null
     */
    public Task removeBlockedBy(Long blockingId) {
        if (blockingId == null) {
            throw new IllegalArgumentException("blockingId не может быть null");
        }
        return withBlockedByIds(removeFromList(blockedByIds, blockingId, "blockingId"));
    }

    /**
     * @throws IllegalArgumentException если blockedId == null или == id
     */
    public Task addBlocks(Long blockedId) {
        if (Objects.equals(id, blockedId)) {
            throw new IllegalArgumentException("Задача не может блокировать саму себя");
        }
        return withBlocksIds(addToList(blocksIds, blockedId, "blockedId"));
    }

    /**
     * @throws IllegalArgumentException если blockedId == null
     */
    public Task removeBlocks(Long blockedId) {
        if (blockedId == null) {
            throw new IllegalArgumentException("blockedId не может быть null");
        }
        return withBlocksIds(removeFromList(blocksIds, blockedId, "blockedId"));
    }

    private static List<Long> addToList(List<Long> src, Long value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " не может быть null");
        }
        if (src.contains(value)) {
            return src;
        }
        List<Long> copy = new ArrayList<>(src);
        copy.add(value);
        return copy;
    }

    private static List<Long> removeFromList(List<Long> src, Long value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " не может быть null");
        }
        if (!src.contains(value)) {
            return src;
        }
        List<Long> copy = new ArrayList<>(src);
        copy.remove(value);
        return copy;
    }

    /**
     * Проверяет просрочку.
     *
     * @param now текущее время (передаётся снаружи для тестируемости)
     */
    public boolean isOverdue(LocalDateTime now) {
        return deadline != null
                && !isDone()
                && deadline.isBefore(now);
    }

    public boolean isBlocked() {
        return !blockedByIds.isEmpty();
    }

    public boolean hasSubtasks() {
        return !subtaskIds.isEmpty();
    }

    public boolean isDone() {
        return status == TaskStatus.DONE;
    }
}