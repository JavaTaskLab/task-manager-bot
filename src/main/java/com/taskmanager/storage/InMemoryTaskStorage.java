package com.taskmanager.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.taskmanager.model.Task;
import com.taskmanager.model.TaskStatus;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class InMemoryTaskStorage implements TaskStorage {

    private final Map<Long, Task> tasks = new HashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    private final ObjectMapper objectMapper;

    public InMemoryTaskStorage() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public Task save(Task task) {
        Task toSave = task;

        if (toSave.id() == null) {
            toSave = toSave.withId(idGenerator.getAndIncrement());
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(toSave.updatedAt())) {
            toSave = toSave.touchedAt(now);
        }

        tasks.put(toSave.id(), toSave);
        return toSave;
    }

    @Override
    public Optional<Task> findById(Long id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(tasks.get(id));
    }

    @Override
    public List<Task> findAll() {
        return List.copyOf(tasks.values());
    }

    @Override
    public List<Task> findByAssigneeId(Long userId) {
        if (userId == null) return List.of();
        return tasks.values().stream()
                .filter(t -> userId.equals(t.assigneeId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Task> findByStatus(TaskStatus status) {
        if (status == null) return List.of();
        return tasks.values().stream()
                .filter(t -> status == t.status())
                .collect(Collectors.toList());
    }

    @Override
    public List<Task> findOverdue(LocalDateTime now) {
        if (now == null) return List.of();
        return tasks.values().stream()
                .filter(t -> t.isOverdue(now))
                .collect(Collectors.toList());
    }

    @Override
    public List<Task> findByCreatorId(Long userId) {
        if (userId == null) return List.of();
        return tasks.values().stream()
                .filter(t -> userId.equals(t.creatorId()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean delete(Long id) {
        if (id == null) return false;
        return tasks.remove(id) != null;
    }

    @Override
    public void saveToFile(String filename) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(filename), tasks.values());
        } catch (IOException e) {
            throw new RuntimeException("Не удалось сохранить задачи: " + filename, e);
        }
    }

    @Override
    public void loadFromFile(String filename) {
        File file = new File(filename);
        if (!file.exists()) {
            return;
        }
        try {
            Task[] loaded = objectMapper.readValue(file, Task[].class);
            for (Task task : loaded) {
                tasks.put(task.id(), task);
                if (task.id() >= idGenerator.get()) {
                    idGenerator.set(task.id() + 1);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Не удалось загрузить задачи: " + filename, e);
        }
    }

    public void clear() {
        tasks.clear();
        idGenerator.set(1);
    }

    public int size() {
        return tasks.size();
    }
}