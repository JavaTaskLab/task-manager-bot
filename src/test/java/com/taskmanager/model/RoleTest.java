package com.taskmanager.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Role — права доступа")
class RoleTest {

    @Test
    @DisplayName("MANAGER может создавать задачи")
    void managerCanCreateTasks() {
        assertTrue(Role.MANAGER.canCreateTasks());
    }

    @Test
    @DisplayName("EXECUTOR может создавать задачи")
    void executorCanCreateTasks() {
        assertTrue(Role.EXECUTOR.canCreateTasks());
    }

    @Test
    @DisplayName("OBSERVER не может создавать задачи")
    void observerCannotCreateTasks() {
        assertFalse(Role.OBSERVER.canCreateTasks());
    }

    @Test
    @DisplayName("только MANAGER может назначать задачи")
    void onlyManagerCanAssign() {
        assertTrue(Role.MANAGER.canAssignTasks());
        assertFalse(Role.EXECUTOR.canAssignTasks());
        assertFalse(Role.OBSERVER.canAssignTasks());
    }

    @Test
    @DisplayName("MANAGER и EXECUTOR могут менять статус")
    void managerAndExecutorCanChangeStatus() {
        assertTrue(Role.MANAGER.canChangeStatus());
        assertTrue(Role.EXECUTOR.canChangeStatus());
        assertFalse(Role.OBSERVER.canChangeStatus());
    }

    @Test
    @DisplayName("только MANAGER может ставить дедлайн")
    void onlyManagerCanSetDeadline() {
        assertTrue(Role.MANAGER.canSetDeadline());
        assertFalse(Role.EXECUTOR.canSetDeadline());
    }

    @Test
    @DisplayName("isReadOnly() только для OBSERVER")
    void observerIsReadOnly() {
        assertTrue(Role.OBSERVER.isReadOnly());
        assertFalse(Role.MANAGER.isReadOnly());
        assertFalse(Role.EXECUTOR.isReadOnly());
    }

    @Test
    @DisplayName("fromString работает")
    void fromStringWorks() {
        assertEquals(Role.MANAGER, Role.fromString("manager"));
        assertEquals(Role.EXECUTOR, Role.fromString("EXECUTOR"));
    }

    @Test
    @DisplayName("fromString(null) бросает")
    void fromStringNullThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> Role.fromString(null));
    }

    @Test
    @DisplayName("getDisplayName() работает")
    void displayNameWorks() {
        assertEquals("Менеджер", Role.MANAGER.getDisplayName());
        assertEquals("Исполнитель", Role.EXECUTOR.getDisplayName());
        assertEquals("Наблюдатель", Role.OBSERVER.getDisplayName());
    }
}