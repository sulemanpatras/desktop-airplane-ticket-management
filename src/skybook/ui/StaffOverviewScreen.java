package skybook.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import skybook.models.Flight;
import skybook.services.BookingService;

import java.util.List;

/**
 * Staff Overview Screen — replaces the admin Dashboard for STAFF role.
 *
 * FIX 8: Staff should NOT see the full admin dashboard with revenue stats.
 *        This screen shows the flight list and seat availability only.
 */
public class StaffOverviewScreen {

    private final BookingService bookingService;

    public StaffOverviewScreen(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    public VBox getView() {
        VBox view = new VBox(20);
        view.setPadding(new Insets(28));
        view.setStyle("-fx-background-color: #0f172a;");

        Label title = new Label("Staff Overview");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #f1f5f9;");
        Label subtitle = new Label("Flight status and seat availability");
        subtitle.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");

        List<Flight> flights = bookingService.getAllFlights();

        // Summary cards
        int totalFlights = flights.size();
        long scheduled   = flights.stream().filter(f -> "SCHEDULED".equals(f.getStatus())).count();
        long delayed     = flights.stream().filter(f -> "DELAYED".equals(f.getStatus())).count();
        int totalSeats   = flights.stream().mapToInt(Flight::getTotalSeats).sum();
        int availSeats   = flights.stream().mapToInt(Flight::getSeatsAvailable).sum();

        HBox cards = new HBox(14);
        cards.getChildren().addAll(
            statCard("Total Flights", String.valueOf(totalFlights), "#38bdf8"),
            statCard("Scheduled",     String.valueOf(scheduled),    "#34d399"),
            statCard("Delayed",       String.valueOf(delayed),      "#f59e0b"),
            statCard("Seats Available", availSeats + "/" + totalSeats, "#a78bfa")
        );

        // Flight table header
        HBox header = buildHeaderRow();

        VBox rows = new VBox(6);
        rows.getChildren().add(header);

        for (Flight f : flights) {
            rows.getChildren().add(buildFlightRow(f));
        }

        if (flights.isEmpty()) {
            Label empty = new Label("No flights found.");
            empty.setStyle("-fx-text-fill: #475569; -fx-font-size: 14px; -fx-padding: 20;");
            rows.getChildren().add(empty);
        }

        ScrollPane scroll = new ScrollPane(rows);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: #0f172a;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        view.getChildren().addAll(title, subtitle, cards, scroll);
        return view;
    }

    private HBox buildHeaderRow() {
        HBox row = new HBox();
        row.setPadding(new Insets(10, 16, 10, 16));
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: #334155; -fx-background-radius: 6;");
        row.getChildren().addAll(
            cell("FLIGHT",     "#94a3b8", 80),
            cell("AIRLINE",    "#94a3b8", 130),
            cell("ROUTE",      "#94a3b8", 220),
            cell("DEPARTURE",  "#94a3b8", 150),
            cell("SEATS",      "#94a3b8", 90),
            cell("STATUS",     "#94a3b8", 100)
        );
        return row;
    }

    private HBox buildFlightRow(Flight f) {
        HBox row = new HBox();
        row.setPadding(new Insets(12, 16, 12, 16));
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 6;");

        String statusColor = switch (f.getStatus()) {
            case "SCHEDULED" -> "#34d399";
            case "DELAYED"   -> "#f59e0b";
            default          -> "#f87171";
        };

        // Dynamic seat bar
        int pct = f.getTotalSeats() == 0 ? 0
                : (int)(((double)(f.getTotalSeats() - f.getSeatsAvailable()) / f.getTotalSeats()) * 100);
        String seatColor = pct >= 90 ? "#ef4444" : pct >= 60 ? "#f59e0b" : "#34d399";
        Label seatLbl = new Label(f.getSeatsAvailable() + "/" + f.getTotalSeats());
        seatLbl.setStyle("-fx-text-fill: " + seatColor + "; -fx-font-size: 12px;");
        seatLbl.setPrefWidth(90);

        Label statusBadge = new Label("  " + f.getStatus() + "  ");
        statusBadge.setStyle("-fx-background-color:" + statusColor + "22;-fx-text-fill:" + statusColor
                + ";-fx-font-size:10px;-fx-background-radius:20;-fx-padding:3 8;");

        row.getChildren().addAll(
            cell(f.getId(),                        "#94a3b8", 80),
            cell(f.getAirline(),                   "#f1f5f9", 130),
            cell(f.getSource() + " → " + f.getDestination(), "#f1f5f9", 220),
            cell(f.getDepartureTime(),             "#94a3b8", 150),
            seatLbl,
            statusBadge
        );
        return row;
    }

    private VBox statCard(String label, String value, String accent) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(18));
        card.setPrefWidth(180);
        card.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 10;");
        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
        Label val = new Label(value);
        val.setStyle("-fx-text-fill: " + accent + "; -fx-font-size: 22px; -fx-font-weight: bold;");
        card.getChildren().addAll(lbl, val);
        return card;
    }

    private Label cell(String text, String color, double width) {
        Label l = new Label(text != null ? text : "");
        l.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px;");
        l.setPrefWidth(width);
        return l;
    }
}