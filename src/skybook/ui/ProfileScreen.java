package skybook.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import skybook.models.User;
import skybook.services.AuthService;

/**
 * Profile Screen — with show/hide password toggles on all password fields.
 */
public class ProfileScreen {

    private final AuthService authService;

    public ProfileScreen(AuthService authService) {
        this.authService = authService;
    }

    public VBox getView() {
        User me = authService.getCurrentUser();

        VBox view = new VBox(24);
        view.setPadding(new Insets(36));
        view.setStyle("-fx-background-color: #0f172a;");
        view.setMaxWidth(560);

        Label title = new Label("My Profile");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #f1f5f9;");
        Label subtitle = new Label("Update your account details");
        subtitle.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");

        // ── Info card ──────────────────────────────────────────────────────
        VBox card = new VBox(16);
        card.setPadding(new Insets(24));
        card.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 12;");

        Label infoTitle = new Label("Account Information");
        infoTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #f1f5f9;");

        GridPane form = new GridPane();
        form.setHgap(16);
        form.setVgap(14);

        TextField fullNameField = styledField(me.getFullName());
        TextField usernameField = styledField(me.getUsername());
        TextField emailField    = styledField(me.getEmail());

        form.add(fieldLbl("Full Name"), 0, 0); form.add(fullNameField, 1, 0);
        form.add(fieldLbl("Username"),  0, 1); form.add(usernameField, 1, 1);
        form.add(fieldLbl("Email"),     0, 2); form.add(emailField,    1, 2);

        // ── Password change section ────────────────────────────────────────
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #334155;");

        Label pwTitle = new Label("Change Password");
        pwTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #94a3b8;");

        GridPane pwForm = new GridPane();
        pwForm.setHgap(16);
        pwForm.setVgap(14);

        PasswordField currentPwField = styledPassField("Current password");
        PasswordField newPwField     = styledPassField("New password (min 6 chars)");
        PasswordField confirmPwField = styledPassField("Confirm new password");

        // ── Show/hide rows ─────────────────────────────────────────────────
        HBox currentPwRow = buildPasswordRow(currentPwField);
        HBox newPwRow     = buildPasswordRow(newPwField);
        HBox confirmPwRow = buildPasswordRow(confirmPwField);

        pwForm.add(fieldLbl("Current Password"), 0, 0); pwForm.add(currentPwRow, 1, 0);
        pwForm.add(fieldLbl("New Password"),      0, 1); pwForm.add(newPwRow,     1, 1);
        pwForm.add(fieldLbl("Confirm Password"),  0, 2); pwForm.add(confirmPwRow, 1, 2);

        // ── Feedback label ─────────────────────────────────────────────────
        Label feedback = new Label("");
        feedback.setWrapText(true);
        feedback.setStyle("-fx-font-size: 12px;");

        // ── Save button ────────────────────────────────────────────────────
        Button saveBtn = new Button("Save Changes");
        saveBtn.setStyle("""
            -fx-background-color: #38bdf8;
            -fx-text-fill: #0f172a;
            -fx-font-weight: bold;
            -fx-padding: 10 24;
            -fx-background-radius: 8;
            -fx-cursor: hand;
        """);
        saveBtn.setOnAction(e -> {
            feedback.setText("");
            String newPw     = newPwField.getText();
            String confirmPw = confirmPwField.getText();

            if (!newPw.isEmpty() && !newPw.equals(confirmPw)) {
                feedback.setText("⚠ New passwords do not match.");
                feedback.setStyle("-fx-text-fill: #f87171; -fx-font-size: 12px;");
                return;
            }

            try {
                authService.updateProfile(
                    me.getId(),
                    fullNameField.getText(),
                    usernameField.getText(),
                    emailField.getText(),
                    currentPwField.getText().isEmpty() ? null : currentPwField.getText(),
                    newPw.isEmpty() ? null : newPw
                );
                feedback.setText("✓ Profile updated! A confirmation email has been sent to your inbox.");
                feedback.setStyle("-fx-text-fill: #34d399; -fx-font-size: 12px;");
                currentPwField.clear();
                newPwField.clear();
                confirmPwField.clear();
            } catch (Exception ex) {
                feedback.setText("⚠ " + ex.getMessage());
                feedback.setStyle("-fx-text-fill: #f87171; -fx-font-size: 12px;");
            }
        });

        HBox btnRow = new HBox(saveBtn);
        btnRow.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(infoTitle, form, sep, pwTitle, pwForm, feedback, btnRow);
        view.getChildren().addAll(title, subtitle, card);
        return view;
    }

    // ─── SHOW/HIDE PASSWORD HELPER ────────────────────────────────────────────

    private HBox buildPasswordRow(PasswordField passwordField) {
        TextField visibleField = new TextField();
        visibleField.setPromptText(passwordField.getPromptText());
        applyFieldStyle(visibleField);
        visibleField.setManaged(false);
        visibleField.setVisible(false);
        visibleField.setPrefWidth(260);

        passwordField.textProperty().addListener((obs, o, n) -> {
            if (!visibleField.isFocused()) visibleField.setText(n);
        });
        visibleField.textProperty().addListener((obs, o, n) -> {
            if (!passwordField.isFocused()) passwordField.setText(n);
        });

        Button eyeBtn = new Button("👁");
        eyeBtn.setStyle("""
            -fx-background-color: #334155;
            -fx-text-fill: #94a3b8;
            -fx-font-size: 13px;
            -fx-padding: 6 10;
            -fx-background-radius: 6;
            -fx-cursor: hand;
            -fx-min-width: 36px;
        """);

        final boolean[] showing = {false};

        eyeBtn.setOnAction(e -> {
            showing[0] = !showing[0];
            if (showing[0]) {
                visibleField.setText(passwordField.getText());
                passwordField.setManaged(false);
                passwordField.setVisible(false);
                visibleField.setManaged(true);
                visibleField.setVisible(true);
                eyeBtn.setText("🙈");
                eyeBtn.setStyle("""
                    -fx-background-color: #38bdf8;
                    -fx-text-fill: #0f172a;
                    -fx-font-size: 13px;
                    -fx-padding: 6 10;
                    -fx-background-radius: 6;
                    -fx-cursor: hand;
                    -fx-min-width: 36px;
                """);
            } else {
                passwordField.setText(visibleField.getText());
                visibleField.setManaged(false);
                visibleField.setVisible(false);
                passwordField.setManaged(true);
                passwordField.setVisible(true);
                eyeBtn.setText("👁");
                eyeBtn.setStyle("""
                    -fx-background-color: #334155;
                    -fx-text-fill: #94a3b8;
                    -fx-font-size: 13px;
                    -fx-padding: 6 10;
                    -fx-background-radius: 6;
                    -fx-cursor: hand;
                    -fx-min-width: 36px;
                """);
            }
        });

        StackPane fieldStack = new StackPane(passwordField, visibleField);
        HBox.setHgrow(fieldStack, Priority.ALWAYS);

        HBox row = new HBox(8, fieldStack, eyeBtn);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────

    private Label fieldLbl(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
        l.setPrefWidth(130);
        return l;
    }

    private TextField styledField(String value) {
        TextField tf = new TextField(value);
        applyFieldStyle(tf);
        tf.setPrefWidth(260);
        return tf;
    }

    private PasswordField styledPassField(String prompt) {
        PasswordField pf = new PasswordField();
        pf.setPromptText(prompt);
        applyFieldStyle(pf);
        pf.setPrefWidth(260);
        return pf;
    }

    private void applyFieldStyle(Control c) {
        String base = """
            -fx-background-color: #0f172a;
            -fx-text-fill: #f1f5f9;
            -fx-prompt-text-fill: #475569;
            -fx-border-color: #334155;
            -fx-border-radius: 6;
            -fx-background-radius: 6;
            -fx-padding: 9;
            -fx-font-size: 13px;
        """;
        String focused = base.replace("#334155", "#38bdf8");
        c.setStyle(base);
        c.focusedProperty().addListener((obs, o, f) -> c.setStyle(f ? focused : base));
    }
}