package com.taskmanager.vk;

public class VkKeyboardBuilder {

    public String buildStartKeyboard() {
        return """
                {
                  "one_time": true,
                  "buttons": [
                    [{"action": {"type": "text", "label": "🏠 Начать"}, "color": "positive"}]
                  ]
                }
                """;
    }

    public String buildMainKeyboard() {
        return """
                {
                  "one_time": false,
                  "buttons": [
                    [{"action": {"type": "text", "label": "📋 Мои задачи"}, "color": "primary"},
                     {"action": {"type": "text", "label": "📊 Все задачи"}, "color": "secondary"}],
                    [{"action": {"type": "text", "label": "⚠️ Просроченные"}, "color": "negative"},
                     {"action": {"type": "text", "label": "❓ Помощь"}, "color": "secondary"}]
                  ]
                }
                """;
    }
}