package com.taskmanager.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanager.model.User;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Реализация хранилища пользователей в памяти с сохранением в JSON.
 */
public class InMemoryUserStorage implements UserStorage {

    private final Map<Long, User> usersById = new HashMap<>();
    private final Map<String, Long> usernameIndex = new HashMap<>();   // username → id
    private final AtomicLong idGenerator = new AtomicLong(1);
    private final ObjectMapper objectMapper;

    public InMemoryUserStorage() {
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public User save(User user) {
        User toSave = user;

        if (toSave.id() == null) {
            toSave = toSave.withId(idGenerator.getAndIncrement());
        }

        Long existingId = usernameIndex.get(toSave.username().toLowerCase());
        if (existingId != null && !existingId.equals(toSave.id())) {
            throw new IllegalArgumentException(
                "Пользователь с именем '" + toSave.username() + "' уже существует");
        }

        usersById.put(toSave.id(), toSave);
        usernameIndex.put(toSave.username().toLowerCase(), toSave.id());

        return toSave;
    }

    @Override
    public Optional<User> findById(Long id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(usersById.get(id));
    }

    @Override
    public Optional<User> findByUsername(String username) {
        if (username == null || username.isBlank()) return Optional.empty();
        Long id = usernameIndex.get(username.toLowerCase());
        if (id == null) return Optional.empty();
        return Optional.ofNullable(usersById.get(id));
    }

    @Override
    public List<User> findAll() {
        return List.copyOf(usersById.values());
    }

    @Override
    public boolean delete(Long id) {
        if (id == null) return false;
        User removed = usersById.remove(id);
        if (removed != null) {
            usernameIndex.remove(removed.username().toLowerCase());
            return true;
        }
        return false;
    }

    @Override
    public void saveToFile(String filename) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(filename), usersById.values());
        } catch (IOException e) {
            throw new RuntimeException("Не удалось сохранить пользователей: " + filename, e);
        }
    }

    @Override
    public void loadFromFile(String filename) {
        File file = new File(filename);
        if (!file.exists()) {
            return;
        }
        try {
            User[] loaded = objectMapper.readValue(file, User[].class);
            for (User user : loaded) {
                usersById.put(user.id(), user);
                usernameIndex.put(user.username().toLowerCase(), user.id());
                if (user.id() >= idGenerator.get()) {
                    idGenerator.set(user.id() + 1);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Не удалось загрузить пользователей: " + filename, e);
        }
    }

    public void clear() {
        usersById.clear();
        usernameIndex.clear();
        idGenerator.set(1);
    }

    public int size() {
        return usersById.size();
    }
}