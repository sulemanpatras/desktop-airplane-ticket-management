package skybook.exceptions;

/**
 * Custom Exception 2: Thrown when a flight ID cannot be found.
 */
public class FlightNotFoundException extends Exception {

    private String searchedId;

    public FlightNotFoundException(String flightId) {
        super("Flight not found with ID: " + flightId);
        this.searchedId = flightId;
    }

    public FlightNotFoundException(String flightId, String context) {
        super("Flight '" + flightId + "' not found during: " + context);
        this.searchedId = flightId;
    }

    public String getSearchedId() {
        return searchedId;
    }
}
