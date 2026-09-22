package com.taskmanager.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class TaskTest {

    @Test
    void shouldCreateTaskWithBacklogStatus() {
        Task task = new Task("Написать отчёт", 1L);
        assertEquals("Написать отчёт", task.getTitle());
        assertEquals(TaskStatus.BACKLOG, task.getStatus());
        assertEquals(1L, task.getCreatorId());
        assertNotNull(task.getCreatedAt());
    }

    @Test
    void shouldDetectOverdueTask() {
        Task task = new Task("Просроченная", 1L);
        task.setDeadline(LocalDate.now().minusDays(1));
        task.setStatus(TaskStatus.IN_PROGRESS);
        assertTrue(task.isOverdue());
    }

    @Test
    void shouldNotBeOverdueIfDone() {
        Task task = new Task("Сделанная", 1L);
        task.setDeadline(LocalDate.now().minusDays(1));
        task.setStatus(TaskStatus.DONE);
        assertFalse(task.isOverdue());
    }
}