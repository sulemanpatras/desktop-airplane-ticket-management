package skybook.ui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import skybook.models.User;
import skybook.services.AuthService;
import skybook.services.BookingService;

/**
 * SkyBook JavaFX Application Entry Point.
 *
 * Changes:
 *  - Shows LoginScreen first; routes to main app after authentication.
 *  - Sidebar nav is role-based (ADMIN sees all, STAFF sees admin tabs, PASSENGER sees passenger tabs).
 *  - Logout button in sidebar.
 *
 * Demonstrates: JavaFX scenes, role-based UI, service injection
 */
public class MainApp extends Application {

    private BookingService bookingService;
    private AuthService authService;
    private Stage primaryStage;
    private Scene loginScene;
    private Scene mainScene;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        this.bookingService = new BookingService();
        this.authService = AuthService.getInstance();

        stage.setTitle("SkyBook – Airline Ticket Management System");
        stage.setMinWidth(900);
        stage.setMinHeight(650);

        showLoginScreen();
        stage.show();
    }

    // ─── LOGIN ───────────────────────────────────────────────────────────────

    private void showLoginScreen() {
        LoginScreen loginScreen = new LoginScreen(authService, this::showMainScreen);
        loginScene = new Scene(loginScreen.getView(), 1000, 680);
        primaryStage.setScene(loginScene);
    }

    // ─── MAIN SCREEN (post-login) ────────────────────────────────────────────

    private void showMainScreen() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #0f172a;");

        VBox sidebar = buildSidebar(root);
        root.setLeft(sidebar);

        // Default view based on role
        User user = authService.getCurrentUser();
        if (user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.STAFF) {
            root.setCenter(new DashboardScreen(bookingService).getView());
        } else {
            root.setCenter(new SearchFlightsScreen(bookingService, primaryStage).getView());
        }

        mainScene = new Scene(root, 1000, 680);
        primaryStage.setScene(mainScene);
    }

    // ─── SIDEBAR ─────────────────────────────────────────────────────────────

    private VBox buildSidebar(BorderPane root) {
        VBox sidebar = new VBox(8);
        sidebar.setPadding(new Insets(20, 12, 20, 12));
        sidebar.setPrefWidth(210);
        sidebar.setStyle("-fx-background-color: #1e293b;");

        // Logo
        Label logo = new Label("✈ SkyBook");
        logo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");
        logo.setPadding(new Insets(0, 0, 8, 0));

        // User info
        User user = authService.getCurrentUser();
        VBox userBox = new VBox(2);
        userBox.setPadding(new Insets(0, 0, 10, 0));
        Label userName = new Label(user.getFullName());
        userName.setStyle("-fx-text-fill: #f1f5f9; -fx-font-size: 13px; -fx-font-weight: bold;");
        Label userRole = new Label(user.getRole().name());
        String roleColor = switch (user.getRole()) {
            case ADMIN -> "#a78bfa";
            case STAFF -> "#34d399";
            case PASSENGER -> "#38bdf8";
        };
        userRole.setStyle("-fx-text-fill: " + roleColor + "; -fx-font-size: 10px; -fx-font-weight: bold;");
        userBox.getChildren().addAll(userName, userRole);

        Separator sep1 = new Separator();
        sep1.setStyle("-fx-background-color: #334155;");

        // Nav items
        sidebar.getChildren().addAll(logo, userBox, sep1);

        boolean isAdminOrStaff = user.getRole() != User.Role.PASSENGER;
        boolean isAdmin        = user.getRole() == User.Role.ADMIN;

        if (!isAdmin) {
            // Passenger section header
            Label passengerLabel = sectionLabel("PASSENGER");
            sidebar.getChildren().addAll(passengerLabel,
                navBtn("📊  Dashboard",    root, () -> root.setCenter(new DashboardScreen(bookingService).getView())),
                navBtn("🔍  Search Flights", root, () -> root.setCenter(new SearchFlightsScreen(bookingService, primaryStage).getView())),
                navBtn("🎫  My Bookings",  root, () -> root.setCenter(new MyBookingsScreen(bookingService).getView()))
            );
        }

        if (isAdminOrStaff) {
            Label adminLabel = sectionLabel(isAdmin ? "ADMIN" : "STAFF");
            sidebar.getChildren().addAll(adminLabel,
                navBtn("📊  Dashboard",      root, () -> root.setCenter(new DashboardScreen(bookingService).getView())),
                navBtn("🔍  Search Flights", root, () -> root.setCenter(new SearchFlightsScreen(bookingService, primaryStage).getView())),
                navBtn("🎫  My Bookings",    root, () -> root.setCenter(new MyBookingsScreen(bookingService).getView())),
                navBtn("✈  Manage Flights",  root, () -> root.setCenter(new ManageFlightsScreen(bookingService).getView())),
                navBtn("👥  All Bookings",   root, () -> root.setCenter(new AllBookingsScreen(bookingService, primaryStage).getView()))
            );

            if (isAdmin) {
                sidebar.getChildren().add(
                    navBtn("👤  Users",        root, () -> root.setCenter(new UserManagementScreen(authService).getView()))
                );
            }
        }

        // Spacer
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Logout
        Button logoutBtn = new Button("⎋  Log Out");
        logoutBtn.setMaxWidth(Double.MAX_VALUE);
        logoutBtn.setAlignment(Pos.CENTER_LEFT);
        logoutBtn.setStyle("""
            -fx-background-color: #f8717122;
            -fx-text-fill: #f87171;
            -fx-font-size: 13px;
            -fx-padding: 8 12;
            -fx-cursor: hand;
            -fx-background-radius: 6;
        """);
        logoutBtn.setOnAction(e -> {
            authService.logout();
            showLoginScreen();
        });

        sidebar.getChildren().addAll(spacer, new Separator(), logoutBtn);
        return sidebar;
    }

    private Label sectionLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748b; -fx-font-weight: bold;");
        l.setPadding(new Insets(12, 0, 4, 0));
        return l;
    }

    private Button navBtn(String text, BorderPane root, Runnable action) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        String base = """
            -fx-background-color: transparent;
            -fx-text-fill: #cbd5e1;
            -fx-font-size: 13px;
            -fx-padding: 8 12;
            -fx-cursor: hand;
            -fx-background-radius: 6;
        """;
        String hover = """
            -fx-background-color: #334155;
            -fx-text-fill: #f1f5f9;
            -fx-font-size: 13px;
            -fx-padding: 8 12;
            -fx-cursor: hand;
            -fx-background-radius: 6;
        """;
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(base));
        btn.setOnAction(e -> action.run());
        return btn;
    }

    public static void main(String[] args) {
        launch(args);
    }
}