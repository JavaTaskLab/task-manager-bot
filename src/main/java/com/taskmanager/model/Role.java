package com.taskmanager.model;

/**
 * Роли пользователей в системе.
 *
 * <ul>
 *   <li>{@link #MANAGER} — видит все задачи, назначает исполнителей, ставит дедлайны.</li>
 *   <li>{@link #EXECUTOR} — видит свои задачи, меняет их статус.</li>
 *   <li>{@link #OBSERVER} — только просмотр.</li>
 * </ul>
 */
public enum Role {

    MANAGER("Менеджер"),
    EXECUTOR("Исполнитель"),
    OBSERVER("Наблюдатель");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    // Права доступа

    public boolean canCreateTasks() {
        return this == MANAGER || this == EXECUTOR;
    }

    public boolean canAssignTasks() {
        return this == MANAGER;
    }

    public boolean canChangeStatus() {
        return this == MANAGER || this == EXECUTOR;
    }

    public boolean canSetDeadline() {
        return this == MANAGER;
    }

    public boolean isReadOnly() {
        return this == OBSERVER;
    }

    /**
     * @throws IllegalArgumentException если value null/пустой/неизвестный
     */
    public static Role fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Роль не может быть пустой");
        }
        try {
            return Role.valueOf(value.strip().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Неизвестная роль: " + value);
        }
    }
}