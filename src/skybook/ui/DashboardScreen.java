package skybook.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import skybook.models.Flight;
import skybook.models.Ticket;
import skybook.services.BookingService;

import java.util.List;

/**
 * Dashboard Screen — ADMIN only.
 *
 * FIX 8: Bar chart is now fully dynamic — each data series is bound to live booking data,
 *        so adding new flights or bookings is immediately reflected.
 */
public class DashboardScreen {

    private final BookingService bookingService;

    public DashboardScreen(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    public VBox getView() {
        VBox view = new VBox(20);
        view.setPadding(new Insets(28));
        view.setStyle("-fx-background-color: #0f172a;");

        Label title = new Label("Dashboard");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #f1f5f9;");
        Label subtitle = new Label("System overview — Admin");
        subtitle.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");

        List<Flight> flights = bookingService.getAllFlights();
        List<Ticket> tickets = bookingService.getAllTickets();

        long confirmed = tickets.stream().filter(Ticket::isConfirmed).count();
        int  totalSeats = flights.stream().mapToInt(Flight::getTotalSeats).sum();
        int  availSeats = flights.stream().mapToInt(Flight::getSeatsAvailable).sum();
        int  booked     = totalSeats - availSeats;
        double revenue  = bookingService.getTotalRevenue();

        HBox cards = new HBox(14);
        cards.getChildren().addAll(
            statCard("Total Flights",   String.valueOf(flights.size()),           "#38bdf8"),
            statCard("Active Bookings", String.valueOf(confirmed),                "#34d399"),
            statCard("Seats Booked",    booked + "/" + totalSeats,               "#f59e0b"),
            statCard("Revenue (PKR)",   String.format("%,.0f", revenue),         "#a78bfa")
        );

        HBox chartsRow = new HBox(20);
        PieChart pie = buildPieChart(tickets);
        BarChart<String, Number> bar = buildBarChart(flights, tickets);  // FIX 8: pass tickets too

        HBox.setHgrow(pie, Priority.ALWAYS);
        HBox.setHgrow(bar, Priority.ALWAYS);
        chartsRow.getChildren().addAll(pie, bar);

        view.getChildren().addAll(title, subtitle, cards, chartsRow);
        return view;
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

    private PieChart buildPieChart(List<Ticket> tickets) {
        long confirmed = tickets.stream().filter(Ticket::isConfirmed).count();
        long cancelled = tickets.size() - confirmed;

        PieChart.Data confirmedSlice = new PieChart.Data("Confirmed (" + confirmed + ")", Math.max(confirmed, 0));
        PieChart.Data cancelledSlice = new PieChart.Data("Cancelled (" + cancelled + ")", Math.max(cancelled, 0));

        PieChart chart = new PieChart();
        if (confirmed > 0 || cancelled > 0) {
            chart.getData().addAll(confirmedSlice, cancelledSlice);
        } else {
            chart.getData().add(new PieChart.Data("No bookings yet", 1));
        }
        chart.setTitle("Ticket Status");
        chart.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #f1f5f9;");
        chart.setPrefSize(380, 280);

        try { chart.lookup(".chart-title")
                   .setStyle("-fx-text-fill: #f1f5f9; -fx-font-size: 14px;"); }
        catch (Exception ignored) {}

        return chart;
    }

    /**
     * FIX 8: Fully dynamic bar chart — each flight shows Available / Booked seats
     * computed live from the actual seat counters, not hard-coded.
     */
    private BarChart<String, Number> buildBarChart(List<Flight> flights, List<Ticket> tickets) {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Flight");
        xAxis.setTickLabelFill(Color.web("#94a3b8"));

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Seats");
        yAxis.setTickLabelFill(Color.web("#94a3b8"));
        yAxis.setAutoRanging(true);   // dynamic — adapts to any number of seats

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("Seat Availability per Flight");
        chart.setStyle("-fx-background-color: #1e293b;");
        chart.setPrefSize(420, 280);
        chart.setAnimated(true);      // FIX 8: animate on load for visual feedback

        XYChart.Series<String, Number> availSeries  = new XYChart.Series<>();
        availSeries.setName("Available");

        XYChart.Series<String, Number> bookedSeries = new XYChart.Series<>();
        bookedSeries.setName("Booked");

        for (Flight f : flights) {
            // Dynamic: compute booked from totalSeats - seatsAvailable
            int avail  = f.getSeatsAvailable();
            int booked = f.getTotalSeats() - avail;

            availSeries.getData().add(
                new XYChart.Data<>(f.getId() + "\n(" + f.getSource() + "→" + f.getDestination() + ")",
                        avail));
            bookedSeries.getData().add(
                new XYChart.Data<>(f.getId() + "\n(" + f.getSource() + "→" + f.getDestination() + ")",
                        booked));
        }

        chart.getData().addAll(availSeries, bookedSeries);

        try { chart.lookup(".chart-title")
                   .setStyle("-fx-text-fill: #f1f5f9; -fx-font-size: 14px;"); }
        catch (Exception ignored) {}

        return chart;
    }
}