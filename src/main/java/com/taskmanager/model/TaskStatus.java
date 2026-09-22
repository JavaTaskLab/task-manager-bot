package com.taskmanager.model;

/**
 * Статусы задачи.
 * Жизненный цикл: BACKLOG → TO_DO → IN_PROGRESS → REVIEW → DONE
 * BLOCKED — если задача заблокирована другой задачей.
 */
public enum TaskStatus {
    BACKLOG,       // В бэклоге, ещё не запланирована
    TO_DO,         // Запланирована к выполнению
    IN_PROGRESS,   // В работе
    REVIEW,        // На проверке
    DONE,          // Завершена
    BLOCKED        // Заблокирована другой задачей
}