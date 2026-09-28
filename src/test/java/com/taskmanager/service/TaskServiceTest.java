package com.taskmanager.service;

import com.taskmanager.exception.PermissionDeniedException;
import com.taskmanager.exception.TaskNotFoundException;
import com.taskmanager.model.*;
import com.taskmanager.storage.InMemoryTaskStorage;
import com.taskmanager.storage.InMemoryUserStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TaskService — бизнес-логика")
class TaskServiceTest {

    private TaskService taskService;
    private UserService userService;
    private InMemoryTaskStorage taskStorage;
    private InMemoryUserStorage userStorage;
    private Long managerId;
    private Long executorId;

    @BeforeEach
    void setUp() {
        taskStorage = new InMemoryTaskStorage();
        userStorage = new InMemoryUserStorage();
        userService = new UserService(userStorage);
        taskService = new TaskService(taskStorage, userStorage);

        managerId = userService.createUser("Admin", Role.MANAGER).id();
        executorId = userService.createUser("Petrov", Role.EXECUTOR).id();
    }

    @Nested
    @DisplayName("createTask()")
    class CreateTests {

        @Test
        @DisplayName("создаёт задачу")
        void shouldCreate() {
            Task task = taskService.createTask("Отчёт", managerId);
            assertNotNull(task.id());
            assertEquals("Отчёт", task.title());
            assertEquals(TaskStatus.BACKLOG, task.status());
        }

        @Test
        @DisplayName("бросает при несуществующем создателе")
        void shouldThrowOnUnknownCreator() {
            assertThrows(IllegalArgumentException.class,
                () -> taskService.createTask("A", 999L));
        }
    }

    @Nested
    @DisplayName("assignTask()")
    class AssignTests {

        @Test
        @DisplayName("менеджер назначает задачу")
        void managerCanAssign() {
            Task task = taskService.createTask("A", managerId);
            Task assigned = taskService.assignTask(task.id(), executorId, managerId);
            assertEquals(executorId, assigned.assigneeId());
        }

        @Test
        @DisplayName("исполнитель НЕ может назначать")
        void executorCannotAssign() {
            Task task = taskService.createTask("A", managerId);
            assertThrows(PermissionDeniedException.class,
                () -> taskService.assignTask(task.id(), executorId, executorId));
        }

        @Test
        @DisplayName("назначение на null снимает исполнителя")
        void canUnassign() {
            Task task = taskService.createTask("A", managerId);
            taskService.assignTask(task.id(), executorId, managerId);
            Task unassigned = taskService.assignTask(task.id(), null, managerId);
            assertNull(unassigned.assigneeId());
        }
    }

    @Nested
    @DisplayName("changeStatus()")
    class StatusTests {

        @Test
        @DisplayName("меняет статус по цепочке")
        void shouldChangeStatus() {
            Task task = taskService.createTask("A", managerId);
            task = taskService.assignTask(task.id(), executorId, managerId);
            task = taskService.changeStatus(task.id(), TaskStatus.TO_DO, managerId);
            task = taskService.changeStatus(task.id(), TaskStatus.IN_PROGRESS, executorId);
            assertEquals(TaskStatus.IN_PROGRESS, task.status());
        }

        @Test
        @DisplayName("бросает при недопустимом переходе")
        void shouldThrowOnInvalidTransition() {
            Task task = taskService.createTask("A", managerId);
            assertThrows(IllegalArgumentException.class,
                () -> taskService.changeStatus(task.id(), TaskStatus.DONE, managerId));
        }

        @Test
        @DisplayName("OBSERVER не может менять статус")
        void observerCannotChangeStatus() {
            Long observerId = userService.createUser("Observer", Role.OBSERVER).id();
            Task task = taskService.createTask("A", managerId);
            assertThrows(PermissionDeniedException.class,
                () -> taskService.changeStatus(task.id(), TaskStatus.TO_DO, observerId));
        }
    }

    @Nested
    @DisplayName("Правило A: подзадача не раньше родителя")
    class RuleATests {

        @Test
        @DisplayName("нельзя начать подзадачу, если родитель в BACKLOG")
        void cannotStartSubtaskBeforeParent() {
            Task parent = taskService.createTask("Родитель", managerId);
            Task child = taskService.createTask("Ребёнок", managerId);
            taskService.addSubtask(parent.id(), child.id(), managerId);

            taskService.changeStatus(child.id(), TaskStatus.TO_DO, managerId);
            taskService.changeStatus(parent.id(), TaskStatus.TO_DO, managerId);

            assertThrows(IllegalStateException.class,
                () -> taskService.changeStatus(child.id(), TaskStatus.IN_PROGRESS, managerId));
        }

        @Test
        @DisplayName("можно начать подзадачу, если родитель начат")
        void canStartWhenParentStarted() {
            Task parent = taskService.createTask("Родитель", managerId);
            Task child = taskService.createTask("Ребёнок", managerId);
            taskService.addSubtask(parent.id(), child.id(), managerId);

            taskService.changeStatus(parent.id(), TaskStatus.TO_DO, managerId);
            taskService.changeStatus(parent.id(), TaskStatus.IN_PROGRESS, managerId);

            taskService.changeStatus(child.id(), TaskStatus.TO_DO, managerId);
            Task started = taskService.changeStatus(child.id(), TaskStatus.IN_PROGRESS, managerId);

            assertEquals(TaskStatus.IN_PROGRESS, started.status());
        }
    }

    @Nested
    @DisplayName("Правило B: родитель не раньше подзадач")
    class RuleBTests {

        @Test
        @DisplayName("нельзя завершить родителя с незавершёнными подзадачами")
        void cannotFinishParentWithOpenSubtasks() {
            Task parent = taskService.createTask("Родитель", managerId);
            Task child = taskService.createTask("Ребёнок", managerId);
            taskService.addSubtask(parent.id(), child.id(), managerId);

            taskService.changeStatus(parent.id(), TaskStatus.TO_DO, managerId);
            taskService.changeStatus(parent.id(), TaskStatus.IN_PROGRESS, managerId);
            taskService.changeStatus(parent.id(), TaskStatus.REVIEW, managerId);

            assertThrows(IllegalStateException.class,
                () -> taskService.changeStatus(parent.id(), TaskStatus.DONE, managerId));
        }

        @Test
        @DisplayName("можно завершить родителя, если подзадачи DONE")
        void canFinishWhenSubtasksDone() {
            Task parent = taskService.createTask("Родитель", managerId);
            Task child = taskService.createTask("Ребёнок", managerId);
            taskService.addSubtask(parent.id(), child.id(), managerId);

            taskService.changeStatus(parent.id(), TaskStatus.TO_DO, managerId);
            taskService.changeStatus(parent.id(), TaskStatus.IN_PROGRESS, managerId);

            taskService.changeStatus(child.id(), TaskStatus.TO_DO, managerId);
            taskService.changeStatus(child.id(), TaskStatus.IN_PROGRESS, managerId);
            taskService.changeStatus(child.id(), TaskStatus.REVIEW, managerId);
            taskService.changeStatus(child.id(), TaskStatus.DONE, managerId);

            taskService.changeStatus(parent.id(), TaskStatus.REVIEW, managerId);
            Task done = taskService.changeStatus(parent.id(), TaskStatus.DONE, managerId);

            assertEquals(TaskStatus.DONE, done.status());
        }
    }

    @Nested
    @DisplayName("addSubtask()")
    class SubtaskTests {

        @Test
        @DisplayName("добавляет подзадачу")
        void shouldAdd() {
            Task p = taskService.createTask("P", managerId);
            Task c = taskService.createTask("C", managerId);
            taskService.addSubtask(p.id(), c.id(), managerId);
            assertTrue(taskService.getTask(p.id()).subtaskIds().contains(c.id()));
        }

        @Test
        @DisplayName("нельзя self-reference")
        void shouldRejectSelf() {
            Task p = taskService.createTask("P", managerId);
            assertThrows(IllegalArgumentException.class,
                () -> taskService.addSubtask(p.id(), p.id(), managerId));
        }

        @Test
        @DisplayName("нельзя двум родителям")
        void shouldRejectTwoParents() {
            Task p1 = taskService.createTask("P1", managerId);
            Task p2 = taskService.createTask("P2", managerId);
            Task c  = taskService.createTask("C", managerId);

            taskService.addSubtask(p1.id(), c.id(), managerId);
            assertThrows(IllegalArgumentException.class,
                () -> taskService.addSubtask(p2.id(), c.id(), managerId));
        }

        @Test
        @DisplayName("нельзя создать цикл")
        void shouldRejectCycle() {
            Task a = taskService.createTask("A", managerId);
            Task b = taskService.createTask("B", managerId);
            Task c = taskService.createTask("C", managerId);

            taskService.addSubtask(a.id(), b.id(), managerId);
            taskService.addSubtask(b.id(), c.id(), managerId);

            assertThrows(IllegalArgumentException.class,
                () -> taskService.addSubtask(c.id(), a.id(), managerId));
        }
    }

    @Nested
    @DisplayName("getMyTasks()")
    class MyTasksTests {

        @Test
        @DisplayName("возвращает задачи исполнителя")
        void shouldReturnAssigned() {
            Task t1 = taskService.createTask("A", managerId);
            Task t2 = taskService.createTask("B", managerId);
            taskService.assignTask(t1.id(), executorId, managerId);
            taskService.assignTask(t2.id(), executorId, managerId);

            assertEquals(2, taskService.getMyTasks(executorId).size());
        }
    }
}