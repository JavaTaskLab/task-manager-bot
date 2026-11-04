package com.taskmanager;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.taskmanager.vk.dto.LongPollResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
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


    //Получает адрес Long Poll сервера и ключ через groups.getLongPollServer.

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

        // Используем DTO для автоматического парсинга
        LongPollResponse resp = gson.fromJson(root.get("response"), LongPollResponse.class);
        
        this.server = resp.server;
        this.key = resp.key;
        this.ts = resp.ts;

        logger.info("Long Poll инициализирован: server={}, ts={}", server, ts);
    }


    // Бесконечный цикл опроса Long Poll.

    public void run() {
        while (true) {
            try {
                String url = server + "?act=a_check&key=" + key + "&ts=" + ts + "&wait=25";
                String jsonResponse = loadJson(url);
                
                // Автоматический парсинг ответа
                LongPollResponse response = gson.fromJson(jsonResponse, LongPollResponse.class);

                if (response.failed > 0) {
                    logger.warn("Long Poll вернул ошибку {}, переинициализация...", response.failed);
                    init();
                    continue;
                }

                this.ts = response.ts;

                if (response.updates != null) {
                    for (int i = 0; i < response.updates.size(); i++) {
                        JsonObject rawUpdate = response.updates.get(i).getAsJsonObject();
                        bot.handleRawUpdate(rawUpdate);
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
     * ИСПРАВЛЕНИЕ КОДИРОВКИ: явно указываем UTF-8 при чтении.
     */
    private String loadJson(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(30_000);
        connection.setReadTimeout(30_000);

        // Указываем UTF-8, чтобы эмодзи не превращались в кракозябры
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
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