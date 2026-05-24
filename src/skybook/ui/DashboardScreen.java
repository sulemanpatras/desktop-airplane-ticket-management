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
 * Dashboard Screen.
 * Demonstrates: JavaFX Labels, Charts (PieChart, BarChart), VBox/HBox/GridPane layout
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

        // Header
        Label title = new Label("Dashboard");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #f1f5f9;");
        Label subtitle = new Label("Overview of your airline system");
        subtitle.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");

        // Stats cards
        List<Flight> flights = bookingService.getAllFlights();
        List<Ticket> tickets = bookingService.getAllTickets();

        long confirmed = tickets.stream().filter(Ticket::isConfirmed).count();
        int totalSeats = flights.stream().mapToInt(Flight::getTotalSeats).sum();
        int availSeats = flights.stream().mapToInt(Flight::getSeatsAvailable).sum();
        int booked = totalSeats - availSeats;
        double revenue = bookingService.getTotalRevenue();

        HBox cards = new HBox(14);
        cards.getChildren().addAll(
            statCard("Total Flights", String.valueOf(flights.size()), "#38bdf8"),
            statCard("Active Bookings", String.valueOf(confirmed), "#34d399"),
            statCard("Seats Booked", booked + "/" + totalSeats, "#f59e0b"),
            statCard("Revenue", String.format("$%,.0f", revenue), "#a78bfa")
        );

        // Charts row
        HBox chartsRow = new HBox(20);
        chartsRow.getChildren().addAll(buildPieChart(tickets), buildBarChart(flights));
        HBox.setHgrow(chartsRow.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(chartsRow.getChildren().get(1), Priority.ALWAYS);

        view.getChildren().addAll(title, subtitle, cards, chartsRow);
        return view;
    }

    private VBox statCard(String label, String value, String accent) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(18));
        card.setPrefWidth(180);
        card.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 10; -fx-border-radius: 10;");

        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

        Label val = new Label(value);
        val.setStyle("-fx-text-fill: " + accent + "; -fx-font-size: 24px; -fx-font-weight: bold;");

        card.getChildren().addAll(lbl, val);
        return card;
    }

    // ─── PIE CHART: Ticket Status ────────────────────────────────────────────────

    private PieChart buildPieChart(List<Ticket> tickets) {
        long confirmed = tickets.stream().filter(Ticket::isConfirmed).count();
        long cancelled = tickets.size() - confirmed;

        PieChart.Data confirmedSlice = new PieChart.Data("Confirmed (" + confirmed + ")", confirmed);
        PieChart.Data cancelledSlice = new PieChart.Data("Cancelled (" + cancelled + ")", cancelled);

        PieChart chart = new PieChart();
        chart.getData().addAll(confirmedSlice, cancelledSlice);
        chart.setTitle("Ticket Status");
        chart.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #f1f5f9;");
        chart.setLegendVisible(true);
        chart.setPrefSize(380, 280);

        // Style chart title
        chart.lookup(".chart-title").setStyle("-fx-text-fill: #f1f5f9; -fx-font-size: 14px;");

        return chart;
    }

    // ─── BAR CHART: Seats per Flight ────────────────────────────────────────────

    private BarChart<String, Number> buildBarChart(List<Flight> flights) {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Flight");
        xAxis.setTickLabelFill(Color.web("#94a3b8"));

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Seats");
        yAxis.setTickLabelFill(Color.web("#94a3b8"));

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("Seats per Flight");
        chart.setStyle("-fx-background-color: #1e293b;");
        chart.setPrefSize(420, 280);
        chart.setLegendVisible(true);

        XYChart.Series<String, Number> availSeries = new XYChart.Series<>();
        availSeries.setName("Available");

        XYChart.Series<String, Number> bookedSeries = new XYChart.Series<>();
        bookedSeries.setName("Booked");

        for (Flight f : flights) {
            availSeries.getData().add(new XYChart.Data<>(f.getId(), f.getSeatsAvailable()));
            bookedSeries.getData().add(new XYChart.Data<>(
                    f.getId(), f.getTotalSeats() - f.getSeatsAvailable()));
        }

        chart.getData().addAll(availSeries, bookedSeries);
        chart.lookup(".chart-title").setStyle("-fx-text-fill: #f1f5f9; -fx-font-size: 14px;");

        return chart;
    }
}
