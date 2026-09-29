package com.taskmanager;

import com.google.gson.JsonObject;
import com.taskmanager.dialog.DialogManager;
import com.taskmanager.model.Role;
import com.taskmanager.model.User;
import com.taskmanager.service.TaskService;
import com.taskmanager.service.UserService;
import com.taskmanager.storage.InMemoryTaskStorage;
import com.taskmanager.storage.InMemoryUserStorage;
import com.taskmanager.storage.TaskStorage;
import com.taskmanager.storage.UserStorage;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class TaskManagerBot {

    private static final Logger logger = LoggerFactory.getLogger(TaskManagerBot.class);

    private final VkApiClient vk;
    private final GroupActor actor;
    private final Long groupId;

    // Бизнес-логика
    private final UserService userService;
    private final TaskService taskService;
    private final DialogManager dialogManager;

    public TaskManagerBot(Long groupId, String accessToken) {
        this.groupId = groupId;
        this.actor = new GroupActor(groupId, accessToken);
        this.vk = new VkApiClient(new HttpTransportClient());

        // --- Инициализация хранилищ и сервисов ---
        TaskStorage taskStorage = new InMemoryTaskStorage();
        UserStorage userStorage = new InMemoryUserStorage();

        this.userService = new UserService(userStorage);
        this.taskService = new TaskService(taskStorage, userStorage);
        this.dialogManager = new DialogManager(taskService, userService);

        logger.info("Токен получен, длина: {}", accessToken != null ? accessToken.length() : "null");
        logger.info("Бизнес-логика инициализирована");
    }

    public VkApiClient getVk() { return vk; }
    public GroupActor getActor() { return actor; }
    public Long getGroupId() { return groupId; }

    /**
     * Обработка "сырого" JSON-объекта события из Long Poll.
     */
    public void handleRawUpdate(JsonObject update) {
        if (!update.has("type") || !update.get("type").getAsString().equals("message_new")) {
            return;
        }

        JsonObject messageObject = update.getAsJsonObject("object").getAsJsonObject("message");
        String text = messageObject.get("text").getAsString();
        int peerId = messageObject.get("peer_id").getAsInt();
        int fromId = messageObject.get("from_id").getAsInt();

        logger.info("Получено сообщение (peerId={}, fromId={}): {}", peerId, fromId, text);

        // В личке peerId == userId; в беседе peerId > 2_000_000_000
        long userId = (peerId > 2_000_000_000L) ? fromId : peerId;

        // Регистрируем пользователя (первый становится MANAGER)
        ensureUserExists(userId);

        // Передаём в бизнес-логику
        String response;
        try {
            response = dialogManager.processCommand(userId, text);
        } catch (Exception e) {
            logger.error("Ошибка при обработке команды", e);
            response = "❌ Внутренняя ошибка: " + e.getMessage();
        }

        sendMessage(peerId, response);
    }

    /**
     * Создаёт пользователя в UserService, если его ещё нет.
     * Первый зарегистрированный получает роль MANAGER.
     */
    private void ensureUserExists(long userId) {
        if (userService.findById(userId) != null) {
            return;
        }
        Role role = userService.findAll().isEmpty() ? Role.MANAGER : Role.EXECUTOR;
        User created = User.create("user_" + userId, role).withId(userId);
        userService.save(created);
        logger.info("Зарегистрирован новый пользователь: id={}, role={}", userId, role);
    }

    /**
     * Отправка сообщения через прямой HTTP-запрос к VK API.
     */
    public void sendMessage(int peerId, String text) {
        try {
            String url = "https://api.vk.com/method/messages.send"
                    + "?peer_id=" + peerId
                    + "&message=" + java.net.URLEncoder.encode(text, "UTF-8")
                    + "&random_id=" + (System.currentTimeMillis() % Integer.MAX_VALUE)
                    + "&access_token=" + actor.getAccessToken()
                    + "&v=5.131";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            logger.info("Результат отправки: {}", response.body());
        } catch (Exception e) {
            logger.error("Не удалось отправить сообщение", e);
        }
    }
}