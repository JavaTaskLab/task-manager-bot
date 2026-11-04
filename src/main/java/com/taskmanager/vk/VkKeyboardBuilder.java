package com.taskmanager.vk;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.ArrayList;
import java.util.List;

public class VkKeyboardBuilder {
    private boolean oneTime = false;
    private final List<List<Button>> buttons = new ArrayList<>();

    public static VkKeyboardBuilder create() { return new VkKeyboardBuilder(); }

    public VkKeyboardBuilder oneTime(boolean oneTime) {
        this.oneTime = oneTime;
        return this;
    }

    public VkKeyboardBuilder addRow() {
        buttons.add(new ArrayList<>());
        return this;
    }

    public VkKeyboardBuilder addTextButton(String label, String color) {
        if (buttons.isEmpty()) addRow();
        buttons.get(buttons.size() - 1).add(new Button(new Action("text", label), color));
        return this;
    }

    public String build() {
        return new GsonBuilder().create().toJson(new Keyboard(oneTime, buttons));
    }

    private static class Keyboard {
        boolean one_time;
        List<List<Button>> buttons;
        Keyboard(boolean one_time, List<List<Button>> buttons) {
            this.one_time = one_time; this.buttons = buttons;
        }
    }
    private static class Button {
        Action action; String color;
        Button(Action action, String color) { this.action = action; this.color = color; }
    }
    private static class Action {
        String type; String label;
        Action(String type, String label) { this.type = type; this.label = label; }
    }
}