package skybook.models;

import java.util.ArrayList;

/**
 * Passenger class - child of Person.
 * Demonstrates: Inheritance, Polymorphism (method overriding)
 */
public class Passenger extends Person {

    private String passportNumber;
    private ArrayList<String> ticketIds; // ArrayLists usage

    // Default constructor
    public Passenger() {
        super();
        this.passportNumber = "N/A";
        this.ticketIds = new ArrayList<>();
    }

    // Parameterized constructor
    public Passenger(String id, String name, String email, String phone, String passportNumber) {
        super(id, name, email, phone);
        this.passportNumber = passportNumber;
        this.ticketIds = new ArrayList<>();
    }

    // Constructor overloading - simpler version
    public Passenger(String id, String name, String email) {
        super(id, name, email);
        this.passportNumber = "UNSET";
        this.ticketIds = new ArrayList<>();
    }

    // Method overriding (Polymorphism)
    @Override
    public String getRole() {
        return "PASSENGER";
    }

    @Override
    public String getSummary() {
        return String.format("Passenger %s | Passport: %s | Tickets: %d",
                getName(), passportNumber, ticketIds.size());
    }

    // Method overloading
    public void addTicket(String ticketId) {
        ticketIds.add(ticketId);
    }

    public void addTicket(String ticketId, boolean notify) {
        ticketIds.add(ticketId);
        if (notify) {
            System.out.println("[INFO] Ticket " + ticketId + " added to passenger " + getName());
        }
    }

    public ArrayList<String> getTicketIds() { return ticketIds; }

    public String getPassportNumber() { return passportNumber; }
    public void setPassportNumber(String passportNumber) { this.passportNumber = passportNumber; }

    // Serialize to CSV line for file storage
    public String toCSV() {
        return String.join(",", getId(), getName(), getEmail(), getPhone(), passportNumber);
    }

    // Parse from CSV (file handling)
    public static Passenger fromCSV(String csvLine) {
        String[] parts = csvLine.split(",", -1);
        if (parts.length < 5) return null;
        return new Passenger(parts[0], parts[1], parts[2], parts[3], parts[4]);
    }
}
