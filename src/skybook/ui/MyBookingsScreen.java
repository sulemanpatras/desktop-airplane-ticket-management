package skybook.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import skybook.exceptions.InvalidBookingException;
import skybook.models.Ticket;
import skybook.models.User;
import skybook.services.AuthService;
import skybook.services.BookingService;

import java.util.List;
import java.util.stream.Collectors;

public class MyBookingsScreen {

    private final BookingService bookingService;
    private final AuthService    authService;
    private VBox listBox;

    private Button searchBtn;
    private Button showAllBtn;

    public MyBookingsScreen(BookingService bookingService, AuthService authService) {
        this.bookingService = bookingService;
        this.authService    = authService;
    }

    public VBox getView() {
        VBox view = new VBox(20);
        view.setPadding(new Insets(28));
        view.setStyle("-fx-background-color: #0f172a;");

        User me = authService.getCurrentUser();

        Label title = new Label("My Bookings");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #f1f5f9;");
        Label subtitle = new Label("Tickets booked under: " + me.getEmail());
        subtitle.setStyle("-fx-text-fill: #94a3b8;");

        HBox lookup = new HBox(12);
        lookup.setAlignment(Pos.CENTER_LEFT);
        lookup.setPadding(new Insets(14));
        lookup.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 8;");

        Label lbl = new Label("Search by Ticket ID:");
        lbl.setStyle("-fx-text-fill: #94a3b8;");

        TextField ticketField = new TextField();
        ticketField.setPromptText("e.g. TK10001");
        ticketField.setPrefWidth(200);
        styleField(ticketField);

        searchBtn  = new Button("Search");
        searchBtn.setStyle("""
            -fx-background-color: #38bdf8;
            -fx-text-fill: #0f172a;
            -fx-font-weight: bold;
            -fx-padding: 8 18;
            -fx-background-radius: 6;
            -fx-cursor: hand;
        """);

        showAllBtn = new Button("Show All");
        showAllBtn.setStyle("""
            -fx-background-color: #334155;
            -fx-text-fill: #cbd5e1;
            -fx-font-weight: bold;
            -fx-padding: 8 14;
            -fx-background-radius: 6;
            -fx-cursor: hand;
        """);

        // Initially only Search is visible
        showAllBtn.setVisible(false);
        showAllBtn.setManaged(false);

        lookup.getChildren().addAll(lbl, ticketField, searchBtn, showAllBtn);

        listBox = new VBox(12);

        // Load all tickets on open — no filter
        loadMyTickets(me.getEmail(), "", false);

        searchBtn.setOnAction(e -> {
            String filter = ticketField.getText().trim();
            if (filter.isEmpty()) {
                loadMyTickets(me.getEmail(), "", false);
            } else {
                loadMyTickets(me.getEmail(), filter, true);
            }
        });

        showAllBtn.setOnAction(e -> {
            ticketField.clear();
            loadMyTickets(me.getEmail(), "", false);
        });

        ticketField.setOnAction(e -> {
            String filter = ticketField.getText().trim();
            if (filter.isEmpty()) {
                loadMyTickets(me.getEmail(), "", false);
            } else {
                loadMyTickets(me.getEmail(), filter, true);
            }
        });

        ScrollPane scroll = new ScrollPane(listBox);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: #0f172a;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        view.getChildren().addAll(title, subtitle, lookup, scroll);
        return view;
    }

    /**
     * filtered=true  → results from a search query; hide Search, show Show All
     * filtered=false → showing all tickets; show Search, hide Show All
     *
     * Special case: if filtered=true but NO results found, reset to Search visible
     * (nothing to "show all" for).
     */
    private void loadMyTickets(String userEmail, String ticketIdFilter, boolean filtered) {
        listBox.getChildren().clear();

        List<Ticket> myTickets = bookingService.getTicketsForPassenger(userEmail);

        if (ticketIdFilter != null && !ticketIdFilter.isEmpty()) {
            final String f = ticketIdFilter.toUpperCase();
            myTickets = myTickets.stream()
                    .filter(t -> t.getId().toUpperCase().contains(f))
                    .collect(Collectors.toList());
        }

        if (myTickets.isEmpty()) {
            // No results — reset button state to Search visible
            setFilteredMode(false);

            Label empty = new Label(ticketIdFilter.isEmpty()
                    ? "🎫  You have no bookings yet."
                    : "🎫  No ticket found matching \"" + ticketIdFilter + "\".");
            empty.setStyle("-fx-text-fill: #475569; -fx-font-size: 16px;");
            empty.setPadding(new Insets(40, 0, 0, 0));
            listBox.getChildren().add(empty);
            return;
        }

        // Results exist — apply button visibility based on filter state
        setFilteredMode(filtered);

        for (Ticket ticket : myTickets) {
            listBox.getChildren().add(buildTicketCard(ticket, userEmail));
        }
    }

    /**
     * filtered=true  → hide Search button, show Show All button
     * filtered=false → show Search button, hide Show All button
     */
    private void setFilteredMode(boolean filtered) {
        searchBtn.setVisible(!filtered);
        searchBtn.setManaged(!filtered);
        showAllBtn.setVisible(filtered);
        showAllBtn.setManaged(filtered);
    }

    private HBox buildTicketCard(Ticket ticket, String userEmail) {
        HBox card = new HBox(16);
        card.setPadding(new Insets(16));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: " + (ticket.isConfirmed() ? "#1e293b" : "#1a1a2e")
                + "; -fx-background-radius: 10;");

        VBox info = new VBox(4);

        HBox idRow = new HBox(10);
        Label idLbl = new Label(ticket.getId());
        idLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #f1f5f9; -fx-font-size: 14px;");
        String statusColor = ticket.isConfirmed() ? "#34d399" : "#f87171";
        Label statusLbl = new Label("  " + ticket.getStatus() + "  ");
        statusLbl.setStyle("-fx-background-color: " + statusColor + "22; -fx-text-fill: " + statusColor
                + "; -fx-font-size: 10px; -fx-background-radius: 20; -fx-padding: 3 6;");
        idRow.getChildren().addAll(idLbl, statusLbl);

        Label route = new Label(ticket.getFlightId() + "  ·  Seat " + ticket.getSeatNumber());
        route.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
        Label date = new Label("Booked: " + ticket.getBookedAt()
                + "  ·  PKR " + String.format("%.2f", ticket.getPricePaid()));
        date.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");

        info.getChildren().addAll(idRow, route, date);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

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
                loadMyTickets(userEmail, "", false);
            } catch (InvalidBookingException ex) {
                new Alert(Alert.AlertType.WARNING, ex.getMessage()).showAndWait();
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
            }
        });

        card.getChildren().addAll(info, spacer, cancelBtn);
        return card;
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