package com.taskmanager.service;

import com.taskmanager.model.Role;
import com.taskmanager.model.User;
import com.taskmanager.storage.InMemoryUserStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserService — бизнес-логика")
class UserServiceTest {

    private UserService userService;
    private InMemoryUserStorage userStorage;

    @BeforeEach
    void setUp() {
        userStorage = new InMemoryUserStorage();
        userService = new UserService(userStorage);
    }

    @Nested
    @DisplayName("createUser()")
    class CreateTests {

        @Test
        @DisplayName("создаёт пользователя")
        void shouldCreate() {
            User user = userService.createUser("alice", Role.MANAGER);
            assertNotNull(user.id());
            assertEquals("alice", user.username());
        }

        @Test
        @DisplayName("роль по умолчанию — EXECUTOR")
        void defaultRole() {
            User user = userService.createUser("bob");
            assertEquals(Role.EXECUTOR, user.role());
        }

        @Test
        @DisplayName("нельзя двух с одинаковым username")
        void shouldRejectDuplicate() {
            userService.createUser("alice");
            assertThrows(IllegalArgumentException.class,
                () -> userService.createUser("alice"));
        }
    }

    @Nested
    @DisplayName("getOrCreate()")
    class GetOrCreateTests {

        @Test
        @DisplayName("создаёт нового")
        void shouldCreateNew() {
            User user = userService.getOrCreate(100L, "alice", Role.EXECUTOR);
            assertEquals(100L, user.id());
        }

        @Test
        @DisplayName("возвращает существующего по id")
        void shouldReturnExisting() {
            User created = userService.createUser("alice", Role.MANAGER);
            User found = userService.getOrCreate(created.id(), "alice", Role.EXECUTOR);
            assertEquals(created.id(), found.id());
            assertEquals(Role.MANAGER, found.role());
        }

        @Test
        @DisplayName("первый пользователь — MANAGER")
        void firstUserIsManager() {
            User first = userService.getOrCreate(1L, "admin", null);
            assertEquals(Role.MANAGER, first.role());
        }

        @Test
        @DisplayName("второй пользователь — EXECUTOR")
        void secondUserIsExecutor() {
            userService.getOrCreate(1L, "admin", null);
            User second = userService.getOrCreate(2L, "user", null);
            assertEquals(Role.EXECUTOR, second.role());
        }
    }

    @Nested
    @DisplayName("findById / findByUsername")
    class FindTests {

        @Test
        @DisplayName("findByUsername находит")
        void shouldFindByUsername() {
            userService.createUser("alice", Role.EXECUTOR);
            assertNotNull(userService.findByUsername("alice"));
        }

        @Test
        @DisplayName("findByUsername игнорирует регистр")
        void caseInsensitive() {
            userService.createUser("Alice");
            assertNotNull(userService.findByUsername("alice"));
            assertNotNull(userService.findByUsername("ALICE"));
        }

        @Test
        @DisplayName("findByUsername(unknown) возвращает null")
        void unknownReturnsNull() {
            assertNull(userService.findByUsername("unknown"));
        }
    }
}