package com.taskmanager.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Scanner;

public class Messages {
    private static JsonObject data = new JsonObject();
    private static String currentLang = "ru";

    public static void setLocale(String langCode) {
        currentLang = langCode;
        String fileName = "messages_" + langCode + ".json";
        
        try (InputStream is = Messages.class.getClassLoader().getResourceAsStream(fileName)) {
            if (is == null) {
                System.err.println("Файл перевода не найден: " + fileName);
                return;
            }
            Scanner scanner = new Scanner(is, StandardCharsets.UTF_8).useDelimiter("\\A");
            String json = scanner.hasNext() ? scanner.next() : "{}";
            data = JsonParser.parseString(json).getAsJsonObject();
        } catch (Exception e) {
            System.err.println("Ошибка чтения " + fileName + ": " + e.getMessage());
        }
    }

    public static String get(String path, Object... args) {
        String rawValue = getRaw(path);
        if (rawValue == null) return "!" + path + "!";
        return args.length > 0 ? MessageFormat.format(rawValue, args) : rawValue;
    }

    private static String getRaw(String path) {
        String[] parts = path.split("\\.");
        JsonElement current = data;
        
        for (String part : parts) {
            if (current == null || !current.isJsonObject()) return null;
            current = current.getAsJsonObject().get(part);
        }
        
        if (current == null) return null;
        
        // Если это массив (как наша справка) — склеиваем строки через \n
        if (current.isJsonArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonElement el : current.getAsJsonArray()) {
                sb.append(el.getAsString()).append("\n");
            }
            return sb.toString().stripTrailing();
        }
        
        return current.getAsString();
    }
}