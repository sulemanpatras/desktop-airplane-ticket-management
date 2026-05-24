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
 * AuthService: Handles user registration, login, session management, and profile updates.
 *
 * FIX 6: Registration no longer exposes role selection — new users are always PASSENGER.
 * FIX 5: Added updateProfile() method; sends confirmation email to the user (not admin).
 * FIX 7: Admin can create/update any user; email sent to affected user only.
 */
public class AuthService {

    private static final String DATA_DIR   = "skybook_data";
    private static final String USERS_FILE = DATA_DIR + "/users.csv";

    private static AuthService instance;
    private List<User> users;
    private User currentUser;

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
    public boolean isLoggedIn()  { return currentUser != null; }
    public boolean isAdmin()     { return isLoggedIn() && currentUser.getRole() == User.Role.ADMIN; }
    public boolean isStaff()     { return isLoggedIn() &&
            (currentUser.getRole() == User.Role.STAFF || currentUser.getRole() == User.Role.ADMIN); }

    // ─── REGISTRATION (FIX 6: always PASSENGER, no role choice) ─────────────────

    /**
     * Public self-registration — role is always PASSENGER.
     */
    public User register(String username, String password, String confirmPassword,
                         String email, String fullName) throws Exception {
        return register(username, password, confirmPassword, email, fullName, User.Role.PASSENGER);
    }

    /**
     * Internal/admin registration — caller specifies role.
     */
    public User register(String username, String password, String confirmPassword,
                         String email, String fullName, User.Role role) throws Exception {

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

        boolean exists = users.stream()
                .anyMatch(u -> u.getUsername().equalsIgnoreCase(username.trim()));
        if (exists)
            throw new Exception("Username '" + username + "' is already taken.");

        boolean emailExists = users.stream()
                .anyMatch(u -> u.getEmail().equalsIgnoreCase(email.trim()));
        if (emailExists)
            throw new Exception("An account with this email already exists.");

        String id   = "U" + String.format("%03d", users.size() + 1);
        String hash = hashPassword(password);
        User user   = new User(id, username.trim(), hash, email.trim(), fullName.trim(), role);

        users.add(user);
        saveUsers();
        System.out.println("[AuthService] Registered: " + user);
        return user;
    }

    // ─── PROFILE UPDATE (FIX 5) ──────────────────────────────────────────────────

    /**
     * Updates profile fields for the given userId.
     * Sends a profile-update confirmation email to the user only.
     *
     * @param userId        target user
     * @param newFullName   new display name (null = no change)
     * @param newUsername   new username (null = no change)
     * @param newEmail      new email (null = no change)
     * @param oldPassword   current password (required for password change)
     * @param newPassword   new password (null = no change)
     */
    public void updateProfile(String userId,
                              String newFullName,
                              String newUsername,
                              String newEmail,
                              String oldPassword,
                              String newPassword) throws Exception {

        User u = users.stream().filter(x -> x.getId().equals(userId))
                .findFirst().orElseThrow(() -> new Exception("User not found."));

        List<String> changes = new ArrayList<>();

        // Full name
        if (newFullName != null && !newFullName.trim().isEmpty()
                && !newFullName.trim().equals(u.getFullName())) {
            u.setFullName(newFullName.trim());
            changes.add("Full name updated");
        }

        // Username
        if (newUsername != null && !newUsername.trim().isEmpty()
                && !newUsername.trim().equalsIgnoreCase(u.getUsername())) {
            if (newUsername.trim().length() < 3)
                throw new Exception("Username must be at least 3 characters.");
            boolean taken = users.stream()
                    .anyMatch(x -> !x.getId().equals(userId)
                            && x.getUsername().equalsIgnoreCase(newUsername.trim()));
            if (taken) throw new Exception("Username '" + newUsername + "' is already taken.");
            u.setUsername(newUsername.trim());
            changes.add("Username updated");
        }

        // Email
        if (newEmail != null && !newEmail.trim().isEmpty()
                && !newEmail.trim().equalsIgnoreCase(u.getEmail())) {
            if (!newEmail.contains("@")) throw new Exception("Invalid email address.");
            boolean taken = users.stream()
                    .anyMatch(x -> !x.getId().equals(userId)
                            && x.getEmail().equalsIgnoreCase(newEmail.trim()));
            if (taken) throw new Exception("This email is already in use.");
            u.setEmail(newEmail.trim());
            changes.add("Email updated");
        }

        // Password
        if (newPassword != null && !newPassword.isEmpty()) {
            if (oldPassword == null || !verifyPassword(oldPassword, u.getPasswordHash()))
                throw new Exception("Current password is incorrect.");
            if (newPassword.length() < 6)
                throw new Exception("New password must be at least 6 characters.");
            u.setPasswordHash(hashPassword(newPassword));
            changes.add("Password changed");
        }

        if (changes.isEmpty()) throw new Exception("No changes were made.");

        saveUsers();

        // Reflect changes in current session if same user
        if (currentUser != null && currentUser.getId().equals(userId)) {
            this.currentUser = u;
        }

        // Send email to the user (not admin) — FIX 5
        try {
            EmailService emailService = new EmailService();
            emailService.sendProfileUpdateNotification(u, changes);
        } catch (Exception e) {
            System.err.println("[AuthService] Profile email failed: " + e.getMessage());
        }
    }

    // ─── ADMIN: UPDATE ANY USER (FIX 7) ─────────────────────────────────────────

    /**
     * Admin-only: update any user's details and role.
     * Sends email to the updated user only.
     */
    public void adminUpdateUser(String userId, String newFullName, String newUsername,
                                String newEmail, User.Role newRole) throws Exception {
        User u = users.stream().filter(x -> x.getId().equals(userId))
                .findFirst().orElseThrow(() -> new Exception("User not found."));

        List<String> changes = new ArrayList<>();

        if (newFullName != null && !newFullName.trim().isEmpty()) {
            u.setFullName(newFullName.trim()); changes.add("Full name updated"); }
        if (newUsername != null && !newUsername.trim().isEmpty()) {
            boolean taken = users.stream().anyMatch(x -> !x.getId().equals(userId)
                    && x.getUsername().equalsIgnoreCase(newUsername.trim()));
            if (taken) throw new Exception("Username already taken.");
            u.setUsername(newUsername.trim()); changes.add("Username updated"); }
        if (newEmail != null && !newEmail.trim().isEmpty()) {
            if (!newEmail.contains("@")) throw new Exception("Invalid email.");
            u.setEmail(newEmail.trim()); changes.add("Email updated"); }
        if (newRole != null && newRole != u.getRole()) {
            u.setRole(newRole); changes.add("Role changed to " + newRole.name()); }

        if (changes.isEmpty()) throw new Exception("No changes made.");
        saveUsers();

        try {
            new EmailService().sendProfileUpdateNotification(u, changes);
        } catch (Exception e) {
            System.err.println("[AuthService] Admin-update email failed: " + e.getMessage());
        }
    }

    // ─── USER MANAGEMENT ─────────────────────────────────────────────────────────

    public List<User> getAllUsers() { return new ArrayList<>(users); }

    public Optional<User> findUserById(String userId) {
        return users.stream().filter(u -> u.getId().equals(userId)).findFirst();
    }

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

    public static String hashPassword(String password) {
        try {
            SecureRandom rng = new SecureRandom();
            byte[] salt = new byte[16];
            rng.nextBytes(salt);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hash = md.digest(password.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(salt)
                 + ":" + Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Password hashing failed", e);
        }
    }

    public static boolean verifyPassword(String password, String stored) {
        try {
            String[] parts = stored.split(":", 2);
            if (parts.length != 2) return false;
            byte[] salt         = Base64.getDecoder().decode(parts[0]);
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

    private void seedDefaultUsers() {
        if (!users.isEmpty()) return;
        try {
            register("admin",     "admin123", "admin123", "admin@skybook.com",    "System Admin",   User.Role.ADMIN);
            register("staff1",    "staff123", "staff123", "staff@skybook.com",    "Ground Staff",   User.Role.STAFF);
            register("passenger", "pass123",  "pass123",  "passenger@skybook.com","John Passenger", User.Role.PASSENGER);
            System.out.println("[AuthService] Default users seeded.");
        } catch (Exception e) {
            System.err.println("[AuthService] Seed error: " + e.getMessage());
        }
    }
}