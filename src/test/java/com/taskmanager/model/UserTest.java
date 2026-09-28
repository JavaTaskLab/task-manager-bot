package com.taskmanager.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("User — модель пользователя")
class UserTest {

    @Nested
    @DisplayName("create()")
    class CreateTests {

        @Test
        @DisplayName("создаёт с id == null")
        void shouldCreateWithNullId() {
            User user = User.create("alice");
            assertNull(user.id());
        }

        @Test
        @DisplayName("роль по умолчанию — EXECUTOR")
        void shouldDefaultToExecutor() {
            User user = User.create("alice");
            assertEquals(Role.EXECUTOR, user.role());
        }

        @Test
        @DisplayName("role == null → EXECUTOR")
        void nullRoleBecomesExecutor() {
            User user = User.create("alice", null);
            assertEquals(Role.EXECUTOR, user.role());
        }

        @Test
        @DisplayName("бросает при null username")
        void shouldThrowOnNullUsername() {
            assertThrows(IllegalArgumentException.class,
                () -> User.create(null));
        }

        @Test
        @DisplayName("бросает при пустом username")
        void shouldThrowOnBlankUsername() {
            assertThrows(IllegalArgumentException.class,
                () -> User.create("   "));
        }

        @Test
        @DisplayName("обрезает пробелы")
        void shouldTrimUsername() {
            User user = User.create("  alice  ");
            assertEquals("alice", user.username());
        }

        @Test
        @DisplayName("бросает при слишком длинном username")
        void shouldThrowOnLongUsername() {
            String longName = "A".repeat(51);
            assertThrows(IllegalArgumentException.class,
                () -> User.create(longName));
        }
    }

    @Nested
    @DisplayName("withId()")
    class WithIdTests {

        @Test
        @DisplayName("присваивает id")
        void shouldSetId() {
            User user = User.create("alice").withId(42L);
            assertEquals(42L, user.id());
        }

        @Test
        @DisplayName("бросает при повторном withId")
        void shouldThrowOnSecondWithId() {
            User user = User.create("alice").withId(42L);
            assertThrows(IllegalStateException.class,
                () -> user.withId(99L));
        }

        @Test
        @DisplayName("бросает при id <= 0")
        void shouldThrowOnNonPositiveId() {
            User user = User.create("alice");
            assertThrows(IllegalArgumentException.class,
                () -> user.withId(0L));
        }
    }

    @Nested
    @DisplayName("with*")
    class WithTests {

        @Test
        @DisplayName("withUsername меняет имя")
        void shouldChangeUsername() {
            User user = User.create("alice").withUsername("bob");
            assertEquals("bob", user.username());
        }

        @Test
        @DisplayName("withRole меняет роль")
        void shouldChangeRole() {
            User user = User.create("alice").withRole(Role.MANAGER);
            assertEquals(Role.MANAGER, user.role());
        }

        @Test
        @DisplayName("withRole(null) бросает")
        void shouldThrowOnNullRole() {
            User user = User.create("alice");
            assertThrows(IllegalArgumentException.class,
                () -> user.withRole(null));
        }
    }

    @Nested
    @DisplayName("is* методы")
    class RoleTests {

        @Test
        @DisplayName("isManager()")
        void shouldDetectManager() {
            assertTrue(User.create("a", Role.MANAGER).isManager());
            assertFalse(User.create("a", Role.EXECUTOR).isManager());
        }

        @Test
        @DisplayName("isExecutor()")
        void shouldDetectExecutor() {
            assertTrue(User.create("a", Role.EXECUTOR).isExecutor());
            assertFalse(User.create("a", Role.MANAGER).isExecutor());
        }

        @Test
        @DisplayName("isObserver()")
        void shouldDetectObserver() {
            assertTrue(User.create("a", Role.OBSERVER).isObserver());
            assertFalse(User.create("a", Role.MANAGER).isObserver());
        }
    }
}