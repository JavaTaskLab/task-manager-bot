package com.taskmanager;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class GroupLongPollHandler {

    private static final Logger logger = LoggerFactory.getLogger(GroupLongPollHandler.class);

    private final TaskManagerBot bot;
    private final Gson gson = new Gson();

    private String server;
    private String key;
    private int ts;

    public GroupLongPollHandler(TaskManagerBot bot) {
        this.bot = bot;
    }

    /**
     * Получает адрес Long Poll сервера и ключ через groups.getLongPollServer.
     */
    public void init() throws Exception {
        String url = "https://api.vk.com/method/groups.getLongPollServer"
                + "?group_id=" + bot.getGroupId()
                + "&access_token=" + bot.getAccessToken()
                + "&v=5.131";

        String jsonResponse = loadJson(url);
        JsonObject root = gson.fromJson(jsonResponse, JsonObject.class);

        if (root.has("error")) {
            throw new RuntimeException("VK API error: " + root.getAsJsonObject("error"));
        }

        JsonObject response = root.getAsJsonObject("response");
        this.server = response.get("server").getAsString();
        this.key = response.get("key").getAsString();
        this.ts = response.get("ts").getAsInt();

        logger.info("Long Poll инициализирован: server={}, ts={}", server, ts);
    }

    /**
     * Бесконечный цикл опроса Long Poll.
     */
    public void run() {
        while (true) {
            try {
                String url = server + "?act=a_check&key=" + key
                        + "&ts=" + ts + "&wait=25";

                String jsonResponse = loadJson(url);
                JsonObject root = gson.fromJson(jsonResponse, JsonObject.class);

                if (root.has("failed")) {
                    logger.warn("Long Poll вернул ошибку, переинициализация...");
                    init();
                    continue;
                }

                if (root.has("ts")) {
                    ts = root.get("ts").getAsInt();
                }

                if (root.has("updates")) {
                    JsonArray updates = root.getAsJsonArray("updates");
                    for (int i = 0; i < updates.size(); i++) {
                        JsonObject update = updates.get(i).getAsJsonObject();
                        bot.handleRawUpdate(update);
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