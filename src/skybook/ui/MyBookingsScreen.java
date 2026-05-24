package skybook.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import skybook.exceptions.InvalidBookingException;
import skybook.models.Flight;
import skybook.models.Ticket;
import skybook.services.BookingService;

import java.util.List;

/**
 * My Bookings Screen – lets a passenger look up their bookings by email.
 * Demonstrates: JavaFX TextFields, Buttons, Labels, VBox layout
 */
public class MyBookingsScreen {

    private final BookingService bookingService;
    private VBox listBox;

    public MyBookingsScreen(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    public VBox getView() {
        VBox view = new VBox(20);
        view.setPadding(new Insets(28));
        view.setStyle("-fx-background-color: #0f172a;");

        Label title = new Label("My Bookings");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #f1f5f9;");
        Label subtitle = new Label("View and manage your tickets");
        subtitle.setStyle("-fx-text-fill: #94a3b8;");

        // Email lookup
        HBox lookup = new HBox(12);
        lookup.setAlignment(Pos.CENTER_LEFT);
        lookup.setPadding(new Insets(14));
        lookup.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 8;");

        Label lbl = new Label("Your Email:");
        lbl.setStyle("-fx-text-fill: #94a3b8;");

        TextField emailField = new TextField();
        emailField.setPromptText("e.g. ali@example.com");
        emailField.setPrefWidth(260);

        Button findBtn = new Button("Find My Tickets");
        findBtn.setStyle("""
            -fx-background-color: #38bdf8;
            -fx-text-fill: #0f172a;
            -fx-font-weight: bold;
            -fx-padding: 8 18;
            -fx-background-radius: 6;
            -fx-cursor: hand;
        """);

        lookup.getChildren().addAll(lbl, emailField, findBtn);

        listBox = new VBox(12);

        findBtn.setOnAction(e -> loadTickets(emailField.getText().trim()));

        // Default: show all
        loadTickets("");

        ScrollPane scroll = new ScrollPane(listBox);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: #0f172a;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        view.getChildren().addAll(title, subtitle, lookup, scroll);
        return view;
    }

    private void loadTickets(String email) {
        listBox.getChildren().clear();
        List<Ticket> tickets = email.isEmpty()
                ? bookingService.getAllTickets()
                : bookingService.getTicketsForPassenger(email);

        if (tickets.isEmpty()) {
            Label empty = new Label("🎫  No bookings found.");
            empty.setStyle("-fx-text-fill: #475569; -fx-font-size: 16px;");
            empty.setPadding(new Insets(40, 0, 0, 0));
            listBox.getChildren().add(empty);
            return;
        }

        for (Ticket ticket : tickets) {
            listBox.getChildren().add(buildTicketCard(ticket));
        }
    }

    private HBox buildTicketCard(Ticket ticket) {
        HBox card = new HBox(16);
        card.setPadding(new Insets(16));
        card.setAlignment(Pos.CENTER_LEFT);
        String bg = ticket.isConfirmed() ? "#1e293b" : "#1a1a2e";
        card.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 10;");

        // Left info block
        VBox info = new VBox(4);

        HBox idRow = new HBox(10);
        Label idLbl = new Label(ticket.getId());
        idLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #f1f5f9; -fx-font-size: 14px;");

        String statusColor = ticket.isConfirmed() ? "#34d399" : "#f87171";
        Label statusLbl = new Label("  " + ticket.getStatus() + "  ");
        statusLbl.setStyle("-fx-background-color: " + statusColor + "22; -fx-text-fill: " +
                statusColor + "; -fx-font-size: 10px; -fx-background-radius: 20; -fx-padding: 3 6;");
        idRow.getChildren().addAll(idLbl, statusLbl);

        Label route = new Label(ticket.getFlightId() + "  ·  Seat " + ticket.getSeatNumber());
        route.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

        Label date = new Label("Booked: " + ticket.getBookedAt() + "  ·  $" +
                String.format("%.2f", ticket.getPricePaid()));
        date.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");

        info.getChildren().addAll(idRow, route, date);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Cancel button
        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("""
            -fx-background-color: transparent;
            -fx-text-fill: #f87171;
            -fx-border-color: #f8717166;
            -fx-border-radius: 5;
            -fx-padding: 6 14;
            -fx-cursor: hand;
        """);
        cancelBtn.setVisible(ticket.isConfirmed());
        cancelBtn.setOnAction(e -> {
            try {
                bookingService.cancelTicket(ticket.getId());
                loadTickets(""); // Refresh
            } catch (InvalidBookingException ex) {
                new Alert(Alert.AlertType.WARNING, ex.getMessage()).showAndWait();
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
            }
        });

        card.getChildren().addAll(info, spacer, cancelBtn);
        return card;
    }
}
