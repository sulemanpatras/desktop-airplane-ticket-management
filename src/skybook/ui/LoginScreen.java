package skybook.ui;

import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import skybook.models.User;
import skybook.services.AuthService;

/**
 * Login Screen – authenticates users and routes to main app.
 * Demonstrates: JavaFX layouts, event handling, custom styling
 */
public class LoginScreen {

    private final AuthService authService;
    private final Runnable onLoginSuccess;
    private Label errorLabel;

    public LoginScreen(AuthService authService, Runnable onLoginSuccess) {
        this.authService = authService;
        this.onLoginSuccess = onLoginSuccess;
    }

    public StackPane getView() {
        // Root
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: #0f172a;");

        // Background decorative circles
        root.getChildren().add(buildBackground());

        // Card
        VBox card = new VBox(20);
        card.setMaxWidth(420);
        card.setPadding(new Insets(40));
        card.setAlignment(Pos.CENTER);
        card.setStyle("""
            -fx-background-color: #1e293b;
            -fx-background-radius: 16;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 30, 0, 0, 8);
        """);

        // Logo
        VBox logoBox = new VBox(4);
        logoBox.setAlignment(Pos.CENTER);
        Label logo = new Label("✈ SkyBook");
        logo.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");
        Label tagline = new Label("Airline Ticket Management");
        tagline.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");
        logoBox.getChildren().addAll(logo, tagline);

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #334155;");

        // Form
        VBox form = new VBox(14);

        Label userLbl = fieldLabel("Username");
        TextField usernameField = styledField("Enter your username");

        Label passLbl = fieldLabel("Password");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter your password");
        styleTextField(passwordField);

        errorLabel = new Label("");
        errorLabel.setStyle("-fx-text-fill: #f87171; -fx-font-size: 12px;");
        errorLabel.setWrapText(true);

        Button loginBtn = new Button("Sign In");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setStyle("""
            -fx-background-color: #38bdf8;
            -fx-text-fill: #0f172a;
            -fx-font-weight: bold;
            -fx-font-size: 14px;
            -fx-padding: 12 0;
            -fx-background-radius: 8;
            -fx-cursor: hand;
        """);
        loginBtn.setOnMouseEntered(e -> loginBtn.setStyle("""
            -fx-background-color: #0ea5e9;
            -fx-text-fill: #0f172a;
            -fx-font-weight: bold;
            -fx-font-size: 14px;
            -fx-padding: 12 0;
            -fx-background-radius: 8;
            -fx-cursor: hand;
        """));
        loginBtn.setOnMouseExited(e -> loginBtn.setStyle("""
            -fx-background-color: #38bdf8;
            -fx-text-fill: #0f172a;
            -fx-font-weight: bold;
            -fx-font-size: 14px;
            -fx-padding: 12 0;
            -fx-background-radius: 8;
            -fx-cursor: hand;
        """));

        loginBtn.setOnAction(e -> doLogin(usernameField.getText(), passwordField.getText()));

        // Allow Enter key
        passwordField.setOnAction(e -> doLogin(usernameField.getText(), passwordField.getText()));
        usernameField.setOnAction(e -> passwordField.requestFocus());

        // Register link
        HBox registerRow = new HBox(6);
        registerRow.setAlignment(Pos.CENTER);
        Label noAcct = new Label("Don't have an account?");
        noAcct.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
        Hyperlink registerLink = new Hyperlink("Register here");
        registerLink.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 12px; -fx-border-color: transparent;");
        registerLink.setOnAction(e -> showRegisterPanel(card, loginBtn, form, usernameField, passwordField));
        registerRow.getChildren().addAll(noAcct, registerLink);

        // Demo credentials hint
        VBox demoBox = new VBox(4);
        demoBox.setPadding(new Insets(10, 0, 0, 0));
        demoBox.setStyle("-fx-background-color: #0f172a; -fx-background-radius: 6; -fx-padding: 10;");
        Label demoTitle = new Label("Demo credentials:");
        demoTitle.setStyle("-fx-text-fill: #475569; -fx-font-size: 11px; -fx-font-weight: bold;");
        Label demoAdmin = new Label("admin / admin123  (ADMIN)");
        Label demoStaff = new Label("staff1 / staff123  (STAFF)");
        Label demoPax   = new Label("passenger / pass123  (PASSENGER)");
        for (Label l : new Label[]{demoAdmin, demoStaff, demoPax})
            l.setStyle("-fx-text-fill: #475569; -fx-font-size: 11px; -fx-font-family: monospace;");
        demoBox.getChildren().addAll(demoTitle, demoAdmin, demoStaff, demoPax);

        form.getChildren().addAll(userLbl, usernameField, passLbl, passwordField, errorLabel, loginBtn);
        card.getChildren().addAll(logoBox, sep, form, registerRow, demoBox);

        StackPane.setAlignment(card, Pos.CENTER);
        root.getChildren().add(card);

        // Fade in
        FadeTransition ft = new FadeTransition(Duration.millis(500), card);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();

        return root;
    }

    private void doLogin(String username, String password) {
        errorLabel.setText("");
        try {
            User user = authService.login(username, password);
            onLoginSuccess.run();
        } catch (Exception e) {
            errorLabel.setText("⚠ " + e.getMessage());

            FadeTransition shake = new FadeTransition(Duration.millis(100), errorLabel);
            shake.setFromValue(0);
            shake.setToValue(1);
            shake.play();
        }
    }

    /**
     * Transforms the login card into a registration form in-place.
     */
    private void showRegisterPanel(VBox card, Button loginBtn, VBox loginForm,
                                   TextField usernameField, PasswordField passwordField) {
        card.getChildren().clear();

        // Header
        VBox logoBox = new VBox(4);
        logoBox.setAlignment(Pos.CENTER);
        Label logo = new Label("✈ SkyBook");
        logo.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");
        Label tagline = new Label("Create Your Account");
        tagline.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");
        logoBox.getChildren().addAll(logo, tagline);

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #334155;");

        // Registration form
        VBox form = new VBox(10);

        TextField fullNameField = styledField("Full Name");
        TextField regUsernameField = styledField("Username");
        TextField emailField = styledField("Email Address");
        PasswordField regPassField = new PasswordField();
        regPassField.setPromptText("Password (min 6 chars)");
        styleTextField(regPassField);
        PasswordField confirmPassField = new PasswordField();
        confirmPassField.setPromptText("Confirm Password");
        styleTextField(confirmPassField);

        // Role selection (Passengers only — admin/staff set by admin)
        Label roleLbl = fieldLabel("Account Type");
        ComboBox<String> roleBox = new ComboBox<>();
        roleBox.getItems().addAll("Passenger", "Staff");
        roleBox.setValue("Passenger");
        roleBox.setMaxWidth(Double.MAX_VALUE);
        roleBox.setStyle("""
            -fx-background-color: #0f172a;
            -fx-text-fill: #f1f5f9;
            -fx-border-color: #334155;
            -fx-border-radius: 6;
            -fx-background-radius: 6;
            -fx-padding: 8;
        """);

        Label regError = new Label("");
        regError.setStyle("-fx-text-fill: #f87171; -fx-font-size: 12px;");
        regError.setWrapText(true);

        Button registerBtn = new Button("Create Account");
        registerBtn.setMaxWidth(Double.MAX_VALUE);
        registerBtn.setStyle("""
            -fx-background-color: #34d399;
            -fx-text-fill: #0f172a;
            -fx-font-weight: bold;
            -fx-font-size: 14px;
            -fx-padding: 12 0;
            -fx-background-radius: 8;
            -fx-cursor: hand;
        """);

        registerBtn.setOnAction(e -> {
            regError.setText("");
            User.Role role = roleBox.getValue().equals("Staff") ? User.Role.STAFF : User.Role.PASSENGER;
            try {
                authService.register(
                    regUsernameField.getText(), regPassField.getText(),
                    confirmPassField.getText(), emailField.getText(),
                    fullNameField.getText(), role
                );
                // Success — go back to login
                showLoginPanel(card);
                errorLabel.setText("✓ Account created! Please sign in.");
                errorLabel.setStyle("-fx-text-fill: #34d399; -fx-font-size: 12px;");
            } catch (Exception ex) {
                regError.setText("⚠ " + ex.getMessage());
            }
        });

        // Back link
        Hyperlink backLink = new Hyperlink("← Back to Sign In");
        backLink.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 12px; -fx-border-color: transparent;");
        backLink.setOnAction(e -> showLoginPanel(card));

        form.getChildren().addAll(
            fieldLabel("Full Name"), fullNameField,
            fieldLabel("Username"), regUsernameField,
            fieldLabel("Email"), emailField,
            fieldLabel("Password"), regPassField,
            fieldLabel("Confirm Password"), confirmPassField,
            roleLbl, roleBox,
            regError, registerBtn
        );

        card.getChildren().addAll(logoBox, sep, form, backLink);
    }

    private void showLoginPanel(VBox card) {
        // Rebuild login view — simplest approach: reload getView() children
        card.getChildren().clear();

        VBox logoBox = new VBox(4);
        logoBox.setAlignment(Pos.CENTER);
        Label logo = new Label("✈ SkyBook");
        logo.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");
        Label tagline = new Label("Airline Ticket Management");
        tagline.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");
        logoBox.getChildren().addAll(logo, tagline);

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #334155;");

        VBox form = new VBox(14);
        TextField usernameField = styledField("Enter your username");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter your password");
        styleTextField(passwordField);

        errorLabel = new Label("");
        errorLabel.setStyle("-fx-text-fill: #f87171; -fx-font-size: 12px;");

        Button loginBtn = new Button("Sign In");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setStyle("""
            -fx-background-color: #38bdf8;
            -fx-text-fill: #0f172a;
            -fx-font-weight: bold;
            -fx-font-size: 14px;
            -fx-padding: 12 0;
            -fx-background-radius: 8;
            -fx-cursor: hand;
        """);
        loginBtn.setOnAction(e -> doLogin(usernameField.getText(), passwordField.getText()));
        passwordField.setOnAction(e -> doLogin(usernameField.getText(), passwordField.getText()));

        HBox registerRow = new HBox(6);
        registerRow.setAlignment(Pos.CENTER);
        Label noAcct = new Label("Don't have an account?");
        noAcct.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
        Hyperlink registerLink = new Hyperlink("Register here");
        registerLink.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 12px; -fx-border-color: transparent;");
        registerLink.setOnAction(e -> showRegisterPanel(card, loginBtn, form, usernameField, passwordField));
        registerRow.getChildren().addAll(noAcct, registerLink);

        form.getChildren().addAll(
            fieldLabel("Username"), usernameField,
            fieldLabel("Password"), passwordField,
            errorLabel, loginBtn
        );

        VBox demoBox = new VBox(4);
        demoBox.setStyle("-fx-background-color: #0f172a; -fx-background-radius: 6; -fx-padding: 10;");
        Label demoTitle = new Label("Demo credentials:");
        demoTitle.setStyle("-fx-text-fill: #475569; -fx-font-size: 11px; -fx-font-weight: bold;");
        Label demoAdmin = new Label("admin / admin123  (ADMIN)");
        Label demoStaff = new Label("staff1 / staff123  (STAFF)");
        Label demoPax   = new Label("passenger / pass123  (PASSENGER)");
        for (Label l : new Label[]{demoAdmin, demoStaff, demoPax})
            l.setStyle("-fx-text-fill: #475569; -fx-font-size: 11px; -fx-font-family: monospace;");
        demoBox.getChildren().addAll(demoTitle, demoAdmin, demoStaff, demoPax);

        card.getChildren().addAll(logoBox, sep, form, registerRow, demoBox);
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────────

    private Label fieldLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
        return l;
    }

    private TextField styledField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        styleTextField(tf);
        return tf;
    }

    private void styleTextField(Control tf) {
        tf.setStyle("""
            -fx-background-color: #0f172a;
            -fx-text-fill: #f1f5f9;
            -fx-prompt-text-fill: #475569;
            -fx-border-color: #334155;
            -fx-border-radius: 6;
            -fx-background-radius: 6;
            -fx-padding: 10;
            -fx-font-size: 13px;
        """);
        tf.focusedProperty().addListener((obs, old, focused) -> {
            if (focused) {
                tf.setStyle("""
                    -fx-background-color: #0f172a;
                    -fx-text-fill: #f1f5f9;
                    -fx-prompt-text-fill: #475569;
                    -fx-border-color: #38bdf8;
                    -fx-border-radius: 6;
                    -fx-background-radius: 6;
                    -fx-padding: 10;
                    -fx-font-size: 13px;
                """);
            } else {
                tf.setStyle("""
                    -fx-background-color: #0f172a;
                    -fx-text-fill: #f1f5f9;
                    -fx-prompt-text-fill: #475569;
                    -fx-border-color: #334155;
                    -fx-border-radius: 6;
                    -fx-background-radius: 6;
                    -fx-padding: 10;
                    -fx-font-size: 13px;
                """);
            }
        });
    }

    private Pane buildBackground() {
        Pane bg = new Pane();
        // Decorative blurred circles done via CSS box-shadows / overlapping shapes
        javafx.scene.shape.Circle c1 = new javafx.scene.shape.Circle(200);
        c1.setFill(Color.web("#38bdf820"));
        c1.setLayoutX(100);
        c1.setLayoutY(100);

        javafx.scene.shape.Circle c2 = new javafx.scene.shape.Circle(150);
        c2.setFill(Color.web("#a78bfa15"));
        c2.setLayoutX(900);
        c2.setLayoutY(600);

        bg.getChildren().addAll(c1, c2);
        return bg;
    }
}