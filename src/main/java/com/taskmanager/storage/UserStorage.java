package com.taskmanager.storage;

import com.taskmanager.model.User;

import java.util.List;
import java.util.Optional;

/**
 * Хранилище пользователей.
 * <p>
 * Не зависит от логики диалога (ТЗ п.1).
 */
public interface UserStorage {

    /**
     * Сохраняет пользователя. Если id == null — присваивает новый.
     */
    User save(User user);

    Optional<User> findById(Long id);

    Optional<User> findByUsername(String username);

    List<User> findAll();

    boolean delete(Long id);

    void saveToFile(String filename);

    void loadFromFile(String filename);
}