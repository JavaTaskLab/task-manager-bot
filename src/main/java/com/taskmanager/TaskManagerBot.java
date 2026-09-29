package com.taskmanager;

import com.google.gson.JsonObject;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
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

    public TaskManagerBot(Long groupId, String accessToken) {
        this.groupId = groupId;
        this.actor = new GroupActor(groupId, accessToken);
        this.vk = new VkApiClient(new HttpTransportClient());
    }

    public VkApiClient getVk() { return vk; }
    public GroupActor getActor() { return actor; }
    //public Long getGroupId() { return groupId; }

    /**
     * Обработка "сырого" JSON-объекта события из Long Poll.
     */
    public void handleRawUpdate(JsonObject update) {
        // Проверяем, что это событие нового сообщения
        if (!update.has("type") || !update.get("type").getAsString().equals("message_new")) {
            return;
        }

        JsonObject messageObject = update.getAsJsonObject("object").getAsJsonObject("message");
        String text = messageObject.get("text").getAsString();
        int peerId = messageObject.get("peer_id").getAsInt();

        logger.info("Получено сообщение (peerId={}): {}", peerId, text);

        switch (text) {
            case "/start":
                sendMessage(peerId, "Привет! Я бот-менеджер задач.\nНапиши /help, чтобы узнать, что я умею.");
                break;
            case "/help":
                sendMessage(peerId, "Доступные команды:\n/start — начать работу\n/help — справка");
                break;
            default:
                sendMessage(peerId, "Я получил твоё сообщение: " + text);
        }
    }

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