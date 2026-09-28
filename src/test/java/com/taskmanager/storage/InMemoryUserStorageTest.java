package com.taskmanager.storage;

import com.taskmanager.model.Role;
import com.taskmanager.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("InMemoryUserStorage")
class InMemoryUserStorageTest {

    private InMemoryUserStorage storage;

    @BeforeEach
    void setUp() {
        storage = new InMemoryUserStorage();
    }

    @Test
    @DisplayName("save присваивает id")
    void shouldAssignId() {
        User user = storage.save(User.create("alice", Role.EXECUTOR));
        assertNotNull(user.id());
    }

    @Test
    @DisplayName("нельзя сохранить двух с одинаковым username")
    void shouldRejectDuplicateUsername() {
        storage.save(User.create("alice", Role.EXECUTOR));
        assertThrows(IllegalArgumentException.class,
            () -> storage.save(User.create("alice", Role.MANAGER)));
    }

    @Test
    @DisplayName("findById находит")
    void shouldFindById() {
        User user = storage.save(User.create("alice", Role.EXECUTOR));
        assertTrue(storage.findById(user.id()).isPresent());
    }

    @Test
    @DisplayName("findByUsername находит")
    void shouldFindByUsername() {
        storage.save(User.create("alice", Role.EXECUTOR));
        assertTrue(storage.findByUsername("alice").isPresent());
    }

    @Test
    @DisplayName("findByUsername игнорирует регистр")
    void caseInsensitive() {
        storage.save(User.create("Alice", Role.EXECUTOR));
        assertTrue(storage.findByUsername("ALICE").isPresent());
    }

    @Test
    @DisplayName("delete удаляет")
    void shouldDelete() {
        User user = storage.save(User.create("alice", Role.EXECUTOR));
        assertTrue(storage.delete(user.id()));
        assertTrue(storage.findById(user.id()).isEmpty());
        assertTrue(storage.findByUsername("alice").isEmpty());
    }
}