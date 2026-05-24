package skybook.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * User model for authentication system.
 * Supports roles: ADMIN, STAFF, PASSENGER
 */
public class User {

    public enum Role { ADMIN, STAFF, PASSENGER }

    private String id;
    private String username;
    private String passwordHash; // BCrypt hash
    private String email;
    private String fullName;
    private Role role;
    private boolean active;
    private String createdAt;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public User() {
        this.active = true;
        this.createdAt = LocalDateTime.now().format(FMT);
    }

    public User(String id, String username, String passwordHash,
                String email, String fullName, Role role) {
        this();
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
    }

    // CSV serialization
    public String toCSV() {
        return String.join(",",
                id, username, passwordHash, email, fullName,
                role.name(), String.valueOf(active), createdAt);
    }

    public static User fromCSV(String line) {
        String[] p = line.split(",", -1);
        if (p.length < 8) return null;
        User u = new User(p[0], p[1], p[2], p[3], p[4], Role.valueOf(p[5]));
        u.setActive(Boolean.parseBoolean(p[6]));
        u.setCreatedAt(p[7]);
        return u;
    }

    // Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return String.format("[%s] %s (%s) - %s", role, fullName, username, email);
    }
}