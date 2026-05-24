package skybook.services;

import skybook.exceptions.*;
import skybook.models.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * BookingService: All booking-related business logic.
 * Demonstrates: Exception Handling, ArrayLists, OOP composition
 */
public class BookingService {

    private List<Flight> flights;
    private List<Ticket> tickets;
    private List<Passenger> passengers;
    private final DataStore dataStore;
    private final EmailService emailService;
    private final PdfService pdfService;

    private int ticketCounter;

    public BookingService() {
        this.dataStore = DataStore.getInstance();
        this.emailService = new EmailService();
        this.pdfService = new PdfService();
        loadAll();
    }

    private void loadAll() {
        this.flights    = new ArrayList<>(dataStore.loadFlights());
        this.tickets    = new ArrayList<>(dataStore.loadTickets());
        this.passengers = new ArrayList<>(dataStore.loadPassengers());
        this.ticketCounter = 10000 + tickets.size();
    }

    // ─── FLIGHTS ────────────────────────────────────────────────────────────────

    public List<Flight> getAllFlights() {
        return new ArrayList<>(flights);
    }

    public List<Flight> searchFlights(String source, String destination) {
        return flights.stream()
                .filter(f -> (source == null || source.isEmpty() ||
                              f.getSource().toLowerCase().contains(source.toLowerCase())))
                .filter(f -> (destination == null || destination.isEmpty() ||
                              f.getDestination().toLowerCase().contains(destination.toLowerCase())))
                .filter(Flight::hasAvailableSeats)
                .collect(Collectors.toList());
    }

    public Flight findFlightById(String id) throws FlightNotFoundException {
        return flights.stream()
                .filter(f -> f.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new FlightNotFoundException(id, "find flight"));
    }

    public void addFlight(Flight flight) {
        flights.add(flight);
        dataStore.saveFlights(flights);
    }

    public void updateFlight(Flight updated) throws FlightNotFoundException {
        for (int i = 0; i < flights.size(); i++) {
            if (flights.get(i).getId().equals(updated.getId())) {
                flights.set(i, updated);
                dataStore.saveFlights(flights);
                return;
            }
        }
        throw new FlightNotFoundException(updated.getId(), "update flight");
    }

    public void deleteFlight(String flightId) throws FlightNotFoundException {
        boolean removed = flights.removeIf(f -> f.getId().equals(flightId));
        if (!removed) throw new FlightNotFoundException(flightId, "delete flight");
        dataStore.saveFlights(flights);
    }

    // ─── BOOKING ────────────────────────────────────────────────────────────────

    /**
     * Books a ticket for a passenger on a flight.
     * Demonstrates: try-catch, custom exceptions
     */
    public Ticket bookTicket(String flightId, String passengerName, String passengerEmail)
            throws FlightNotFoundException, NoSeatsAvailableException, InvalidBookingException {

        // Validate input
        if (passengerName == null || passengerName.trim().isEmpty()) {
            throw new InvalidBookingException("passengerName", "Name cannot be empty");
        }
        if (passengerEmail == null || !passengerEmail.contains("@")) {
            throw new InvalidBookingException("passengerEmail", "Invalid email address");
        }

        // Find flight
        Flight flight = findFlightById(flightId);

        // Check seats
        if (!flight.hasAvailableSeats()) {
            throw new NoSeatsAvailableException(flightId);
        }

        // Generate seat number
        int seatNum = flight.getTotalSeats() - flight.getSeatsAvailable() + 1;
        int row = (int) Math.ceil(seatNum / 6.0);
        char col = (char) ('A' + ((seatNum - 1) % 6));
        String seatNumber = row + "" + col;

        // Generate ticket ID
        ticketCounter++;
        String ticketId = "TK" + ticketCounter;

        // Find or create passenger
        String passengerId = findOrCreatePassenger(passengerName, passengerEmail);

        // Create ticket
        Ticket ticket = new Ticket(ticketId, passengerId, passengerName, passengerEmail,
                flightId, seatNumber, flight.getPrice());

        // Update seats
        flight.setSeatsAvailable(flight.getSeatsAvailable() - 1);

        // Persist
        tickets.add(ticket);
        dataStore.saveTickets(tickets);
        dataStore.saveFlights(flights);

        // Side effects: email + PDF
        try {
            emailService.sendBookingConfirmation(ticket, flight);
            pdfService.generateTicketPdf(ticket, flight);
        } catch (Exception e) {
            System.err.println("[BookingService] Post-booking side effect failed: " + e.getMessage());
        }

        return ticket;
    }

    public void cancelTicket(String ticketId) throws Exception {
        Ticket ticket = tickets.stream()
                .filter(t -> t.getId().equals(ticketId))
                .findFirst()
                .orElseThrow(() -> new Exception("Ticket not found: " + ticketId));

        if (!ticket.isConfirmed()) {
            throw new InvalidBookingException("status", "Ticket is already cancelled");
        }

        ticket.cancel();

        // Restore seat
        try {
            Flight flight = findFlightById(ticket.getFlightId());
            flight.setSeatsAvailable(flight.getSeatsAvailable() + 1);
            dataStore.saveFlights(flights);
        } catch (FlightNotFoundException e) {
            System.err.println("[BookingService] Flight not found when restoring seat: " + e.getMessage());
        }

        dataStore.saveTickets(tickets);
        emailService.sendCancellationNotice(ticket);
    }

    // ─── GETTERS ────────────────────────────────────────────────────────────────

    public List<Ticket> getAllTickets() { return new ArrayList<>(tickets); }

    public List<Ticket> getTicketsForPassenger(String passengerEmail) {
        return tickets.stream()
                .filter(t -> t.getPassengerEmail().equalsIgnoreCase(passengerEmail))
                .collect(Collectors.toList());
    }

    public List<Passenger> getAllPassengers() { return new ArrayList<>(passengers); }

    // Revenue
    public double getTotalRevenue() {
        return tickets.stream()
                .filter(Ticket::isConfirmed)
                .mapToDouble(Ticket::getPricePaid)
                .sum();
    }

    // ─── HELPER ─────────────────────────────────────────────────────────────────

    private String findOrCreatePassenger(String name, String email) {
        for (Passenger p : passengers) {
            if (p.getEmail().equalsIgnoreCase(email)) return p.getId();
        }
        String pid = "P" + String.format("%03d", passengers.size() + 1);
        Passenger np = new Passenger(pid, name, email);
        passengers.add(np);
        dataStore.appendPassenger(np);
        return pid;
    }

    public void refresh() {
        loadAll();
    }
}
