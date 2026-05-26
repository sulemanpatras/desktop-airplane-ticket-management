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
 * SkyBook JavaFX Entry Point.
 *
 * FIX 2: Admin/Staff do NOT see "My Bookings" — they manage flights, not book them.
 * FIX 8: Passengers and Staff see a role-appropriate home view (not the admin dashboard).
 *         Staff see a simplified overview; Passengers go straight to Search Flights.
 */
public class MainApp extends Application {

    private BookingService bookingService;
    private AuthService    authService;
    private Stage          primaryStage;

    @Override
    public void start(Stage stage) {
        this.primaryStage   = stage;
        this.bookingService = new BookingService();
        this.authService    = AuthService.getInstance();

        stage.setTitle("SkyBook – Airline Ticket Management System");
        stage.setMinWidth(960);
        stage.setMinHeight(660);

        // Load custom taskbar and window icon
        try {
            stage.getIcons().add(new javafx.scene.image.Image(
                getClass().getResourceAsStream("/skybook/assets/images/airplane.png")
            ));
        } catch (Exception e) {
            System.err.println("[MainApp] Could not load app icon: " + e.getMessage());
        }

        showLoginScreen();
        stage.show();
    }

    // ─── SCREENS ─────────────────────────────────────────────────────────────────

    private void showLoginScreen() {
        LoginScreen loginScreen = new LoginScreen(authService, this::showMainScreen);
        primaryStage.setScene(new Scene(loginScreen.getView(), 1000, 680));
    }

    private void showMainScreen() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #0f172a;");
        root.setLeft(buildSidebar(root));

        User user = authService.getCurrentUser();
        root.setCenter(defaultView(user));

        primaryStage.setScene(new Scene(root, 1000, 680));
    }

    /**
     * FIX 8: Role-appropriate landing view.
     * ADMIN  → full dashboard (charts + stats).
     * STAFF  → staff overview (flights list, no booking stats).
     * PASSENGER → search flights immediately.
     */
    private javafx.scene.Node defaultView(User user) {
        return switch (user.getRole()) {
            case ADMIN     -> new DashboardScreen(bookingService).getView();
            case STAFF     -> new StaffOverviewScreen(bookingService).getView();   // FIX 8
            case PASSENGER -> new SearchFlightsScreen(bookingService, primaryStage).getView();
        };
    }

    // ─── SIDEBAR (FIX 2) ─────────────────────────────────────────────────────────

    private VBox buildSidebar(BorderPane root) {
        VBox sidebar = new VBox(8);
        sidebar.setPadding(new Insets(20, 12, 20, 12));
        sidebar.setPrefWidth(210);
        sidebar.setStyle("-fx-background-color: #1e293b;");

        Label logo = new Label("✈ SkyBook");
        logo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");
        logo.setPadding(new Insets(0, 0, 8, 0));

        User user = authService.getCurrentUser();
        VBox userBox = new VBox(2);
        userBox.setPadding(new Insets(0, 0, 10, 0));
        Label userName = new Label(user.getFullName());
        userName.setStyle("-fx-text-fill: #f1f5f9; -fx-font-size: 13px; -fx-font-weight: bold;");
        String roleColor = switch (user.getRole()) {
            case ADMIN -> "#a78bfa"; case STAFF -> "#34d399"; case PASSENGER -> "#38bdf8";
        };
        Label userRole = new Label(user.getRole().name());
        userRole.setStyle("-fx-text-fill: " + roleColor + "; -fx-font-size: 10px; -fx-font-weight: bold;");
        userBox.getChildren().addAll(userName, userRole);

        sidebar.getChildren().addAll(logo, userBox, buildSep());

        switch (user.getRole()) {

            case PASSENGER -> {
                // FIX 2: Passengers see Search + My Bookings + Profile
                sidebar.getChildren().addAll(
                    sectionLabel("PASSENGER"),
                    navBtn("🔍  Search Flights", root,
                        () -> root.setCenter(new SearchFlightsScreen(bookingService, primaryStage).getView())),
                    navBtn("🎫  My Bookings",   root,
                        () -> root.setCenter(new MyBookingsScreen(bookingService, authService).getView())),
                    navBtn("👤  My Profile",    root,
                        () -> root.setCenter(new ProfileScreen(authService).getView()))
                );
            }

            case STAFF -> {
                // FIX 2: Staff do NOT see My Bookings — they manage flights
                // FIX 8: Staff default view is StaffOverviewScreen
                sidebar.getChildren().addAll(
                    sectionLabel("STAFF"),
                    navBtn("📋  Overview",       root,
                        () -> root.setCenter(new StaffOverviewScreen(bookingService).getView())),
                    navBtn("🔍  Search Flights", root,
                        () -> root.setCenter(new SearchFlightsScreen(bookingService, primaryStage).getView())),
                    navBtn("✈  Manage Flights",  root,
                        () -> root.setCenter(new ManageFlightsScreen(bookingService).getView())),
                    navBtn("👤  My Profile",     root,
                        () -> root.setCenter(new ProfileScreen(authService).getView()))
                );
            }

            case ADMIN -> {
                // FIX 2: Admin does NOT see My Bookings
                sidebar.getChildren().addAll(
                    sectionLabel("ADMIN"),
                    navBtn("📊  Dashboard",      root,
                        () -> root.setCenter(new DashboardScreen(bookingService).getView())),
                    navBtn("🔍  Search Flights", root,
                        () -> root.setCenter(new SearchFlightsScreen(bookingService, primaryStage).getView())),
                    navBtn("✈  Manage Flights",  root,
                        () -> root.setCenter(new ManageFlightsScreen(bookingService).getView())),
                    navBtn("👥  All Bookings",   root,
                        () -> root.setCenter(new AllBookingsScreen(bookingService, primaryStage).getView())),
                    navBtn("👤  Users",          root,
                        () -> root.setCenter(new UserManagementScreen(authService).getView())),
                    navBtn("🔑  My Profile",     root,
                        () -> root.setCenter(new ProfileScreen(authService).getView()))
                );
            }
        }

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

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
        logoutBtn.setOnAction(e -> { authService.logout(); showLoginScreen(); });

        sidebar.getChildren().addAll(spacer, buildSep(), logoutBtn);
        return sidebar;
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────────

    private Separator buildSep() {
        Separator s = new Separator();
        s.setStyle("-fx-background-color: #334155;");
        return s;
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
        String base  = "-fx-background-color:transparent;-fx-text-fill:#cbd5e1;-fx-font-size:13px;"
                     + "-fx-padding:8 12;-fx-cursor:hand;-fx-background-radius:6;";
        String hover = "-fx-background-color:#334155;-fx-text-fill:#f1f5f9;-fx-font-size:13px;"
                     + "-fx-padding:8 12;-fx-cursor:hand;-fx-background-radius:6;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(base));
        btn.setOnAction(e -> action.run());
        return btn;
    }

    public static void main(String[] args) { launch(args); }
}