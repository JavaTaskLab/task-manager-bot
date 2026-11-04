package com.taskmanager;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.taskmanager.dialog.DialogManager;
import com.taskmanager.service.TaskService;
import com.taskmanager.service.UserService;
import com.taskmanager.util.Messages;
import com.taskmanager.vk.VkKeyboardBuilder;
import com.taskmanager.vk.dto.VkMessage;
import com.taskmanager.vk.dto.VkUpdate;
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
    private final DialogManager dialogManager;
    private final Gson gson = new Gson();

    public TaskManagerBot(Long groupId, String accessToken,
                          TaskService taskService, UserService userService) {
        this.groupId = groupId;
        this.accessToken = accessToken;
        this.httpClient = HttpClient.newHttpClient();
        this.dialogManager = new DialogManager(taskService, userService);
        logger.info("Бот создан. groupId={}", groupId);
    }

    public Long getGroupId() { return groupId; }
    public String getAccessToken() { return accessToken; }

    public void handleRawUpdate(JsonObject rawUpdate) {
        VkUpdate update = gson.fromJson(rawUpdate, VkUpdate.class);

        if (update.isMessageAllow()) {
            int userId = update.getAllowUserId();
            sendStartMessage(userId);
            return;
        }

        if (!update.isMessageNew()) return;

        VkMessage message = update.getMessage();
        if (message == null || message.text == null) return;

        if (message.text.equals(Messages.get("vk.btn_start")) || message.text.equals("/start")) {
            sendMainMenu(message.peer_id);
            return;
        }

        String command = mapButtonToCommand(message.text);
        String response = dialogManager.processCommand((long) message.peer_id, command);
        
        sendMessage(message.peer_id, response, buildMainKeyboard());
    }

    private void sendStartMessage(int peerId) {
        String startKeyboard = VkKeyboardBuilder.create()
                .oneTime(true)
                .addTextButton(Messages.get("vk.btn_start"), "positive")
                .build();
        sendMessage(peerId, Messages.get("vk.start_welcome"), startKeyboard);
    }

    private void sendMainMenu(int peerId) {
        sendMessage(peerId, Messages.get("vk.menu_main"), buildMainKeyboard());
    }

    private String mapButtonToCommand(String text) {
        if (text.equals(Messages.get("vk.btn_help"))) return "/help";
        if (text.equals(Messages.get("vk.btn_my_tasks"))) return "my tasks";
        if (text.equals(Messages.get("vk.btn_all_tasks"))) return "list";
        if (text.equals(Messages.get("vk.btn_overdue"))) return "overdue";
        return text;
    }

    private String buildMainKeyboard() {
        return VkKeyboardBuilder.create()
                .oneTime(false)
                .addTextButton(Messages.get("vk.btn_my_tasks"), "primary")
                .addTextButton(Messages.get("vk.btn_all_tasks"), "secondary")
                .addRow()
                .addTextButton(Messages.get("vk.btn_overdue"), "negative")
                .addTextButton(Messages.get("vk.btn_help"), "secondary")
                .build();
    }

    /**
     * ИСПРАВЛЕНИЕ: Отправляем через POST, чтобы длинные сообщения (справка) не обрезались.
     */
        public void sendMessage(int peerId, String text, String keyboardJson) {
        // ЗАЩИТА: Если текст пустой, VK API вернет ошибку. Не отправляем.
        if (text == null || text.isBlank()) {
            logger.warn("Попытка отправить пустое сообщение в чат {}. Отменено.", peerId);
            return;
        }

        try {
            String formBody = "peer_id=" + peerId
                    + "&message=" + URLEncoder.encode(text, StandardCharsets.UTF_8)
                    + "&keyboard=" + URLEncoder.encode(keyboardJson, StandardCharsets.UTF_8)
                    + "&random_id=" + (System.currentTimeMillis() % Integer.MAX_VALUE)
                    + "&access_token=" + accessToken
                    + "&v=5.131";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.vk.com/method/messages.send"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            if (json.has("error")) {
                logger.error("VK API вернул ошибку: {}", json.getAsJsonObject("error"));
            } else {
                logger.info("Сообщение успешно отправлено (peerId={})", peerId);
            }

        } catch (Exception e) {
            logger.error("Не удалось отправить сообщение в чат {}", peerId, e);
        }
    }
}