package skybook.exceptions;

/**
 * Custom Exception 3: Thrown when booking data is invalid.
 */
public class InvalidBookingException extends Exception {

    private String field;

    public InvalidBookingException(String message) {
        super(message);
        this.field = "unknown";
    }

    public InvalidBookingException(String field, String message) {
        super("Invalid booking field [" + field + "]: " + message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
