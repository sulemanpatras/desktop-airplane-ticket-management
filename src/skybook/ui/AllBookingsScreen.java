package skybook.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import skybook.models.Flight;
import skybook.models.Ticket;
import skybook.services.BookingService;
import skybook.services.PdfService;

import java.util.List;

/**
 * All Bookings Screen (Admin).
 * Demonstrates: JavaFX TableView-style list, Labels, Buttons
 */
public class AllBookingsScreen {

    private final BookingService bookingService;

    public AllBookingsScreen(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    public VBox getView() {
        VBox view = new VBox(18);
        view.setPadding(new Insets(28));
        view.setStyle("-fx-background-color: #0f172a;");

        // Header
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Admin · All Bookings");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #f1f5f9;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button reportBtn = new Button("📄 Generate Report");
        reportBtn.setStyle("""
            -fx-background-color: #a78bfa;
            -fx-text-fill: white;
            -fx-font-weight: bold;
            -fx-padding: 9 18;
            -fx-background-radius: 7;
            -fx-cursor: hand;
        """);
        reportBtn.setOnAction(e -> generateReport());

        header.getChildren().addAll(title, sp, reportBtn);

        // Table header
        HBox tableHeader = buildRow("TICKET", "PASSENGER", "FLIGHT", "SEAT", "PRICE", "STATUS", true);

        List<Ticket> tickets = bookingService.getAllTickets();
        VBox rows = new VBox(4);
        rows.getChildren().add(tableHeader);

        for (Ticket t : tickets) {
            String statusColor = t.isConfirmed() ? "#34d399" : "#f87171";
            HBox row = buildRow(
                t.getId(),
                t.getPassengerName(),
                t.getFlightId(),
                t.getSeatNumber(),
                String.format("$%.0f", t.getPricePaid()),
                t.getStatus().toString(),
                false
            );
            // Color-code status cell
            if (!rows.getChildren().isEmpty()) {
                rows.getChildren().add(row);
            }
        }

        // Re-render properly
        rows.getChildren().clear();
        rows.getChildren().add(tableHeader);
        for (Ticket t : tickets) {
            rows.getChildren().add(buildTicketRow(t));
        }

        if (tickets.isEmpty()) {
            Label empty = new Label("No bookings yet.");
            empty.setStyle("-fx-text-fill: #475569; -fx-font-size: 14px; -fx-padding: 20;");
            rows.getChildren().add(empty);
        }

        ScrollPane scroll = new ScrollPane(rows);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: #0f172a;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        view.getChildren().addAll(header, scroll);
        return view;
    }

    private HBox buildTicketRow(Ticket t) {
        String statusColor = t.isConfirmed() ? "#34d399" : "#f87171";
        String bg = t.isConfirmed() ? "#1e293b" : "#1a1a2e";

        HBox row = new HBox();
        row.setPadding(new Insets(12, 16, 12, 16));
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 6;");

        row.getChildren().addAll(
            cell(t.getId(), "#94a3b8", 100),
            cell(t.getPassengerName(), "#f1f5f9", 160),
            cell(t.getFlightId(), "#94a3b8", 80),
            cell(t.getSeatNumber(), "#f1f5f9", 70),
            cell(String.format("$%.0f", t.getPricePaid()), "#38bdf8", 90),
            statusBadge(t.getStatus().toString(), statusColor)
        );

        return row;
    }

    private Label cell(String text, String color, double width) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px;");
        l.setPrefWidth(width);
        return l;
    }

    private Label statusBadge(String text, String color) {
        Label l = new Label("  " + text + "  ");
        l.setStyle("-fx-background-color: " + color + "22; -fx-text-fill: " + color +
                "; -fx-font-size: 10px; -fx-background-radius: 20; -fx-padding: 3 8;");
        return l;
    }

    private HBox buildRow(String c1, String c2, String c3, String c4, String c5, String c6, boolean isHeader) {
        HBox row = new HBox();
        row.setPadding(new Insets(10, 16, 10, 16));
        row.setAlignment(Pos.CENTER_LEFT);
        String style = isHeader
            ? "-fx-background-color: #334155; -fx-background-radius: 6;"
            : "-fx-background-color: #1e293b; -fx-background-radius: 6;";
        row.setStyle(style);

        String color = isHeader ? "#94a3b8" : "#f1f5f9";
        row.getChildren().addAll(
            cell(c1, color, 100), cell(c2, color, 160),
            cell(c3, color, 80),  cell(c4, color, 70),
            cell(c5, color, 90),  cell(c6, color, 90)
        );
        return row;
    }

    private void generateReport() {
        PdfService pdfService = new PdfService();
        List<Ticket> tickets = bookingService.getAllTickets();
        List<Flight> flights  = bookingService.getAllFlights();
        String path = pdfService.generateBookingReport(tickets, flights);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Report Generated");
        alert.setHeaderText("Booking report saved!");
        alert.setContentText("File saved to:\n" + path);
        alert.showAndWait();
    }
}
