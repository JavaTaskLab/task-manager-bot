package com.taskmanager.storage;

import com.taskmanager.model.Task;
import com.taskmanager.model.TaskStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Хранилище задач.
 * <p>
 * Не зависит от логики диалога (ТЗ п.1).
 */
public interface TaskStorage {

    /**
     * Сохраняет задачу. Если id == null — присваивает новый.
     * Обновляет {@code updatedAt}.
     *
     * @return сохранённая задача (с присвоенным id)
     */
    Task save(Task task);

    Optional<Task> findById(Long id);

    List<Task> findAll();

    List<Task> findByAssigneeId(Long userId);

    List<Task> findByStatus(TaskStatus status);

    List<Task> findOverdue(LocalDateTime now);

    List<Task> findByCreatorId(Long userId);

    boolean delete(Long id);

    void saveToFile(String filename);

    void loadFromFile(String filename);
}