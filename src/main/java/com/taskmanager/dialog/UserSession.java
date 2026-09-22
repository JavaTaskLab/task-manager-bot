package com.taskmanager.dialog;

/**
 * Сессия пользователя. Пока пустая — задел на будущее.
 */
public class UserSession {
    private final Long userId;
    private String lastCommand;

    public UserSession(Long userId) {
        this.userId = userId;
    }

    public Long getUserId() { return userId; }
    public String getLastCommand() { return lastCommand; }
    public void setLastCommand(String lastCommand) { this.lastCommand = lastCommand; }
}