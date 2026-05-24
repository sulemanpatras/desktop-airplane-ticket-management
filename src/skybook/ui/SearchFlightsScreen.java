package skybook.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import skybook.exceptions.FlightNotFoundException;
import skybook.exceptions.InvalidBookingException;
import skybook.exceptions.NoSeatsAvailableException;
import skybook.models.Flight;
import skybook.services.BookingService;
import skybook.services.GoogleCalendarService;
import skybook.services.PdfService;
import skybook.models.Ticket;

import java.util.List;

/**
 * Search Flights Screen.
 *
 * Updates:
 *  - After booking, shows "📅 Add to Google Calendar" button.
 *  - PDF/ticket is saved via FileChooser dialog (user picks destination).
 */
public class SearchFlightsScreen {

    private final BookingService bookingService;
    private final Stage stage;
    private VBox resultsBox;

    private final GoogleCalendarService calendarService = new GoogleCalendarService();
    private final PdfService pdfService = new PdfService();

    public SearchFlightsScreen(BookingService bookingService, Stage stage) {
        this.bookingService = bookingService;
        this.stage = stage;
    }

    public VBox getView() {
        VBox view = new VBox(20);
        view.setPadding(new Insets(28));
        view.setStyle("-fx-background-color: #0f172a;");

        Label title = new Label("Search Flights");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #f1f5f9;");
        Label subtitle = new Label("Find and book your next adventure");
        subtitle.setStyle("-fx-text-fill: #94a3b8;");

        GridPane searchGrid = new GridPane();
        searchGrid.setHgap(12);
        searchGrid.setVgap(10);
        searchGrid.setPadding(new Insets(20));
        searchGrid.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 10;");

        TextField fromField = styledField("e.g. Karachi");
        TextField toField   = styledField("e.g. Lahore");

        Button searchBtn  = actionBtn("🔍 Search",  "#38bdf8", "#0f172a");
        Button showAllBtn = actionBtn("Show All",   "#334155", "#cbd5e1");

        searchGrid.add(styledLabel("From"), 0, 0);
        searchGrid.add(fromField, 1, 0);
        searchGrid.add(styledLabel("To"), 2, 0);
        searchGrid.add(toField, 3, 0);
        searchGrid.add(new HBox(10, searchBtn, showAllBtn), 4, 0);

        resultsBox = new VBox(12);

        searchBtn.setOnAction(e -> doSearch(fromField.getText(), toField.getText()));
        showAllBtn.setOnAction(e -> { fromField.clear(); toField.clear(); doSearch("", ""); });

        doSearch("", "");

        ScrollPane scroll = new ScrollPane(resultsBox);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: #0f172a;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        view.getChildren().addAll(title, subtitle, searchGrid, scroll);
        return view;
    }

    private void doSearch(String from, String to) {
        resultsBox.getChildren().clear();
        List<Flight> results = bookingService.searchFlights(from, to);

        if (results.isEmpty()) {
            Label empty = new Label("No flights found. Try adjusting your search.");
            empty.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14px;");
            empty.setPadding(new Insets(30));
            resultsBox.getChildren().add(empty);
            return;
        }
        for (Flight f : results) resultsBox.getChildren().add(buildFlightCard(f));
    }

    private VBox buildFlightCard(Flight flight) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(18));
        card.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 10;");

        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label airline = new Label(flight.getAirline());
        airline.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        int seats = flight.getSeatsAvailable();
        String badgeColor = seats < 10 ? "#ef4444" : seats < 30 ? "#f59e0b" : "#34d399";
        Label seatsBadge = new Label("  " + seats + " seats  ");
        seatsBadge.setStyle("-fx-background-color: " + badgeColor + "22; -fx-text-fill: " +
                badgeColor + "; -fx-font-size: 11px; -fx-background-radius: 20; -fx-padding: 3 8;");

        topRow.getChildren().addAll(airline, spacer, seatsBadge);

        HBox routeRow = new HBox(16);
        routeRow.setAlignment(Pos.CENTER);

        Label depTime = largeLabel(flight.getDepartureTime().length() > 10 ?
                flight.getDepartureTime().substring(11) : flight.getDepartureTime());
        Label depCity = smallLabel(flight.getSource());
        VBox depBox = new VBox(2, depTime, depCity);
        depBox.setAlignment(Pos.CENTER);

        Label arrow = new Label("✈ ─────");
        arrow.setStyle("-fx-text-fill: #38bdf8;");

        Label arrTime = largeLabel(flight.getArrivalTime().length() > 10 ?
                flight.getArrivalTime().substring(11) : flight.getArrivalTime());
        Label arrCity = smallLabel(flight.getDestination());
        VBox arrBox = new VBox(2, arrTime, arrCity);
        arrBox.setAlignment(Pos.CENTER);

        routeRow.getChildren().addAll(depBox, arrow, arrBox);

        HBox bottomRow = new HBox(10);
        bottomRow.setAlignment(Pos.CENTER_LEFT);

        Label date = new Label("📅 " + (flight.getDepartureTime().length() > 10 ?
                flight.getDepartureTime().substring(0, 10) : flight.getDepartureTime()));
        date.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");

        Region sp2 = new Region();
        HBox.setHgrow(sp2, Priority.ALWAYS);

        Label price = new Label(String.format("$%.0f", flight.getPrice()));
        price.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 20px; -fx-font-weight: bold;");

        Button bookBtn = actionBtn("Book Now", "#38bdf8", "#0f172a");
        bookBtn.setOnAction(e -> showBookingDialog(flight));

        bottomRow.getChildren().addAll(date, sp2, price, bookBtn);

        card.getChildren().addAll(topRow, routeRow, bottomRow);
        return card;
    }

    private void showBookingDialog(Flight flight) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Book Flight " + flight.getId());
        dialog.setHeaderText(flight.getSource() + " → " + flight.getDestination() +
                "  |  $" + String.format("%.0f", flight.getPrice()));

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(14);
        form.setPadding(new Insets(20));

        TextField nameField  = new TextField("John Doe");
        TextField emailField = new TextField("john@example.com");

        form.add(new Label("Passenger Name:"), 0, 0);
        form.add(nameField, 1, 0);
        form.add(new Label("Email:"), 0, 1);
        form.add(emailField, 1, 1);

        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    Ticket ticket = bookingService.bookTicket(
                            flight.getId(),
                            nameField.getText().trim(),
                            emailField.getText().trim()
                    );

                    // Show save dialog for boarding pass
                    String savedPath = pdfService.generateTicketPdf(ticket, flight, stage);

                    // Show success with calendar option
                    showBookingSuccess(ticket, flight, savedPath);
                    doSearch("", "");

                } catch (NoSeatsAvailableException ex) {
                    showError("No Seats Available", ex.getMessage());
                } catch (FlightNotFoundException ex) {
                    showError("Flight Not Found", ex.getMessage());
                } catch (InvalidBookingException ex) {
                    showError("Invalid Input", ex.getMessage());
                } catch (Exception ex) {
                    showError("Error", ex.getMessage());
                }
            }
        });
    }

    /**
     * Shows a success alert with an "Add to Google Calendar" button.
     */
    private void showBookingSuccess(Ticket ticket, Flight flight, String savedPath) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Booking Confirmed!");
        alert.setHeaderText("✈ Your ticket is booked!");
        alert.setContentText(
                "Ticket ID : " + ticket.getId() + "\n"
              + "Seat      : " + ticket.getSeatNumber() + "\n"
              + (savedPath != null ? "Saved to  : " + savedPath + "\n" : "")
              + "\nA confirmation email has been sent."
        );

        // Custom "Add to Calendar" button
        ButtonType calendarBtn = new ButtonType("📅 Add to Google Calendar", ButtonBar.ButtonData.LEFT);
        alert.getButtonTypes().add(calendarBtn);

        alert.showAndWait().ifPresent(btn -> {
            if (btn == calendarBtn) {
                calendarService.addFlightToCalendar(ticket, flight);
            }
        });
    }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Label styledLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #94a3b8;");
        return l;
    }

    private TextField styledField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setPrefWidth(150);
        return tf;
    }

    private Label largeLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #f1f5f9; -fx-font-size: 22px; -fx-font-weight: bold;");
        return l;
    }

    private Label smallLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
        return l;
    }

    private Button actionBtn(String text, String bg, String fg) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + fg +
                "; -fx-font-weight: bold; -fx-padding: 7 16; -fx-background-radius: 6; -fx-cursor: hand;");
        return btn;
    }
}