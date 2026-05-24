package skybook.services;

import skybook.models.User;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * AuthService: Handles user registration, login, and session management.
 * Demonstrates: File handling, Exception handling, Security (password hashing)
 *
 * NOTE: Uses SHA-256 + salt for password hashing (no external BCrypt dependency needed).
 * To use BCrypt: add org.mindrot:jbcrypt:0.4 to classpath and swap hashPassword/verifyPassword.
 */
public class AuthService {

    private static final String DATA_DIR   = "skybook_data";
    private static final String USERS_FILE = DATA_DIR + "/users.csv";

    private static AuthService instance;
    private List<User> users;
    private User currentUser; // logged-in session

    private AuthService() {
        ensureFile();
        this.users = loadUsers();
        seedDefaultUsers();
    }

    public static AuthService getInstance() {
        if (instance == null) instance = new AuthService();
        return instance;
    }

    // ─── AUTH ────────────────────────────────────────────────────────────────────

    /**
     * Logs in a user. Returns the User if credentials match, throws otherwise.
     */
    public User login(String username, String password) throws Exception {
        if (username == null || username.isBlank())
            throw new Exception("Username is required.");
        if (password == null || password.isBlank())
            throw new Exception("Password is required.");

        Optional<User> found = users.stream()
                .filter(u -> u.getUsername().equalsIgnoreCase(username) && u.isActive())
                .findFirst();

        if (found.isEmpty())
            throw new Exception("User not found: " + username);

        User user = found.get();
        if (!verifyPassword(password, user.getPasswordHash()))
            throw new Exception("Incorrect password.");

        this.currentUser = user;
        System.out.println("[AuthService] Logged in: " + user);
        return user;
    }

    public void logout() {
        System.out.println("[AuthService] Logged out: " + (currentUser != null ? currentUser.getUsername() : "none"));
        this.currentUser = null;
    }

    public User getCurrentUser() { return currentUser; }
    public boolean isLoggedIn() { return currentUser != null; }

    public boolean isAdmin() { return isLoggedIn() && currentUser.getRole() == User.Role.ADMIN; }
    public boolean isStaff() { return isLoggedIn() &&
            (currentUser.getRole() == User.Role.STAFF || currentUser.getRole() == User.Role.ADMIN); }

    // ─── REGISTRATION ────────────────────────────────────────────────────────────

    public User register(String username, String password, String confirmPassword,
                         String email, String fullName, User.Role role) throws Exception {

        // Validation
        if (username == null || username.trim().length() < 3)
            throw new Exception("Username must be at least 3 characters.");
        if (password == null || password.length() < 6)
            throw new Exception("Password must be at least 6 characters.");
        if (!password.equals(confirmPassword))
            throw new Exception("Passwords do not match.");
        if (email == null || !email.contains("@"))
            throw new Exception("Invalid email address.");
        if (fullName == null || fullName.trim().isEmpty())
            throw new Exception("Full name is required.");

        // Check duplicate
        boolean exists = users.stream()
                .anyMatch(u -> u.getUsername().equalsIgnoreCase(username.trim()));
        if (exists)
            throw new Exception("Username '" + username + "' is already taken.");

        boolean emailExists = users.stream()
                .anyMatch(u -> u.getEmail().equalsIgnoreCase(email.trim()));
        if (emailExists)
            throw new Exception("An account with this email already exists.");

        // Create user
        String id = "U" + String.format("%03d", users.size() + 1);
        String hash = hashPassword(password);
        User user = new User(id, username.trim(), hash, email.trim(), fullName.trim(), role);

        users.add(user);
        saveUsers();
        System.out.println("[AuthService] Registered: " + user);
        return user;
    }

    // ─── USER MANAGEMENT ─────────────────────────────────────────────────────────

    public List<User> getAllUsers() { return new ArrayList<>(users); }

    public void deactivateUser(String userId) throws Exception {
        User u = users.stream().filter(x -> x.getId().equals(userId))
                .findFirst().orElseThrow(() -> new Exception("User not found: " + userId));
        u.setActive(false);
        saveUsers();
    }

    public void changePassword(String userId, String oldPassword, String newPassword) throws Exception {
        User u = users.stream().filter(x -> x.getId().equals(userId))
                .findFirst().orElseThrow(() -> new Exception("User not found."));
        if (!verifyPassword(oldPassword, u.getPasswordHash()))
            throw new Exception("Current password is incorrect.");
        if (newPassword == null || newPassword.length() < 6)
            throw new Exception("New password must be at least 6 characters.");
        u.setPasswordHash(hashPassword(newPassword));
        saveUsers();
    }

    // ─── PASSWORD HASHING ────────────────────────────────────────────────────────

    /**
     * Hash password with SHA-256 + random salt.
     * Format: base64(salt):base64(sha256(salt+password))
     */
    public static String hashPassword(String password) {
        try {
            SecureRandom rng = new SecureRandom();
            byte[] salt = new byte[16];
            rng.nextBytes(salt);

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hash = md.digest(password.getBytes("UTF-8"));

            String saltB64 = Base64.getEncoder().encodeToString(salt);
            String hashB64 = Base64.getEncoder().encodeToString(hash);
            return saltB64 + ":" + hashB64;
        } catch (Exception e) {
            throw new RuntimeException("Password hashing failed", e);
        }
    }

    public static boolean verifyPassword(String password, String stored) {
        try {
            String[] parts = stored.split(":", 2);
            if (parts.length != 2) return false;

            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[1]);

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] actualHash = md.digest(password.getBytes("UTF-8"));

            if (actualHash.length != expectedHash.length) return false;
            int diff = 0;
            for (int i = 0; i < actualHash.length; i++) diff |= actualHash[i] ^ expectedHash[i];
            return diff == 0;
        } catch (Exception e) {
            return false;
        }
    }

    // ─── FILE I/O ────────────────────────────────────────────────────────────────

    private List<User> loadUsers() {
        List<User> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(USERS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                User u = User.fromCSV(line);
                if (u != null) list.add(u);
            }
        } catch (IOException e) {
            System.err.println("[AuthService] Could not read users: " + e.getMessage());
        }
        return list;
    }

    private void saveUsers() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(USERS_FILE, false))) {
            for (User u : users) pw.println(u.toCSV());
        } catch (IOException e) {
            System.err.println("[AuthService] Could not save users: " + e.getMessage());
        }
    }

    private void ensureFile() {
        try {
            Files.createDirectories(Paths.get(DATA_DIR));
            File f = new File(USERS_FILE);
            if (!f.exists()) f.createNewFile();
        } catch (IOException e) {
            System.err.println("[AuthService] Init error: " + e.getMessage());
        }
    }

    /**
     * Seeds default users on first run.
     */
    private void seedDefaultUsers() {
        if (!users.isEmpty()) return;
        try {
            register("admin",     "admin123",  "admin123",  "admin@skybook.com",    "System Admin",    User.Role.ADMIN);
            register("staff1",    "staff123",  "staff123",  "staff@skybook.com",    "Ground Staff",    User.Role.STAFF);
            register("passenger", "pass123",   "pass123",   "passenger@skybook.com","John Passenger",  User.Role.PASSENGER);
            System.out.println("[AuthService] Default users seeded.");
        } catch (Exception e) {
            System.err.println("[AuthService] Seed error: " + e.getMessage());
        }
    }
}