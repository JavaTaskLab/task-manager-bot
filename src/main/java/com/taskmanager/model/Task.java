package com.taskmanager.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Задача в системе управления проектами.
 */
public class Task {

    private Long id;                       // Уникальный идентификатор
    private String title;                  // Название задачи
    private String description;            // Описание (опционально)
    private TaskStatus status;             // Текущий статус
    private Long creatorId;                // ID создателя задачи
    private Long assigneeId;               // ID исполнителя (может быть null)
    private LocalDate deadline;            // Дедлайн (может быть null)
    private LocalDate createdAt;           // Дата создания
    private LocalDate updatedAt;           // Дата последнего обновления

    // Связи с другими задачами
    private List<Long> subtaskIds = new ArrayList<>();     // Подзадачи
    private List<Long> blockedByIds = new ArrayList<>();   // Задачи, которые блокируют эту
    private List<Long> blocksIds = new ArrayList<>();      // Задачи, которые блокирует эта

    // --- Конструкторы ---

    public Task() {
    }

    public Task(String title, Long creatorId) {
        this.title = title;
        this.creatorId = creatorId;
        this.status = TaskStatus.BACKLOG;
        this.createdAt = LocalDate.now();
        this.updatedAt = LocalDate.now();
    }

    // --- Геттеры и сеттеры ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public Long getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(Long creatorId) {
        this.creatorId = creatorId;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDate createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDate getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDate updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<Long> getSubtaskIds() {
        return subtaskIds;
    }

    public void setSubtaskIds(List<Long> subtaskIds) {
        this.subtaskIds = subtaskIds;
    }

    public List<Long> getBlockedByIds() {
        return blockedByIds;
    }

    public void setBlockedByIds(List<Long> blockedByIds) {
        this.blockedByIds = blockedByIds;
    }

    public List<Long> getBlocksIds() {
        return blocksIds;
    }

    public void setBlocksIds(List<Long> blocksIds) {
        this.blocksIds = blocksIds;
    }

    // --- Вспомогательные методы ---

    /**
     * Проверяет, просрочена ли задача (дедлайн прошёл, а статус не DONE).
     */
    public boolean isOverdue() {
        return deadline != null
                && status != TaskStatus.DONE
                && deadline.isBefore(LocalDate.now());
    }

    // --- equals, hashCode, toString ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return Objects.equals(id, task.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Task{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", status=" + status +
                ", assigneeId=" + assigneeId +
                ", deadline=" + deadline +
                '}';
    }
}