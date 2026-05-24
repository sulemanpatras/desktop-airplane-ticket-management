package skybook.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Ticket entity class.
 * Demonstrates: Encapsulation, Constructors
 */
public class Ticket {

    public enum Status { CONFIRMED, CANCELLED, PENDING }

    private String id;
    private String passengerId;
    private String passengerName;
    private String passengerEmail;
    private String flightId;
    private String seatNumber;
    private String bookedAt;
    private Status status;
    private double pricePaid;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // Default constructor
    public Ticket() {
        this.id = "TK00000";
        this.status = Status.PENDING;
        this.bookedAt = LocalDateTime.now().format(FMT);
    }

    // Parameterized constructor
    public Ticket(String id, String passengerId, String passengerName, String passengerEmail,
                  String flightId, String seatNumber, double pricePaid) {
        this.id = id;
        this.passengerId = passengerId;
        this.passengerName = passengerName;
        this.passengerEmail = passengerEmail;
        this.flightId = flightId;
        this.seatNumber = seatNumber;
        this.pricePaid = pricePaid;
        this.status = Status.CONFIRMED;
        this.bookedAt = LocalDateTime.now().format(FMT);
    }

    // Constructor overloading - with explicit booking time
    public Ticket(String id, String passengerId, String passengerName, String passengerEmail,
                  String flightId, String seatNumber, double pricePaid, String bookedAt, Status status) {
        this(id, passengerId, passengerName, passengerEmail, flightId, seatNumber, pricePaid);
        this.bookedAt = bookedAt;
        this.status = status;
    }

    public void cancel() {
        this.status = Status.CANCELLED;
    }

    public boolean isConfirmed() {
        return this.status == Status.CONFIRMED;
    }

    // Build a nicely formatted ticket receipt string
    public String getReceiptText(Flight flight) {
        String border = "═".repeat(50);
        return "\n" + border + "\n" +
               "           ✈  SKYBOOK BOARDING PASS  ✈\n" +
               border + "\n" +
               String.format("  Ticket ID  : %s\n", id) +
               String.format("  Passenger  : %s\n", passengerName) +
               String.format("  Email      : %s\n", passengerEmail) +
               (flight != null ? String.format("  Flight     : %s\n", flight.getId()) : "") +
               (flight != null ? String.format("  Route      : %s\n", flight.getRouteDisplay()) : "") +
               (flight != null ? String.format("  Airline    : %s\n", flight.getAirline()) : "") +
               (flight != null ? String.format("  Departure  : %s\n", flight.getDepartureTime()) : "") +
               String.format("  Seat       : %s\n", seatNumber) +
               String.format("  Price Paid : $%.2f\n", pricePaid) +
               String.format("  Booked At  : %s\n", bookedAt) +
               String.format("  Status     : %s\n", status) +
               border + "\n";
    }

    // CSV serialization
    public String toCSV() {
        return String.join(",",
                id, passengerId, passengerName, passengerEmail,
                flightId, seatNumber,
                String.valueOf(pricePaid),
                bookedAt, status.name());
    }

    public static Ticket fromCSV(String csvLine) {
        String[] p = csvLine.split(",", -1);
        if (p.length < 9) return null;
        return new Ticket(p[0], p[1], p[2], p[3], p[4], p[5],
                Double.parseDouble(p[6]), p[7], Status.valueOf(p[8]));
    }

    // Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPassengerId() { return passengerId; }
    public void setPassengerId(String passengerId) { this.passengerId = passengerId; }

    public String getPassengerName() { return passengerName; }
    public void setPassengerName(String passengerName) { this.passengerName = passengerName; }

    public String getPassengerEmail() { return passengerEmail; }
    public void setPassengerEmail(String passengerEmail) { this.passengerEmail = passengerEmail; }

    public String getFlightId() { return flightId; }
    public void setFlightId(String flightId) { this.flightId = flightId; }

    public String getSeatNumber() { return seatNumber; }
    public void setSeatNumber(String seatNumber) { this.seatNumber = seatNumber; }

    public String getBookedAt() { return bookedAt; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public double getPricePaid() { return pricePaid; }
    public void setPricePaid(double pricePaid) { this.pricePaid = pricePaid; }

    @Override
    public String toString() {
        return String.format("[%s] %s | Flight: %s | Seat: %s | %s | $%.2f",
                id, passengerName, flightId, seatNumber, status, pricePaid);
    }
}
