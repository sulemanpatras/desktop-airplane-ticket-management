package skybook.exceptions;

/**
 * Custom Exception 1: Thrown when no seats are available on a flight.
 * Demonstrates: Custom Exception Handling
 */
public class NoSeatsAvailableException extends Exception {

    private String flightId;

    public NoSeatsAvailableException(String flightId) {
        super("No seats available on flight: " + flightId);
        this.flightId = flightId;
    }

    public NoSeatsAvailableException(String flightId, String message) {
        super(message);
        this.flightId = flightId;
    }

    public String getFlightId() {
        return flightId;
    }
}
