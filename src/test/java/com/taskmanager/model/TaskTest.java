package com.taskmanager.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Task — модель задачи")
class TaskTest {

    @Nested
    @DisplayName("create()")
    class CreateTests {

        @Test
        @DisplayName("создаёт задачу с id == null")
        void shouldCreateWithNullId() {
            Task task = Task.create("Написать отчёт", 1L);
            assertNull(task.id());
        }

        @Test
        @DisplayName("устанавливает статус BACKLOG по умолчанию")
        void shouldSetBacklogByDefault() {
            Task task = Task.create("A", 1L);
            assertEquals(TaskStatus.BACKLOG, task.status());
        }

        @Test
        @DisplayName("updatedAt == createdAt")
        void shouldSetUpdatedAtEqualsCreatedAt() {
            Task task = Task.create("A", 1L);
            assertEquals(task.createdAt(), task.updatedAt());
        }

        @Test
        @DisplayName("бросает исключение при title == null")
        void shouldThrowOnNullTitle() {
            assertThrows(IllegalArgumentException.class,
                () -> Task.create(null, 1L));
        }

        @Test
        @DisplayName("бросает исключение при пустом title")
        void shouldThrowOnBlankTitle() {
            assertThrows(IllegalArgumentException.class,
                () -> Task.create("   ", 1L));
        }

        @Test
        @DisplayName("бросает исключение при creatorId == null")
        void shouldThrowOnNullCreator() {
            assertThrows(IllegalArgumentException.class,
                () -> Task.create("A", null));
        }

        @Test
        @DisplayName("обрезает пробелы в title")
        void shouldTrimTitle() {
            Task task = Task.create("  Отчёт  ", 1L);
            assertEquals("Отчёт", task.title());
        }

        @Test
        @DisplayName("бросает исключение при слишком длинном title")
        void shouldThrowOnLongTitle() {
            String longTitle = "A".repeat(Task.MAX_TITLE_LENGTH + 1);
            assertThrows(IllegalArgumentException.class,
                () -> Task.create(longTitle, 1L));
        }
    }

    @Nested
    @DisplayName("withId()")
    class WithIdTests {

        @Test
        @DisplayName("присваивает id")
        void shouldSetId() {
            Task task = Task.create("A", 1L).withId(42L);
            assertEquals(42L, task.id());
        }

        @Test
        @DisplayName("бросает исключение при повторном withId")
        void shouldThrowOnSecondWithId() {
            Task task = Task.create("A", 1L).withId(42L);
            assertThrows(IllegalStateException.class,
                () -> task.withId(99L));
        }

        @Test
        @DisplayName("бросает исключение при null")
        void shouldThrowOnNullId() {
            Task task = Task.create("A", 1L);
            assertThrows(IllegalArgumentException.class,
                () -> task.withId(null));
        }

        @Test
        @DisplayName("бросает исключение при id <= 0")
        void shouldThrowOnNonPositiveId() {
            Task task = Task.create("A", 1L);
            assertThrows(IllegalArgumentException.class,
                () -> task.withId(0L));
            assertThrows(IllegalArgumentException.class,
                () -> task.withId(-5L));
        }
    }


    @Nested
    @DisplayName("with* методы")
    class WithTests {

        @Test
        @DisplayName("withTitle меняет название")
        void shouldChangeTitle() {
            Task task = Task.create("A", 1L);
            Task updated = task.withTitle("B");
            assertEquals("B", updated.title());
            assertEquals("A", task.title());   // оригинал не изменился
        }

        @Test
        @DisplayName("withStatus меняет статус")
        void shouldChangeStatus() {
            Task task = Task.create("A", 1L);
            Task updated = task.withStatus(TaskStatus.IN_PROGRESS);
            assertEquals(TaskStatus.IN_PROGRESS, updated.status());
        }

        @Test
        @DisplayName("withStatus(null) бросает исключение")
        void shouldThrowOnNullStatus() {
            Task task = Task.create("A", 1L);
            assertThrows(IllegalArgumentException.class,
                () -> task.withStatus(null));
        }

        @Test
        @DisplayName("withDescription меняет описание")
        void shouldChangeDescription() {
            Task task = Task.create("A", 1L);
            Task updated = task.withDescription("Описание");
            assertEquals("Описание", updated.description());
        }

        @Test
        @DisplayName("withAssigneeId назначает исполнителя")
        void shouldChangeAssignee() {
            Task task = Task.create("A", 1L);
            Task updated = task.withAssigneeId(5L);
            assertEquals(5L, updated.assigneeId());
        }

        @Test
        @DisplayName("withDeadline устанавливает дедлайн")
        void shouldChangeDeadline() {
            LocalDateTime deadline = LocalDateTime.of(2026, 10, 15, 12, 0);
            Task task = Task.create("A", 1L).withDeadline(deadline);
            assertEquals(deadline, task.deadline());
        }
    }

    @Nested
    @DisplayName("touchedAt()")
    class TouchedAtTests {

        @Test
        @DisplayName("обновляет updatedAt")
        void shouldUpdateTimestamp() {
            Task task = Task.create("A", 1L);
            LocalDateTime later = task.updatedAt().plusSeconds(10);
            Task updated = task.touchedAt(later);
            assertEquals(later, updated.updatedAt());
        }

        @Test
        @DisplayName("бросает исключение при now == null")
        void shouldThrowOnNull() {
            Task task = Task.create("A", 1L);
            assertThrows(IllegalArgumentException.class,
                () -> task.touchedAt(null));
        }

        @Test
        @DisplayName("бросает исключение при now < createdAt")
        void shouldThrowOnEarlierTime() {
            Task task = Task.create("A", 1L);
            assertThrows(IllegalArgumentException.class,
                () -> task.touchedAt(task.createdAt().minusSeconds(1)));
        }

        @Test
        @DisplayName("бросает исключение при now == updatedAt")
        void shouldThrowOnSameTime() {
            Task task = Task.create("A", 1L);
            assertThrows(IllegalArgumentException.class,
                () -> task.touchedAt(task.updatedAt()));
        }
    }

    @Nested
    @DisplayName("addSubtask / removeSubtask")
    class SubtaskTests {

        @Test
        @DisplayName("добавляет подзадачу")
        void shouldAddSubtask() {
            Task task = Task.create("A", 1L).withId(1L);
            Task updated = task.addSubtask(2L);
            assertTrue(updated.subtaskIds().contains(2L));
        }

        @Test
        @DisplayName("идемпотентен")
        void shouldBeIdempotent() {
            Task task = Task.create("A", 1L).withId(1L);
            Task updated = task.addSubtask(2L).addSubtask(2L);
            assertEquals(1, updated.subtaskIds().size());
        }

        @Test
        @DisplayName("бросает при self-reference")
        void shouldThrowOnSelfReference() {
            Task task = Task.create("A", 1L).withId(1L);
            assertThrows(IllegalArgumentException.class,
                () -> task.addSubtask(1L));
        }

        @Test
        @DisplayName("удаляет подзадачу")
        void shouldRemoveSubtask() {
            Task task = Task.create("A", 1L).withId(1L)
                    .addSubtask(2L).removeSubtask(2L);
            assertFalse(task.subtaskIds().contains(2L));
        }
    }

    @Nested
    @DisplayName("addBlockedBy / addBlocks")
    class BlockingTests {

        @Test
        @DisplayName("addBlockedBy добавляет блокирующую")
        void shouldAddBlockedBy() {
            Task task = Task.create("A", 1L).withId(1L);
            Task updated = task.addBlockedBy(2L);
            assertTrue(updated.blockedByIds().contains(2L));
        }

        @Test
        @DisplayName("addBlocks добавляет блокируемую")
        void shouldAddBlocks() {
            Task task = Task.create("A", 1L).withId(1L);
            Task updated = task.addBlocks(2L);
            assertTrue(updated.blocksIds().contains(2L));
        }

        @Test
        @DisplayName("isBlocked() == true после addBlockedBy")
        void shouldBeBlocked() {
            Task task = Task.create("A", 1L).withId(1L).addBlockedBy(2L);
            assertTrue(task.isBlocked());
        }
    }

    @Nested
    @DisplayName("isOverdue()")
    class OverdueTests {

        @Test
        @DisplayName("false без дедлайна")
        void shouldBeFalseWithoutDeadline() {
            Task task = Task.create("A", 1L);
            assertFalse(task.isOverdue(LocalDateTime.now()));
        }

        @Test
        @DisplayName("true при просрочке")
        void shouldBeTrueWhenPast() {
            Task task = Task.create("A", 1L)
                    .withDeadline(LocalDateTime.now().minusDays(1));
            assertTrue(task.isOverdue(LocalDateTime.now()));
        }

        @Test
        @DisplayName("false если DONE")
        void shouldBeFalseWhenDone() {
            Task task = Task.create("A", 1L)
                    .withDeadline(LocalDateTime.now().minusDays(1))
                    .withStatus(TaskStatus.DONE);
            assertFalse(task.isOverdue(LocalDateTime.now()));
        }
    }

    @Nested
    @DisplayName("equals / hashCode")
    class EqualsTests {

        @Test
        @DisplayName("одинаковые задачи равны")
        void shouldBeEqual() {
            Task a = Task.create("A", 1L).withId(1L);
            Task b = a;   // та же ссылка
            assertEquals(a, b);
        }
    }
}