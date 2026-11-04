package com.taskmanager.vk.dto;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class VkUpdate {
    public String type;
    public JsonObject object;

    public boolean isMessageNew() { return "message_new".equals(type); }
    public boolean isMessageAllow() { return "message_allow".equals(type); }

    public VkMessage getMessage() {
        if (object != null && object.has("message")) {
            return new Gson().fromJson(object.get("message"), VkMessage.class);
        }
        return null;
    }

    public int getAllowUserId() {
        if (object != null && object.has("user_id")) {
            return object.get("user_id").getAsInt();
        }
        return 0;
    }
}