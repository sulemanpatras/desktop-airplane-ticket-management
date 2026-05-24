package skybook.models;

/**
 * Flight entity class.
 * Demonstrates: Encapsulation, Constructors
 */
public class Flight {

    private String id;
    private String airline;
    private String source;
    private String destination;
    private String departureTime;
    private String arrivalTime;
    private double price;
    private int seatsAvailable;
    private int totalSeats;
    private String status; // SCHEDULED, DELAYED, CANCELLED

    // Default constructor
    public Flight() {
        this.id = "FL000";
        this.airline = "Unknown";
        this.source = "N/A";
        this.destination = "N/A";
        this.departureTime = "";
        this.arrivalTime = "";
        this.price = 0.0;
        this.seatsAvailable = 0;
        this.totalSeats = 0;
        this.status = "SCHEDULED";
    }

    // Parameterized constructor
    public Flight(String id, String airline, String source, String destination,
                  String departureTime, String arrivalTime, double price,
                  int seatsAvailable, int totalSeats) {
        this.id = id;
        this.airline = airline;
        this.source = source;
        this.destination = destination;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.price = price;
        this.seatsAvailable = seatsAvailable;
        this.totalSeats = totalSeats;
        this.status = "SCHEDULED";
    }

    // Constructor overloading - minimal version
    public Flight(String id, String source, String destination, double price, int seats) {
        this();
        this.id = id;
        this.source = source;
        this.destination = destination;
        this.price = price;
        this.totalSeats = seats;
        this.seatsAvailable = seats;
    }

    // Business logic
    public boolean hasAvailableSeats() {
        return seatsAvailable > 0;
    }

    public double getOccupancyPercentage() {
        if (totalSeats == 0) return 0;
        return ((double)(totalSeats - seatsAvailable) / totalSeats) * 100;
    }

    public String getRouteDisplay() {
        return source + " → " + destination;
    }

    // Serialize to CSV
    public String toCSV() {
        return String.join(",",
                id, airline, source, destination,
                departureTime, arrivalTime,
                String.valueOf(price),
                String.valueOf(seatsAvailable),
                String.valueOf(totalSeats),
                status);
    }

    // Parse from CSV
    public static Flight fromCSV(String csvLine) {
        String[] p = csvLine.split(",", -1);
        if (p.length < 10) return null;
        Flight f = new Flight(p[0], p[1], p[2], p[3], p[4], p[5],
                Double.parseDouble(p[6]),
                Integer.parseInt(p[7]),
                Integer.parseInt(p[8]));
        f.setStatus(p[9]);
        return f;
    }

    // Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getAirline() { return airline; }
    public void setAirline(String airline) { this.airline = airline; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public String getDepartureTime() { return departureTime; }
    public void setDepartureTime(String departureTime) { this.departureTime = departureTime; }

    public String getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(String arrivalTime) { this.arrivalTime = arrivalTime; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public int getSeatsAvailable() { return seatsAvailable; }
    public void setSeatsAvailable(int seatsAvailable) { this.seatsAvailable = seatsAvailable; }

    public int getTotalSeats() { return totalSeats; }
    public void setTotalSeats(int totalSeats) { this.totalSeats = totalSeats; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return String.format("[%s] %s | %s → %s | $%.2f | Seats: %d/%d | %s",
                id, airline, source, destination, price, seatsAvailable, totalSeats, status);
    }
}
