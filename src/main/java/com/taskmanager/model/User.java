package com.taskmanager.model;

/**
 * Пользователь системы.
 *
 * @apiNote User — value object для чтения после сохранения.
 *          equals/hashCode учитывают все поля, включая {@code id}.
 *          <ul>
 *            <li>Два {@code create("alice", ...)} с {@code id == null} равны.</li>
 *            <li>Не используйте {@code User} в {@code HashSet/HashMap} до сохранения.</li>
 *            <li>Для коллекций применяйте {@code Map<Long, User>} с ключом id.</li>
 *          </ul>
 */
public record User(Long id, String username, Role role) {

    private static final int MAX_USERNAME_LENGTH = 50;
    private static final Role DEFAULT_ROLE = Role.EXECUTOR;

    /**
     * @throws IllegalArgumentException если username null/пустой/длиннее {@value MAX_USERNAME_LENGTH},
     *                                  id <= 0, role == null
     */
    public User {
        if (username == null) {
            throw new IllegalArgumentException("Имя пользователя не может быть null");
        }
        String trimmed = username.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Имя пользователя не может быть пустым");
        }
        if (trimmed.length() > MAX_USERNAME_LENGTH) {
            throw new IllegalArgumentException(
                "Имя пользователя не может быть длиннее " + MAX_USERNAME_LENGTH + " символов");
        }
        username = trimmed;

        if (id != null && id <= 0) {
            throw new IllegalArgumentException("ID должен быть положительным");
        }
        if (role == null) {
            throw new IllegalArgumentException("Роль не может быть null");
        }
    }


    // Фабрики

    /**
     * Создаёт нового пользователя с ролью по умолчанию ({@link Role#EXECUTOR}).
     */
    public static User create(String username) {
        return create(username, DEFAULT_ROLE);
    }

    /**
     * Создаёт нового пользователя.
     * Если {@code role == null} — используется {@link Role#EXECUTOR}.
     */
    public static User create(String username, Role role) {
        return new User(null, username, role == null ? DEFAULT_ROLE : role);
    }


    // with*

    /**
     * Присваивает id при первом сохранении в storage.
     *
     * ⚠️ Использовать ТОЛЬКО из storage.save().
     * Повторный вызов (когда id уже установлен) запрещён — это сломало бы
     * связи с задачами, которые ссылаются на старый id.
     *
     * @throws IllegalStateException если id уже установлен
     * @throws IllegalArgumentException если newId <= 0
     */
    public User withId(Long newId) {
        if (id != null) {
            throw new IllegalStateException(
                "Нельзя менять id уже сохранённого пользователя: " + id + " → " + newId);
        }
        return new User(newId, username, role);
    }

    /**
     * @throws IllegalArgumentException если newUsername null/пустой/длиннее {@value MAX_USERNAME_LENGTH}
     */
    public User withUsername(String newUsername) {
        return new User(id, newUsername, role);
    }

    /**
     * @throws IllegalArgumentException если newRole == null
     */
    public User withRole(Role newRole) {
        return new User(id, username, newRole);
    }


    // Вспомогательные

    public boolean isManager()  { return role == Role.MANAGER; }
    public boolean isExecutor() { return role == Role.EXECUTOR; }
    public boolean isObserver() { return role == Role.OBSERVER; }
}