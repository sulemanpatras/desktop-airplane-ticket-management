package skybook.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import skybook.models.User;
import skybook.services.AuthService;
import skybook.services.BookingService;

import java.util.List;
import java.util.stream.Collectors;

/**
 * User Management Screen (ADMIN only).
 *
 * FIX 7: Added:
 *  - Search by name/username/email
 *  - Filter by role
 *  - Ticket count per user (total + active/confirmed)
 *  - Create new user (any role) — email sent to the new user
 *  - Edit/update user — email sent to the updated user
 */
public class UserManagementScreen {

    private final AuthService    authService;
    private final BookingService bookingService;  // for ticket counts

    private VBox listBox;
    private TextField searchField;
    private ComboBox<String> roleFilter;

    public UserManagementScreen(AuthService authService) {
        this.authService    = authService;
        this.bookingService = new BookingService();  // read-only use
    }

    public VBox getView() {
        VBox view = new VBox(18);
        view.setPadding(new Insets(28));
        view.setStyle("-fx-background-color: #0f172a;");

        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Admin · User Management");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #f1f5f9;");

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);

        Button createBtn = new Button("+ Create User");
        createBtn.setStyle("""
            -fx-background-color: #34d399;
            -fx-text-fill: #0f172a;
            -fx-font-weight: bold;
            -fx-padding: 9 18;
            -fx-background-radius: 7;
            -fx-cursor: hand;
        """);
        createBtn.setOnAction(e -> showCreateUserDialog());

        header.getChildren().addAll(title, sp, createBtn);

        // ── Search & Filter bar ───────────────────────────────────────────────
        HBox filterBar = new HBox(12);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.setPadding(new Insets(12));
        filterBar.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 8;");

        searchField = new TextField();
        searchField.setPromptText("Search by name, username, or email…");
        searchField.setPrefWidth(280);
        styleField(searchField);

        roleFilter = new ComboBox<>();
        roleFilter.getItems().addAll("All Roles", "ADMIN", "STAFF", "PASSENGER");
        roleFilter.setValue("All Roles");
        roleFilter.setStyle("""
            -fx-background-color: #0f172a;
            -fx-text-fill: #f1f5f9;
            -fx-border-color: #334155;
            -fx-border-radius: 6;
            -fx-background-radius: 6;
        """);

        Button searchBtn = new Button("Search");
        searchBtn.setStyle("-fx-background-color:#38bdf8;-fx-text-fill:#0f172a;-fx-font-weight:bold;"
                + "-fx-padding:8 16;-fx-background-radius:6;-fx-cursor:hand;");
        searchBtn.setOnAction(e -> refreshList());
        searchField.setOnAction(e -> refreshList());

        Button resetBtn = new Button("Reset");
        resetBtn.setStyle("-fx-background-color:#334155;-fx-text-fill:#cbd5e1;-fx-font-weight:bold;"
                + "-fx-padding:8 14;-fx-background-radius:6;-fx-cursor:hand;");
        resetBtn.setOnAction(e -> { searchField.clear(); roleFilter.setValue("All Roles"); refreshList(); });

        filterBar.getChildren().addAll(searchField, roleFilter, searchBtn, resetBtn);

        // ── Table ─────────────────────────────────────────────────────────────
        listBox = new VBox(6);
        listBox.getChildren().add(buildTableHeader());
        refreshList();

        ScrollPane scroll = new ScrollPane(listBox);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: #0f172a;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        view.getChildren().addAll(header, filterBar, scroll);
        return view;
    }

    private void refreshList() {
        if (listBox.getChildren().size() > 1)
            listBox.getChildren().remove(1, listBox.getChildren().size());

        String query = searchField != null ? searchField.getText().trim().toLowerCase() : "";
        String role  = roleFilter  != null ? roleFilter.getValue() : "All Roles";

        List<User> users = authService.getAllUsers().stream()
                .filter(u -> query.isEmpty()
                        || u.getUsername().toLowerCase().contains(query)
                        || u.getFullName().toLowerCase().contains(query)
                        || u.getEmail().toLowerCase().contains(query))
                .filter(u -> "All Roles".equals(role) || u.getRole().name().equals(role))
                .collect(Collectors.toList());

        if (users.isEmpty()) {
            Label empty = new Label("No users found.");
            empty.setStyle("-fx-text-fill: #475569; -fx-padding: 20; -fx-font-size: 14px;");
            listBox.getChildren().add(empty);
            return;
        }

        for (User u : users) listBox.getChildren().add(buildUserRow(u));
    }

    private HBox buildTableHeader() {
        HBox row = new HBox();
        row.setPadding(new Insets(10, 16, 10, 16));
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: #334155; -fx-background-radius: 6;");
        row.getChildren().addAll(
            hCell("ID",       "#94a3b8",  60),
            hCell("USERNAME", "#94a3b8", 120),
            hCell("FULL NAME","#94a3b8", 160),
            hCell("EMAIL",    "#94a3b8", 180),
            hCell("ROLE",     "#94a3b8",  90),
            hCell("TICKETS",  "#94a3b8",  80),
            hCell("ACTIVE",   "#94a3b8",  70),
            hCell("STATUS",   "#94a3b8",  80),
            new Region() {{ HBox.setHgrow(this, Priority.ALWAYS); }},
            hCell("ACTIONS",  "#94a3b8",  130)
        );
        return row;
    }

    private HBox buildUserRow(User user) {
        HBox row = new HBox(8);
        row.setPadding(new Insets(11, 16, 11, 16));
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color:" + (user.isActive() ? "#1e293b" : "#1a1025")
                + ";-fx-background-radius:6;");

        String roleColor = switch (user.getRole()) {
            case ADMIN -> "#a78bfa"; case STAFF -> "#34d399"; case PASSENGER -> "#38bdf8";
        };

        // Ticket counts
        long total  = bookingService.getAllTickets().stream()
                .filter(t -> t.getPassengerEmail().equalsIgnoreCase(user.getEmail())).count();
        long active = bookingService.getAllTickets().stream()
                .filter(t -> t.getPassengerEmail().equalsIgnoreCase(user.getEmail()) && t.isConfirmed()).count();

        Label roleBadge = badge(user.getRole().name(), roleColor);

        String statusColor = user.isActive() ? "#34d399" : "#f87171";
        Label  statusBadge = badge(user.isActive() ? "ACTIVE" : "INACTIVE", statusColor);

        Button editBtn = actionBtn("✏ Edit", "#38bdf8");
        editBtn.setOnAction(e -> showEditUserDialog(user));

        Button deactivateBtn = actionBtn(user.isActive() ? "Deactivate" : "Deactivated", "#f87171");
        deactivateBtn.setDisable(!user.isActive());
        deactivateBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Deactivate user '" + user.getUsername() + "'?", ButtonType.YES, ButtonType.NO);
            confirm.showAndWait().ifPresent(btn -> {
                if (btn == ButtonType.YES) {
                    try { authService.deactivateUser(user.getId()); refreshList(); }
                    catch (Exception ex) { new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait(); }
                }
            });
        });

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox actions = new HBox(6, editBtn, deactivateBtn);

        row.getChildren().addAll(
            hCell(user.getId(),                              "#64748b",  60),
            hCell(user.getUsername(),                        "#f1f5f9", 120),
            hCell(user.getFullName(),                        "#f1f5f9", 160),
            hCell(user.getEmail(),                           "#94a3b8", 180),
            roleBadge,
            hCell(total + " (" + active + " active)",        "#38bdf8",  80),
            new Label("  ") {{ setPrefWidth(10); }},
            statusBadge,
            sp,
            actions
        );
        return row;
    }

    // ─── CREATE USER DIALOG (FIX 7) ──────────────────────────────────────────────

    private void showCreateUserDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Create New User");
        dialog.setHeaderText("Create a new SkyBook account");

        GridPane form = buildUserForm(null);
        PasswordField pwField   = (PasswordField) form.getUserData();  // stashed below
        TextField[] fields      = (TextField[]) form.getProperties().get("fields");
        ComboBox<String> roleBox = (ComboBox<String>) form.getProperties().get("roleBox");

        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    User.Role role = User.Role.valueOf(roleBox.getValue());
                    String tempPw = pwField.getText().isEmpty() ? "SkyBook@123" : pwField.getText();
                    User newUser = authService.register(
                        fields[1].getText(), tempPw, tempPw,
                        fields[2].getText(), fields[0].getText(), role
                    );
                    // FIX 7: send welcome email to new user only
                    new skybook.services.EmailService()
                            .sendAdminUserCreated(newUser, pwField.getText().isEmpty() ? "SkyBook@123" : null);
                    refreshList();
                    new Alert(Alert.AlertType.INFORMATION,
                            "User created and welcome email sent to " + newUser.getEmail()).showAndWait();
                } catch (Exception ex) {
                    new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
                }
            }
        });
    }

    // ─── EDIT USER DIALOG (FIX 7) ────────────────────────────────────────────────

    private void showEditUserDialog(User user) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit User – " + user.getUsername());
        dialog.setHeaderText("Update user details");

        GridPane form = buildUserForm(user);
        TextField[] fields     = (TextField[]) form.getProperties().get("fields");
        ComboBox<String> roleBox = (ComboBox<String>) form.getProperties().get("roleBox");

        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    User.Role role = User.Role.valueOf(roleBox.getValue());
                    authService.adminUpdateUser(user.getId(),
                            fields[0].getText(), fields[1].getText(),
                            fields[2].getText(), role);
                    refreshList();
                    new Alert(Alert.AlertType.INFORMATION,
                            "User updated. Notification email sent to " + user.getEmail()).showAndWait();
                } catch (Exception ex) {
                    new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
                }
            }
        });
    }

    /**
     * Returns a GridPane with stashed references in its properties.
     * properties["fields"]  = TextField[]{fullName, username, email}
     * properties["roleBox"] = ComboBox<String>
     * userData              = PasswordField (create only; null for edit)
     */
    private GridPane buildUserForm(User existing) {
        GridPane form = new GridPane();
        form.setHgap(14); form.setVgap(12); form.setPadding(new Insets(20));

        TextField fullNameField  = new TextField(existing != null ? existing.getFullName()  : "");
        TextField usernameField  = new TextField(existing != null ? existing.getUsername()  : "");
        TextField emailField     = new TextField(existing != null ? existing.getEmail()     : "");
        fullNameField.setPrefWidth(220); usernameField.setPrefWidth(220); emailField.setPrefWidth(220);

        ComboBox<String> roleBox = new ComboBox<>();
        roleBox.getItems().addAll("ADMIN", "STAFF", "PASSENGER");
        roleBox.setValue(existing != null ? existing.getRole().name() : "PASSENGER");
        roleBox.setPrefWidth(220);

        int r = 0;
        form.add(new Label("Full Name:"), 0, r); form.add(fullNameField, 1, r++);
        form.add(new Label("Username:"),  0, r); form.add(usernameField, 1, r++);
        form.add(new Label("Email:"),     0, r); form.add(emailField,    1, r++);
        form.add(new Label("Role:"),      0, r); form.add(roleBox,       1, r++);

        PasswordField pwField = null;
        if (existing == null) {
            pwField = new PasswordField();
            pwField.setPromptText("Password (leave blank = SkyBook@123)");
            pwField.setPrefWidth(220);
            form.add(new Label("Password:"), 0, r); form.add(pwField, 1, r);
        }

        form.getProperties().put("fields",  new TextField[]{fullNameField, usernameField, emailField});
        form.getProperties().put("roleBox", roleBox);
        form.setUserData(pwField);
        return form;
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────────

    private Label hCell(String text, String color, double width) {
        Label l = new Label(text != null ? text : "");
        l.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px;");
        l.setPrefWidth(width);
        return l;
    }

    private Label badge(String text, String color) {
        Label l = new Label("  " + text + "  ");
        l.setStyle("-fx-background-color:" + color + "22;-fx-text-fill:" + color
                + ";-fx-font-size:10px;-fx-background-radius:20;-fx-padding:3 8;");
        return l;
    }

    private Button actionBtn(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color:" + color + "22;-fx-text-fill:" + color
                + ";-fx-font-size:11px;-fx-padding:4 10;-fx-background-radius:5;-fx-cursor:hand;");
        return btn;
    }

    private void styleField(TextField tf) {
        tf.setStyle("""
            -fx-background-color: #0f172a;
            -fx-text-fill: #f1f5f9;
            -fx-prompt-text-fill: #475569;
            -fx-border-color: #334155;
            -fx-border-radius: 6;
            -fx-background-radius: 6;
            -fx-padding: 8;
        """);
    }
}