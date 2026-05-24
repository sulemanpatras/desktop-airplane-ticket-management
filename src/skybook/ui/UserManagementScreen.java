package skybook.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import skybook.models.User;
import skybook.services.AuthService;

import java.util.List;

/**
 * User Management Screen (ADMIN only).
 * Shows all registered users and allows deactivating them.
 */
public class UserManagementScreen {

    private final AuthService authService;
    private VBox listBox;

    public UserManagementScreen(AuthService authService) {
        this.authService = authService;
    }

    public VBox getView() {
        VBox view = new VBox(18);
        view.setPadding(new Insets(28));
        view.setStyle("-fx-background-color: #0f172a;");

        // Header
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Admin · User Management");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #f1f5f9;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label countLbl = new Label(authService.getAllUsers().size() + " users total");
        countLbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px;");

        header.getChildren().addAll(title, spacer, countLbl);

        // Table header
        HBox tableHeader = buildHeaderRow();

        listBox = new VBox(6);
        listBox.getChildren().add(tableHeader);
        refreshList();

        ScrollPane scroll = new ScrollPane(listBox);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: #0f172a;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        view.getChildren().addAll(header, scroll);
        return view;
    }

    private void refreshList() {
        // Keep header, remove rest
        if (listBox.getChildren().size() > 1)
            listBox.getChildren().remove(1, listBox.getChildren().size());

        List<User> users = authService.getAllUsers();
        for (User u : users) {
            listBox.getChildren().add(buildUserRow(u));
        }

        if (users.isEmpty()) {
            Label empty = new Label("No users found.");
            empty.setStyle("-fx-text-fill: #475569; -fx-padding: 20; -fx-font-size: 14px;");
            listBox.getChildren().add(empty);
        }
    }

    private HBox buildHeaderRow() {
        HBox row = new HBox();
        row.setPadding(new Insets(10, 16, 10, 16));
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: #334155; -fx-background-radius: 6;");

        row.getChildren().addAll(
            cell("ID",        "#94a3b8", 70),
            cell("USERNAME",  "#94a3b8", 140),
            cell("FULL NAME", "#94a3b8", 180),
            cell("EMAIL",     "#94a3b8", 200),
            cell("ROLE",      "#94a3b8", 100),
            cell("STATUS",    "#94a3b8", 90),
            new Region() {{ HBox.setHgrow(this, Priority.ALWAYS); }},
            cell("ACTIONS",   "#94a3b8", 100)
        );
        return row;
    }

    private HBox buildUserRow(User user) {
        HBox row = new HBox(8);
        row.setPadding(new Insets(12, 16, 12, 16));
        row.setAlignment(Pos.CENTER_LEFT);
        String bg = user.isActive() ? "#1e293b" : "#1a1025";
        row.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 6;");

        String roleColor = switch (user.getRole()) {
            case ADMIN -> "#a78bfa";
            case STAFF -> "#34d399";
            case PASSENGER -> "#38bdf8";
        };

        Label roleBadge = new Label("  " + user.getRole().name() + "  ");
        roleBadge.setStyle("-fx-background-color: " + roleColor + "22; -fx-text-fill: " +
                roleColor + "; -fx-font-size: 10px; -fx-background-radius: 20; -fx-padding: 3 8;");

        String statusColor = user.isActive() ? "#34d399" : "#f87171";
        String statusText  = user.isActive() ? "ACTIVE" : "INACTIVE";
        Label statusBadge = new Label("  " + statusText + "  ");
        statusBadge.setStyle("-fx-background-color: " + statusColor + "22; -fx-text-fill: " +
                statusColor + "; -fx-font-size: 10px; -fx-background-radius: 20; -fx-padding: 3 8;");

        Button deactivateBtn = new Button(user.isActive() ? "Deactivate" : "Deactivated");
        deactivateBtn.setDisable(!user.isActive());
        deactivateBtn.setStyle("-fx-background-color: #f8717122; -fx-text-fill: #f87171;"
                + "-fx-font-size: 11px; -fx-padding: 4 10; -fx-background-radius: 5; -fx-cursor: hand;");
        deactivateBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Deactivate user '" + user.getUsername() + "'? They will no longer be able to log in.",
                    ButtonType.YES, ButtonType.NO);
            confirm.setTitle("Confirm Deactivation");
            confirm.showAndWait().ifPresent(btn -> {
                if (btn == ButtonType.YES) {
                    try {
                        authService.deactivateUser(user.getId());
                        refreshList();
                    } catch (Exception ex) {
                        new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
                    }
                }
            });
        });

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        row.getChildren().addAll(
            cell(user.getId(),        "#64748b", 70),
            cell(user.getUsername(),  "#f1f5f9", 140),
            cell(user.getFullName(),  "#f1f5f9", 180),
            cell(user.getEmail(),     "#94a3b8", 200),
            roleBadge,
            new Label("  ") {{ setPrefWidth(10); }},
            statusBadge,
            sp,
            deactivateBtn
        );

        return row;
    }

    private Label cell(String text, String color, double width) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px;");
        l.setPrefWidth(width);
        return l;
    }
}