package com.taskmanager.model;

/**
 * Статусы задачи.
 *
 * <p>Жизненный цикл:
 * <pre>
 *   BACKLOG → TO_DO → IN_PROGRESS → REVIEW → DONE
 *                        ↕
 *                     BLOCKED
 * </pre>
 */
public enum TaskStatus {

    BACKLOG("В бэклоге"),
    TO_DO("К выполнению"),
    IN_PROGRESS("В работе"),
    REVIEW("На проверке"),
    DONE("Завершена"),
    BLOCKED("Заблокирована");

    private final String displayName;

    TaskStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    // Проверки состояния

    /**
     * Финальный статус? (из него нельзя выйти)
     */
    public boolean isFinal() {
        return this == DONE;
    }

    /**
     * Активная работа?
     */
    public boolean isActive() {
        return this == IN_PROGRESS;
    }

    /**
     * Задача «открыта»? (не завершена)
     */
    public boolean isOpen() {
        return this != DONE;
    }

    /**
     * Можно ли начать работу?
     */
    public boolean canStart() {
        return this == TO_DO || this == BACKLOG;
    }

    // Переходы

    /**
     * Разрешён ли переход в новый статус?
     *
     * <p>Матрица переходов:
     * <pre>
     *   BACKLOG     → TO_DO
     *   TO_DO       → IN_PROGRESS, BACKLOG
     *   IN_PROGRESS → REVIEW, BLOCKED, TO_DO
     *   REVIEW      → DONE, IN_PROGRESS
     *   BLOCKED     → IN_PROGRESS, TO_DO
     *   DONE        → (финал)
     * </pre>
     */
    public boolean canTransitionTo(TaskStatus newStatus) {
        if (newStatus == null) return false;
        if (this == newStatus) return false;

        return switch (this) {
            case BACKLOG -> newStatus == TO_DO;
            case TO_DO -> newStatus == IN_PROGRESS || newStatus == BACKLOG;
            case IN_PROGRESS -> newStatus == REVIEW
                             || newStatus == BLOCKED
                             || newStatus == TO_DO;
            case REVIEW -> newStatus == DONE
                        || newStatus == IN_PROGRESS;
            case BLOCKED -> newStatus == IN_PROGRESS
                         || newStatus == TO_DO;
            case DONE -> false;
        };
    }

    // Парсинг

    /**
     * @throws IllegalArgumentException если value null/пустой/неизвестный
     */
    public static TaskStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Статус не может быть пустым");
        }
        try {
            return TaskStatus.valueOf(value.strip().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Неизвестный статус: " + value);
        }
    }
}