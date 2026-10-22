package com.taskmanager.vk;

public class ButtonMapper {
    public static String toCommand(String buttonText) {
        return switch (buttonText) {
            case "🏠 Начать" -> "/start";
            case "📋 Мои задачи" -> "my tasks";
            case "📊 Все задачи" -> "list";
            case "⚠️ Просроченные" -> "overdue";
            case "❓ Помощь" -> "/help";
            default -> buttonText; // Обычный текст передаем как есть
        };
    }
}