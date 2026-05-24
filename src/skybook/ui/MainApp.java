package skybook.ui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.Stage;
import skybook.services.BookingService;

/**
 * SkyBook JavaFX Application Entry Point.
 * Demonstrates: JavaFX GUI with TextFields, Buttons, Labels, ComboBoxes, VBox/HBox/GridPane
 */
public class MainApp extends Application {

    private BookingService bookingService;
    private Stage primaryStage;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        this.bookingService = new BookingService();

        stage.setTitle("SkyBook – Airline Ticket Management System");
        stage.setMinWidth(900);
        stage.setMinHeight(650);

        showMainScreen();
        stage.show();
    }

    // ─── MAIN SCREEN ────────────────────────────────────────────────────────────

    private void showMainScreen() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #0f172a;");

        // Sidebar
        VBox sidebar = buildSidebar();
        root.setLeft(sidebar);

        // Default: show dashboard
        root.setCenter(new DashboardScreen(bookingService).getView());

        // Sidebar nav
        for (javafx.scene.Node node : sidebar.getChildren()) {
            if (node instanceof Button btn) {
                btn.setOnAction(e -> {
                    String text = btn.getText();
                    switch (text) {
                        case "📊  Dashboard" ->
                                root.setCenter(new DashboardScreen(bookingService).getView());
                        case "🔍  Search Flights" ->
                                root.setCenter(new SearchFlightsScreen(bookingService, primaryStage).getView());
                        case "🎫  My Bookings" ->
                                root.setCenter(new MyBookingsScreen(bookingService).getView());
                        case "✈  Manage Flights" ->
                                root.setCenter(new ManageFlightsScreen(bookingService).getView());
                        case "👥  All Bookings" ->
                                root.setCenter(new AllBookingsScreen(bookingService).getView());
                    }
                });
            }
        }

        Scene scene = new Scene(root, 1000, 680);
        primaryStage.setScene(scene);
    }

    private VBox buildSidebar() {
        VBox sidebar = new VBox(8);
        sidebar.setPadding(new Insets(20, 12, 20, 12));
        sidebar.setPrefWidth(200);
        sidebar.setStyle("-fx-background-color: #1e293b;");

        // Logo
        Label logo = new Label("✈ SkyBook");
        logo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");
        logo.setPadding(new Insets(0, 0, 16, 0));

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #334155;");

        Label passengerLabel = new Label("PASSENGER");
        passengerLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748b; -fx-font-weight: bold;");
        passengerLabel.setPadding(new Insets(12, 0, 4, 0));

        Button btnDash   = sidebarBtn("📊  Dashboard");
        Button btnSearch = sidebarBtn("🔍  Search Flights");
        Button btnMine   = sidebarBtn("🎫  My Bookings");

        Label adminLabel = new Label("ADMIN");
        adminLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748b; -fx-font-weight: bold;");
        adminLabel.setPadding(new Insets(12, 0, 4, 0));

        Button btnManage = sidebarBtn("✈  Manage Flights");
        Button btnAll    = sidebarBtn("👥  All Bookings");

        sidebar.getChildren().addAll(
                logo, sep,
                passengerLabel, btnDash, btnSearch, btnMine,
                adminLabel, btnManage, btnAll
        );

        return sidebar;
    }

    private Button sidebarBtn(String text) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setStyle("""
            -fx-background-color: transparent;
            -fx-text-fill: #cbd5e1;
            -fx-font-size: 13px;
            -fx-padding: 8 12;
            -fx-cursor: hand;
            -fx-background-radius: 6;
        """);
        btn.setOnMouseEntered(e -> btn.setStyle("""
            -fx-background-color: #334155;
            -fx-text-fill: #f1f5f9;
            -fx-font-size: 13px;
            -fx-padding: 8 12;
            -fx-cursor: hand;
            -fx-background-radius: 6;
        """));
        btn.setOnMouseExited(e -> btn.setStyle("""
            -fx-background-color: transparent;
            -fx-text-fill: #cbd5e1;
            -fx-font-size: 13px;
            -fx-padding: 8 12;
            -fx-cursor: hand;
            -fx-background-radius: 6;
        """));
        return btn;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
