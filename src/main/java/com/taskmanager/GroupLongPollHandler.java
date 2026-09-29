package com.taskmanager;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.objects.groups.responses.GetLongPollServerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class GroupLongPollHandler {

    private static final Logger logger = LoggerFactory.getLogger(GroupLongPollHandler.class);

    private final VkApiClient vk;
    private final GroupActor actor;
    private final Long groupId;
    private final TaskManagerBot bot;
    private final Gson gson = new Gson();

    private String server;
    private String key;
    private int ts;

    public GroupLongPollHandler(VkApiClient vk, GroupActor actor, Long groupId, TaskManagerBot bot) {
        this.vk = vk;
        this.actor = actor;
        this.groupId = groupId;
        this.bot = bot;
    }

    /**
     * Получает адрес Long Poll сервера и ключ.
     */
    public void init() throws ClientException, ApiException {
        GetLongPollServerResponse response = vk.groups()
                .getLongPollServer(actor, groupId)
                .execute();

        this.server = response.getServer().toString();
        this.key = response.getKey();
        this.ts = Integer.parseInt(response.getTs());

        logger.info("Long Poll инициализирован: server={}, ts={}", server, ts);
    }

    /**
     * Бесконечный цикл опроса Long Poll.
     */
    public void run() {
        while (true) {
            try {
                String urlString = server
                        + "?act=a_check&key=" + key
                        + "&ts=" + ts
                        + "&wait=25";
                String jsonResponse = loadJson(urlString);
                JsonObject root = gson.fromJson(jsonResponse, JsonObject.class);

                if (root == null) {
                    continue;
                }

                // Проверяем, не пришла ли ошибка от VK
                if (root.has("failed")) {
                    int failed = root.get("failed").getAsInt();
                    logger.warn("Long Poll вернул failed={}, переинициализация", failed);
                    init();
                    continue;
                }

                // Обновляем ts
                if (root.has("ts")) {
                    ts = root.get("ts").getAsInt();
                }

                // Обрабатываем массив updates
                if (root.has("updates")) {
                    JsonArray updates = root.getAsJsonArray("updates");
                    for (int i = 0; i < updates.size(); i++) {
                        JsonObject update = updates.get(i).getAsJsonObject();
                        try {
                            bot.handleRawUpdate(update);
                        } catch (Exception e) {
                            logger.error("Ошибка при обработке update", e);
                        }
                    }
                }

            } catch (Exception e) {
                logger.error("Ошибка в Long Poll цикле", e);
                try {
                    Thread.sleep(3000);
                    init();
                } catch (Exception ex) {
                    logger.error("Не удалось переинициализировать Long Poll", ex);
                }
            }
        }
    }

    /**
     * Загружает содержимое URL как строку.
     */
    private String loadJson(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(30_000);
        connection.setReadTimeout(30_000);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } finally {
            connection.disconnect();
        }
    }
}