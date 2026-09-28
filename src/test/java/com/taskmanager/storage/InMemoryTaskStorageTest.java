package com.taskmanager.storage;

import com.taskmanager.model.Task;
import com.taskmanager.model.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("InMemoryTaskStorage")
class InMemoryTaskStorageTest {

    private InMemoryTaskStorage storage;

    @BeforeEach
    void setUp() {
        storage = new InMemoryTaskStorage();
    }

    @Test
    @DisplayName("save присваивает id новой задаче")
    void shouldAssignIdOnSave() {
        Task task = Task.create("A", 1L);
        Task saved = storage.save(task);
        assertNotNull(saved.id());
    }

    @Test
    @DisplayName("save обновляет updatedAt")
    void shouldUpdateTimestampOnSave() throws InterruptedException {
        Task task = storage.save(Task.create("A", 1L));
        LocalDateTime firstUpdated = task.updatedAt();

        Thread.sleep(5);   
        Task updated = storage.save(task.withStatus(TaskStatus.TO_DO));

        assertTrue(updated.updatedAt().isAfter(firstUpdated));
    }

    @Test
    @DisplayName("findById находит задачу")
    void shouldFindById() {
        Task saved = storage.save(Task.create("A", 1L));
        Optional<Task> found = storage.findById(saved.id());
        assertTrue(found.isPresent());
        assertEquals(saved.id(), found.get().id());
    }

    @Test
    @DisplayName("findById(null) возвращает пустой Optional")
    void findByIdNullReturnsEmpty() {
        assertTrue(storage.findById(null).isEmpty());
    }

    @Test
    @DisplayName("findById(unknown) возвращает пустой Optional")
    void findByIdUnknownReturnsEmpty() {
        assertTrue(storage.findById(999L).isEmpty());
    }

    @Test
    @DisplayName("findAll возвращает все задачи")
    void shouldFindAll() {
        storage.save(Task.create("A", 1L));
        storage.save(Task.create("B", 1L));
        assertEquals(2, storage.findAll().size());
    }

    @Test
    @DisplayName("findByAssigneeId фильтрует")
    void shouldFindByAssignee() {
        Task t1 = storage.save(Task.create("A", 1L));
        Task t2 = storage.save(Task.create("B", 1L));
        storage.save(t1.withAssigneeId(5L));
        storage.save(t2.withAssigneeId(6L));

        List<Task> tasks = storage.findByAssigneeId(5L);
        assertEquals(1, tasks.size());
    }

    @Test
    @DisplayName("findOverdue находит просроченные")
    void shouldFindOverdue() {
        Task t = Task.create("A", 1L)
                .withDeadline(LocalDateTime.now().minusDays(1));
        storage.save(t);

        List<Task> overdue = storage.findOverdue(LocalDateTime.now());
        assertEquals(1, overdue.size());
    }

    @Test
    @DisplayName("delete удаляет задачу")
    void shouldDelete() {
        Task saved = storage.save(Task.create("A", 1L));
        assertTrue(storage.delete(saved.id()));
        assertTrue(storage.findById(saved.id()).isEmpty());
    }

    @Test
    @DisplayName("delete(unknown) возвращает false")
    void deleteUnknownReturnsFalse() {
        assertFalse(storage.delete(999L));
    }
}