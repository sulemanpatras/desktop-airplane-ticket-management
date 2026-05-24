package skybook.ui;

import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import skybook.models.User;
import skybook.services.AuthService;

/**
 * Login Screen with show/hide password toggle on all password fields.
 */
public class LoginScreen {

    private final AuthService authService;
    private final Runnable onLoginSuccess;
    private Label errorLabel;

    public LoginScreen(AuthService authService, Runnable onLoginSuccess) {
        this.authService    = authService;
        this.onLoginSuccess = onLoginSuccess;
    }

    public StackPane getView() {
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: #0f172a;");
        root.getChildren().add(buildBackground());

        VBox card = buildLoginCard();
        StackPane.setAlignment(card, Pos.CENTER);
        root.getChildren().add(card);

        FadeTransition ft = new FadeTransition(Duration.millis(500), card);
        ft.setFromValue(0); ft.setToValue(1); ft.play();

        return root;
    }

    // ─── LOGIN CARD ──────────────────────────────────────────────────────────

    private VBox buildLoginCard() {
        VBox card = new VBox(20);
        card.setMaxWidth(420);
        card.setPadding(new Insets(40));
        card.setAlignment(Pos.CENTER);
        card.setStyle("""
            -fx-background-color: #1e293b;
            -fx-background-radius: 16;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 30, 0, 0, 8);
        """);
        populateLoginCard(card);
        return card;
    }

    private void populateLoginCard(VBox card) {
        card.getChildren().clear();

        VBox logoBox  = buildLogo("Airline Ticket Management");
        Separator sep = buildSep();

        VBox form = new VBox(14);

        Label userLbl      = fieldLabel("Username");
        TextField usernameField = styledField("Enter your username");

        Label passLbl      = fieldLabel("Password");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter your password");
        styleTextField(passwordField);

        // ── Show/hide password row ────────────────────────────────────────
        HBox passwordRow = buildPasswordRow(passwordField);

        errorLabel = new Label("");
        errorLabel.setStyle("-fx-text-fill: #f87171; -fx-font-size: 12px;");
        errorLabel.setWrapText(true);

        Button loginBtn = primaryBtn("Sign In", "#38bdf8", "#0f172a");
        loginBtn.setOnAction(e -> doLogin(usernameField.getText(), passwordField.getText()));
        passwordField.setOnAction(e -> doLogin(usernameField.getText(), passwordField.getText()));
        usernameField.setOnAction(e -> passwordField.requestFocus());

        HBox registerRow = new HBox(6);
        registerRow.setAlignment(Pos.CENTER);
        Label noAcct = new Label("Don't have an account?");
        noAcct.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
        Hyperlink registerLink = new Hyperlink("Register here");
        registerLink.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 12px; -fx-border-color: transparent;");
        registerLink.setOnAction(e -> showRegisterPanel(card));
        registerRow.getChildren().addAll(noAcct, registerLink);

        VBox demoBox = new VBox(4);
        demoBox.setStyle("-fx-background-color: #0f172a; -fx-background-radius: 6; -fx-padding: 10;");
        Label demoTitle = new Label("Demo credentials:");
        demoTitle.setStyle("-fx-text-fill: #475569; -fx-font-size: 11px; -fx-font-weight: bold;");
        for (Label l : new Label[]{
                new Label("admin / admin123  (ADMIN)"),
                new Label("staff1 / staff123  (STAFF)"),
                new Label("passenger / pass123  (PASSENGER)")}) {
            l.setStyle("-fx-text-fill: #475569; -fx-font-size: 11px; -fx-font-family: monospace;");
            demoBox.getChildren().add(l);
        }
        demoBox.getChildren().add(0, demoTitle);

        form.getChildren().addAll(
            userLbl, usernameField,
            passLbl, passwordRow,
            errorLabel, loginBtn
        );
        card.getChildren().addAll(logoBox, sep, form, registerRow, demoBox);
    }

    // ─── REGISTER PANEL ──────────────────────────────────────────────────────

    private void showRegisterPanel(VBox card) {
        card.getChildren().clear();
        card.setMaxWidth(440);

        VBox logoBox  = buildLogo("Create Your Account");
        Separator sep = buildSep();

        TextField fullNameField    = styledField("Full Name");
        TextField regUsernameField = styledField("Username (min 3 chars)");
        TextField emailField       = styledField("Email Address");

        PasswordField regPassField = new PasswordField();
        regPassField.setPromptText("Password (min 6 chars)");
        styleTextField(regPassField);

        PasswordField confirmPassField = new PasswordField();
        confirmPassField.setPromptText("Confirm Password");
        styleTextField(confirmPassField);

        // Show/hide rows for both password fields
        HBox regPassRow     = buildPasswordRow(regPassField);
        HBox confirmPassRow = buildPasswordRow(confirmPassField);

        Label regError = new Label("");
        regError.setStyle("-fx-text-fill: #f87171; -fx-font-size: 12px;");
        regError.setWrapText(true);

        Button registerBtn = primaryBtn("Create Account", "#34d399", "#0f172a");
        registerBtn.setOnAction(e -> {
            regError.setText("");
            try {
                authService.register(
                    regUsernameField.getText(),
                    regPassField.getText(),
                    confirmPassField.getText(),
                    emailField.getText(),
                    fullNameField.getText()
                );
                populateLoginCard(card);
                if (errorLabel != null) {
                    errorLabel.setText("✓ Account created! Please sign in.");
                    errorLabel.setStyle("-fx-text-fill: #34d399; -fx-font-size: 12px;");
                }
            } catch (Exception ex) {
                regError.setText("⚠ " + ex.getMessage());
            }
        });

        Hyperlink backLink = new Hyperlink("← Back to Sign In");
        backLink.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 12px; -fx-border-color: transparent;");
        backLink.setOnAction(e -> populateLoginCard(card));

        VBox formContent = new VBox(10);
        formContent.getChildren().addAll(
            fieldLabel("Full Name"),         fullNameField,
            fieldLabel("Username"),          regUsernameField,
            fieldLabel("Email"),             emailField,
            fieldLabel("Password"),          regPassRow,
            fieldLabel("Confirm Password"),  confirmPassRow,
            regError, registerBtn
        );

        ScrollPane scroll = new ScrollPane(formContent);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(340);
        scroll.setMaxHeight(360);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;"
                + "-fx-border-color: transparent;");

        card.getChildren().addAll(logoBox, sep, scroll, backLink);
    }

    // ─── SHOW / HIDE PASSWORD HELPER ─────────────────────────────────────────

    /**
     * Wraps a PasswordField in an HBox with an eye-icon toggle button.
     * Clicking the button reveals the password as plain text (TextField overlay)
     * and clicking again hides it back.
     */
    private HBox buildPasswordRow(PasswordField passwordField) {
        // Visible-text field shown when "show" is active
        TextField visibleField = new TextField();
        visibleField.setPromptText(passwordField.getPromptText());
        styleTextField(visibleField);
        visibleField.setManaged(false);
        visibleField.setVisible(false);

        // Keep both fields in sync
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
            -fx-font-size: 14px;
            -fx-padding: 6 10;
            -fx-background-radius: 6;
            -fx-cursor: hand;
            -fx-min-width: 36px;
        """);

        final boolean[] showing = {false};

        eyeBtn.setOnAction(e -> {
            showing[0] = !showing[0];
            if (showing[0]) {
                // Show password
                visibleField.setText(passwordField.getText());
                passwordField.setManaged(false);
                passwordField.setVisible(false);
                visibleField.setManaged(true);
                visibleField.setVisible(true);
                eyeBtn.setText("🙈");
                eyeBtn.setStyle(eyeBtn.getStyle().replace("#334155", "#38bdf8")
                                                  .replace("#94a3b8", "#0f172a"));
            } else {
                // Hide password
                passwordField.setText(visibleField.getText());
                visibleField.setManaged(false);
                visibleField.setVisible(false);
                passwordField.setManaged(true);
                passwordField.setVisible(true);
                eyeBtn.setText("👁");
                eyeBtn.setStyle(eyeBtn.getStyle().replace("#38bdf8", "#334155")
                                                  .replace("#0f172a", "#94a3b8"));
            }
        });

        // Stack both fields in a StackPane so they occupy the same space
        StackPane fieldStack = new StackPane(passwordField, visibleField);
        HBox.setHgrow(fieldStack, Priority.ALWAYS);

        HBox row = new HBox(8, fieldStack, eyeBtn);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    // ─── LOGIN LOGIC ─────────────────────────────────────────────────────────

    private void doLogin(String username, String password) {
        if (errorLabel != null) errorLabel.setText("");
        try {
            authService.login(username, password);
            onLoginSuccess.run();
        } catch (Exception e) {
            if (errorLabel != null) {
                errorLabel.setStyle("-fx-text-fill: #f87171; -fx-font-size: 12px;");
                errorLabel.setText("⚠ " + e.getMessage());
                FadeTransition ft = new FadeTransition(Duration.millis(100), errorLabel);
                ft.setFromValue(0); ft.setToValue(1); ft.play();
            }
        }
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────

    private VBox buildLogo(String tagline) {
        VBox box = new VBox(4);
        box.setAlignment(Pos.CENTER);
        Label logo = new Label("✈ SkyBook");
        logo.setStyle("-fx-font-size: 30px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");
        Label tag = new Label(tagline);
        tag.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");
        box.getChildren().addAll(logo, tag);
        return box;
    }

    private Separator buildSep() {
        Separator s = new Separator();
        s.setStyle("-fx-background-color: #334155;");
        return s;
    }

    private Button primaryBtn(String text, String bg, String fg) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:" + fg + ";-fx-font-weight:bold;"
                   + "-fx-font-size:14px;-fx-padding:12 0;-fx-background-radius:8;-fx-cursor:hand;");
        return btn;
    }

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
        String base = """
            -fx-background-color: #0f172a;
            -fx-text-fill: #f1f5f9;
            -fx-prompt-text-fill: #475569;
            -fx-border-color: #334155;
            -fx-border-radius: 6;
            -fx-background-radius: 6;
            -fx-padding: 10;
            -fx-font-size: 13px;
        """;
        String focused = base.replace("#334155", "#38bdf8");
        tf.setStyle(base);
        tf.focusedProperty().addListener((obs, old, f) -> tf.setStyle(f ? focused : base));
    }

    private Pane buildBackground() {
        Pane bg = new Pane();
        javafx.scene.shape.Circle c1 = new javafx.scene.shape.Circle(200);
        c1.setFill(Color.web("#38bdf820")); c1.setLayoutX(100); c1.setLayoutY(100);
        javafx.scene.shape.Circle c2 = new javafx.scene.shape.Circle(150);
        c2.setFill(Color.web("#a78bfa15")); c2.setLayoutX(900); c2.setLayoutY(600);
        bg.getChildren().addAll(c1, c2);
        return bg;
    }
}