package com.taskmanager;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.taskmanager.dialog.DialogManager;
import com.taskmanager.vk.ButtonMapper;
import com.taskmanager.vk.VkKeyboardBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class TaskManagerBot {

    private static final Logger logger = LoggerFactory.getLogger(TaskManagerBot.class);

    private final Long groupId;
    private final String accessToken;
    private final HttpClient httpClient;

    // Зависимости
    private final DialogManager dialogManager;
    private final VkKeyboardBuilder keyboardBuilder;
    private final ButtonMapper buttonMapper;

    public TaskManagerBot(Long groupId, String accessToken, DialogManager dialogManager) {
        this.groupId = groupId;
        this.accessToken = accessToken;
        this.httpClient = HttpClient.newHttpClient();
        this.dialogManager = dialogManager;
        this.keyboardBuilder = new VkKeyboardBuilder();
        this.buttonMapper = new ButtonMapper();
    }

    public Long getGroupId() { return groupId; }
    public String getAccessToken() { return accessToken; }

    public void handleRawUpdate(JsonObject update) {
        String type = update.has("type") ? update.get("type").getAsString() : "";

        if (type.equals("message_allow")) {
            int userId = update.getAsJsonObject("object").get("user_id").getAsInt();
            sendMessage(userId, "👋 Добро пожаловать! Нажмите кнопку ниже.", keyboardBuilder.buildStartKeyboard());
            return;
        }

        if (!type.equals("message_new")) return;

        JsonObject messageObject = update.getAsJsonObject("object").getAsJsonObject("message");
        String text = messageObject.get("text").getAsString();
        int peerId = messageObject.get("peer_id").getAsInt();

        logger.info("Получено сообщение (peerId={}): {}", peerId, text);

        // 1. Маппим текст в команду
        String command = buttonMapper.toCommand(text);

        // 2. Получаем текстовый ответ от DialogManager
        String response = dialogManager.processCommand((long) peerId, command);

        // 3. Выбираем клавиатуру (стартовую или основную)
        String keyboard = text.equals("🏠 Начать") || text.equals("/start")
                ? keyboardBuilder.buildMainKeyboard()
                : keyboardBuilder.buildMainKeyboard(); // Пока всегда основная

        // 4. Отправляем
        sendMessage(peerId, response, keyboard);
    }

    private void sendMessage(int peerId, String text, String keyboardJson) {
        try {
            String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);
            String encodedKeyboard = URLEncoder.encode(keyboardJson, StandardCharsets.UTF_8);
            int randomId = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);

            String url = "https://api.vk.com/method/messages.send"
                    + "?peer_id=" + peerId
                    + "&message=" + encodedText
                    + "&keyboard=" + encodedKeyboard
                    + "&random_id=" + randomId
                    + "&access_token=" + accessToken
                    + "&v=5.131";

            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            if (json.has("error")) {
                logger.error("VK API вернул ошибку: {}", json.getAsJsonObject("error"));
            }
        } catch (Exception e) {
            logger.error("Не удалось отправить сообщение в чат {}", peerId, e);
        }
    }
}