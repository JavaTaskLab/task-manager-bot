package com.taskmanager.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TaskStatus — переходы и свойства")
class TaskStatusTest {

    @Test
    @DisplayName("isFinal() только для DONE")
    void isFinalOnlyDone() {
        assertTrue(TaskStatus.DONE.isFinal());
        assertFalse(TaskStatus.BACKLOG.isFinal());
        assertFalse(TaskStatus.IN_PROGRESS.isFinal());
    }

    @Test
    @DisplayName("isActive() только для IN_PROGRESS")
    void isActiveOnlyInProgress() {
        assertTrue(TaskStatus.IN_PROGRESS.isActive());
        assertFalse(TaskStatus.TO_DO.isActive());
        assertFalse(TaskStatus.DONE.isActive());
    }

    @Test
    @DisplayName("isOpen() == false только для DONE")
    void isOpenExceptDone() {
        assertFalse(TaskStatus.DONE.isOpen());
        assertTrue(TaskStatus.BACKLOG.isOpen());
        assertTrue(TaskStatus.IN_PROGRESS.isOpen());
    }

    @Test
    @DisplayName("canStart() true для BACKLOG и TO_DO")
    void canStartForBacklogAndTodo() {
        assertTrue(TaskStatus.BACKLOG.canStart());
        assertTrue(TaskStatus.TO_DO.canStart());
        assertFalse(TaskStatus.IN_PROGRESS.canStart());
        assertFalse(TaskStatus.DONE.canStart());
    }

    // Переходы

    @Test
    @DisplayName("BACKLOG → TO_DO разрешён")
    void backlogToTodo() {
        assertTrue(TaskStatus.BACKLOG.canTransitionTo(TaskStatus.TO_DO));
    }

    @Test
    @DisplayName("BACKLOG → DONE запрещён")
    void backlogToDoneNotAllowed() {
        assertFalse(TaskStatus.BACKLOG.canTransitionTo(TaskStatus.DONE));
    }

    @Test
    @DisplayName("TO_DO → IN_PROGRESS разрешён")
    void todoToInProgress() {
        assertTrue(TaskStatus.TO_DO.canTransitionTo(TaskStatus.IN_PROGRESS));
    }

    @Test
    @DisplayName("IN_PROGRESS → REVIEW разрешён")
    void inProgressToReview() {
        assertTrue(TaskStatus.IN_PROGRESS.canTransitionTo(TaskStatus.REVIEW));
    }

    @Test
    @DisplayName("IN_PROGRESS → BLOCKED разрешён")
    void inProgressToBlocked() {
        assertTrue(TaskStatus.IN_PROGRESS.canTransitionTo(TaskStatus.BLOCKED));
    }

    @Test
    @DisplayName("REVIEW → DONE разрешён")
    void reviewToDone() {
        assertTrue(TaskStatus.REVIEW.canTransitionTo(TaskStatus.DONE));
    }

    @Test
    @DisplayName("DONE → любой запрещён")
    void doneIsFinal() {
        for (TaskStatus s : TaskStatus.values()) {
            assertFalse(TaskStatus.DONE.canTransitionTo(s),
                "DONE → " + s + " должен быть запрещён");
        }
    }

    @Test
    @DisplayName("нельзя перейти в тот же статус")
    void cannotTransitionToSame() {
        for (TaskStatus s : TaskStatus.values()) {
            assertFalse(s.canTransitionTo(s));
        }
    }

    @Test
    @DisplayName("null запрещён")
    void nullNotAllowed() {
        assertFalse(TaskStatus.BACKLOG.canTransitionTo(null));
    }

    @ParameterizedTest
    @EnumSource(TaskStatus.class)
    @DisplayName("fromString работает для всех значений")
    void fromStringWorks(TaskStatus status) {
        assertEquals(status, TaskStatus.fromString(status.name()));
    }

    @Test
    @DisplayName("fromString игнорирует регистр")
    void fromStringCaseInsensitive() {
        assertEquals(TaskStatus.IN_PROGRESS, TaskStatus.fromString("in_progress"));
        assertEquals(TaskStatus.IN_PROGRESS, TaskStatus.fromString("IN_PROGRESS"));
    }

    @Test
    @DisplayName("fromString(null) бросает")
    void fromStringNullThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> TaskStatus.fromString(null));
    }

    @Test
    @DisplayName("fromString(unknown) бросает")
    void fromStringUnknownThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> TaskStatus.fromString("unknown"));
    }
}