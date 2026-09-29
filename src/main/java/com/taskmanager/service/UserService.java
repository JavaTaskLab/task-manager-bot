package com.taskmanager.service;

import com.taskmanager.model.Role;
import com.taskmanager.model.User;
import com.taskmanager.storage.UserStorage;

import java.util.List;

public class UserService {

    private final UserStorage userStorage;

    public UserService(UserStorage userStorage) {
        this.userStorage = userStorage;
    }


    public User createUser(String username, Role role) {
        User user = User.create(username, role);
        return userStorage.save(user);
    }

    public User createUser(String username) {
        return createUser(username, Role.EXECUTOR);
    }

    public User findById(Long id) {
        return userStorage.findById(id).orElse(null);
    }

    public User findByUsername(String username) {
        return userStorage.findByUsername(username).orElse(null);
    }

    public List<User> findAll() {
        return userStorage.findAll();
    }

    public User save(User user) {
        return userStorage.save(user);
    }

    public User getOrCreate(Long id, String username, Role role) {
        User existing = findById(id);
        if (existing != null) return existing;

        User byName = findByUsername(username);
        if (byName != null) return byName;

        Role effectiveRole = (role != null) ? role
                : (findAll().isEmpty() ? Role.MANAGER : Role.EXECUTOR);

        User created = User.create(username, effectiveRole);
        if (id != null) {
            created = created.withId(id);
        }
        return userStorage.save(created);
    }
}