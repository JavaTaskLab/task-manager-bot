package com.taskmanager.vk.dto;

import com.google.gson.JsonArray;

public class LongPollResponse {
    public String server;
    public String key;
    public int ts;
    public JsonArray updates;
    public int failed;
}