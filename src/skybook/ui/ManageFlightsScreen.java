package skybook.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import skybook.exceptions.FlightNotFoundException;
import skybook.models.Flight;
import skybook.services.BookingService;

import java.util.List;

/**
 * Manage Flights Screen (Admin).
 * Demonstrates: JavaFX ComboBox, GridPane, TextField, CRUD operations
 */
public class ManageFlightsScreen {

    private final BookingService bookingService;
    private VBox listBox;

    public ManageFlightsScreen(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    public VBox getView() {
        VBox view = new VBox(20);
        view.setPadding(new Insets(28));
        view.setStyle("-fx-background-color: #0f172a;");

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Admin · Manage Flights");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #f1f5f9;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addBtn = new Button("+ Add Flight");
        addBtn.setStyle("""
            -fx-background-color: #38bdf8;
            -fx-text-fill: #0f172a;
            -fx-font-weight: bold;
            -fx-padding: 9 20;
            -fx-background-radius: 7;
            -fx-cursor: hand;
        """);
        addBtn.setOnAction(e -> showFlightDialog(null));

        header.getChildren().addAll(title, spacer, addBtn);

        listBox = new VBox(10);
        refreshList();

        ScrollPane scroll = new ScrollPane(listBox);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: #0f172a;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        view.getChildren().addAll(header, scroll);
        return view;
    }

    private void refreshList() {
        listBox.getChildren().clear();
        List<Flight> flights = bookingService.getAllFlights();
        for (Flight f : flights) {
            listBox.getChildren().add(buildFlightRow(f));
        }
    }

    private HBox buildFlightRow(Flight flight) {
        HBox row = new HBox(16);
        row.setPadding(new Insets(14, 18, 14, 18));
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 8;");

        VBox info = new VBox(4);
        Label id = new Label(flight.getId() + "  ·  " + flight.getAirline());
        id.setStyle("-fx-font-weight: bold; -fx-text-fill: #f1f5f9;");
        Label route = new Label(flight.getSource() + " → " + flight.getDestination() +
                "  |  " + flight.getDepartureTime());
        route.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
        Label seats = new Label("Seats: " + flight.getSeatsAvailable() + "/" + flight.getTotalSeats() +
                "  |  $" + String.format("%.0f", flight.getPrice()));
        seats.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");

        String statusColor = flight.getStatus().equals("SCHEDULED") ? "#34d399" :
                flight.getStatus().equals("DELAYED") ? "#f59e0b" : "#f87171";
        Label status = new Label("  " + flight.getStatus() + "  ");
        status.setStyle("-fx-background-color: " + statusColor + "22; -fx-text-fill: " +
                statusColor + "; -fx-font-size: 10px; -fx-background-radius: 20; -fx-padding: 3 8;");

        info.getChildren().addAll(id, route, seats);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button editBtn = iconBtn("✏ Edit", "#38bdf8");
        editBtn.setOnAction(e -> showFlightDialog(flight));

        Button delBtn = iconBtn("🗑 Delete", "#f87171");
        delBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Delete flight " + flight.getId() + "?", ButtonType.YES, ButtonType.NO);
            confirm.showAndWait().ifPresent(btn -> {
                if (btn == ButtonType.YES) {
                    try {
                        bookingService.deleteFlight(flight.getId());
                        refreshList();
                    } catch (FlightNotFoundException ex) {
                        new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
                    }
                }
            });
        });

        row.getChildren().addAll(info, sp, status, editBtn, delBtn);
        return row;
    }

    private void showFlightDialog(Flight existing) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add New Flight" : "Edit Flight " + existing.getId());

        GridPane form = new GridPane();
        form.setHgap(14);
        form.setVgap(12);
        form.setPadding(new Insets(20));

        TextField idField        = field(existing == null ? autoId() : existing.getId());
        TextField airlineField   = field(existing == null ? "" : existing.getAirline());
        TextField sourceField    = field(existing == null ? "" : existing.getSource());
        TextField destField      = field(existing == null ? "" : existing.getDestination());
        TextField depField       = field(existing == null ? "" : existing.getDepartureTime());
        TextField arrField       = field(existing == null ? "" : existing.getArrivalTime());
        TextField priceField     = field(existing == null ? "0" : String.valueOf((int)existing.getPrice()));
        TextField seatsAvField   = field(existing == null ? "150" : String.valueOf(existing.getSeatsAvailable()));
        TextField totalSeatsField= field(existing == null ? "150" : String.valueOf(existing.getTotalSeats()));

        ComboBox<String> statusBox = new ComboBox<>();
        statusBox.getItems().addAll("SCHEDULED", "DELAYED", "CANCELLED");
        statusBox.setValue(existing == null ? "SCHEDULED" : existing.getStatus());

        // Disable ID editing for existing flights
        if (existing != null) idField.setDisable(true);

        int r = 0;
        form.add(new Label("Flight ID:"),   0, r); form.add(idField, 1, r++);
        form.add(new Label("Airline:"),     0, r); form.add(airlineField, 1, r++);
        form.add(new Label("From:"),        0, r); form.add(sourceField, 1, r++);
        form.add(new Label("To:"),          0, r); form.add(destField, 1, r++);
        form.add(new Label("Departure:"),   0, r); form.add(depField, 1, r++);
        form.add(new Label("Arrival:"),     0, r); form.add(arrField, 1, r++);
        form.add(new Label("Price (PKR):"), 0, r); form.add(priceField, 1, r++);
        form.add(new Label("Seats Avail:"), 0, r); form.add(seatsAvField, 1, r++);
        form.add(new Label("Total Seats:"), 0, r); form.add(totalSeatsField, 1, r++);
        form.add(new Label("Status:"),      0, r); form.add(statusBox, 1, r);

        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    Flight f = new Flight(
                        idField.getText().trim(),
                        airlineField.getText().trim(),
                        sourceField.getText().trim(),
                        destField.getText().trim(),
                        depField.getText().trim(),
                        arrField.getText().trim(),
                        Double.parseDouble(priceField.getText().trim()),
                        Integer.parseInt(seatsAvField.getText().trim()),
                        Integer.parseInt(totalSeatsField.getText().trim())
                    );
                    f.setStatus(statusBox.getValue());

                    if (existing == null) {
                        bookingService.addFlight(f);
                    } else {
                        bookingService.updateFlight(f);
                    }
                    refreshList();
                } catch (FlightNotFoundException ex) {
                    new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
                } catch (NumberFormatException ex) {
                    new Alert(Alert.AlertType.ERROR, "Price and seats must be valid numbers.").showAndWait();
                }
            }
        });
    }

    private TextField field(String val) {
        TextField tf = new TextField(val);
        tf.setPrefWidth(220);
        return tf;
    }

    private Button iconBtn(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: " + color + "22; -fx-text-fill: " + color +
                "; -fx-font-size: 12px; -fx-padding: 6 12; -fx-background-radius: 5; -fx-cursor: hand;");
        return btn;
    }

    private String autoId() {
        int count = bookingService.getAllFlights().size();
        return "FL" + String.format("%03d", count + 1);
    }
}
