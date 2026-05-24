package skybook.services;

import skybook.models.*;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DataStore: Handles all file I/O for flights, tickets, and passengers.
 * Demonstrates: File Handling (create, read, update)
 */
public class DataStore {

    private static final String DATA_DIR   = "skybook_data";
    private static final String FLIGHTS_FILE    = DATA_DIR + "/flights.csv";
    private static final String TICKETS_FILE    = DATA_DIR + "/tickets.csv";
    private static final String PASSENGERS_FILE = DATA_DIR + "/passengers.csv";
    private static final String ADMINS_FILE     = DATA_DIR + "/admins.csv";

    // Singleton pattern so one instance manages all files
    private static DataStore instance;

    private DataStore() {
        initDataDirectory();
    }

    public static DataStore getInstance() {
        if (instance == null) instance = new DataStore();
        return instance;
    }

    /**
     * Creates the data directory and seeds default data if files don't exist.
     */
    private void initDataDirectory() {
        try {
            Files.createDirectories(Paths.get(DATA_DIR));

            if (!new File(FLIGHTS_FILE).exists()) seedFlights();
            if (!new File(PASSENGERS_FILE).exists()) seedPassengers();
            if (!new File(ADMINS_FILE).exists()) seedAdmins();
            if (!new File(TICKETS_FILE).exists()) {
                new FileWriter(TICKETS_FILE, false).close(); // empty file
            }
        } catch (IOException e) {
            System.err.println("[DataStore] Failed to init data directory: " + e.getMessage());
        }
    }

    // ─── FLIGHTS ────────────────────────────────────────────────────────────────

    public List<Flight> loadFlights() {
        List<Flight> flights = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(FLIGHTS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                Flight f = Flight.fromCSV(line);
                if (f != null) flights.add(f);
            }
        } catch (IOException e) {
            System.err.println("[DataStore] Error reading flights: " + e.getMessage());
        }
        return flights;
    }

    public void saveFlights(List<Flight> flights) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(FLIGHTS_FILE, false))) {
            for (Flight f : flights) pw.println(f.toCSV());
        } catch (IOException e) {
            System.err.println("[DataStore] Error saving flights: " + e.getMessage());
        }
    }

    public void appendFlight(Flight flight) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(FLIGHTS_FILE, true))) {
            pw.println(flight.toCSV());
        } catch (IOException e) {
            System.err.println("[DataStore] Error appending flight: " + e.getMessage());
        }
    }

    // ─── TICKETS ────────────────────────────────────────────────────────────────

    public List<Ticket> loadTickets() {
        List<Ticket> tickets = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(TICKETS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                Ticket t = Ticket.fromCSV(line);
                if (t != null) tickets.add(t);
            }
        } catch (IOException e) {
            System.err.println("[DataStore] Error reading tickets: " + e.getMessage());
        }
        return tickets;
    }

    public void saveTickets(List<Ticket> tickets) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(TICKETS_FILE, false))) {
            for (Ticket t : tickets) pw.println(t.toCSV());
        } catch (IOException e) {
            System.err.println("[DataStore] Error saving tickets: " + e.getMessage());
        }
    }

    public void appendTicket(Ticket ticket) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(TICKETS_FILE, true))) {
            pw.println(ticket.toCSV());
        } catch (IOException e) {
            System.err.println("[DataStore] Error appending ticket: " + e.getMessage());
        }
    }

    // ─── PASSENGERS ─────────────────────────────────────────────────────────────

    public List<Passenger> loadPassengers() {
        List<Passenger> passengers = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(PASSENGERS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                Passenger p = Passenger.fromCSV(line);
                if (p != null) passengers.add(p);
            }
        } catch (IOException e) {
            System.err.println("[DataStore] Error reading passengers: " + e.getMessage());
        }
        return passengers;
    }

    public void savePassengers(List<Passenger> passengers) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(PASSENGERS_FILE, false))) {
            for (Passenger p : passengers) pw.println(p.toCSV());
        } catch (IOException e) {
            System.err.println("[DataStore] Error saving passengers: " + e.getMessage());
        }
    }

    public void appendPassenger(Passenger passenger) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(PASSENGERS_FILE, true))) {
            pw.println(passenger.toCSV());
        } catch (IOException e) {
            System.err.println("[DataStore] Error appending passenger: " + e.getMessage());
        }
    }

    // ─── ADMINS ─────────────────────────────────────────────────────────────────

    public List<Admin> loadAdmins() {
        List<Admin> admins = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(ADMINS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                Admin a = Admin.fromCSV(line);
                if (a != null) admins.add(a);
            }
        } catch (IOException e) {
            System.err.println("[DataStore] Error reading admins: " + e.getMessage());
        }
        return admins;
    }

    // ─── SEED DATA ──────────────────────────────────────────────────────────────

    private void seedFlights() throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(FLIGHTS_FILE, false))) {
            pw.println("FL001,PIA,Karachi,Lahore,2025-06-10 08:00,2025-06-10 09:30,8500.00,120,150,SCHEDULED");
            pw.println("FL002,Emirates,Karachi,Dubai,2025-06-11 14:00,2025-06-11 16:30,45000.00,80,200,SCHEDULED");
            pw.println("FL003,AirBlue,Lahore,Islamabad,2025-06-12 09:00,2025-06-12 10:00,6500.00,60,100,SCHEDULED");
            pw.println("FL004,Qatar Airways,Islamabad,London,2025-06-13 22:00,2025-06-14 06:00,180000.00,30,250,SCHEDULED");
            pw.println("FL005,PIA,Peshawar,Karachi,2025-06-14 07:00,2025-06-14 09:00,9000.00,100,130,SCHEDULED");
            pw.println("FL006,FlyDubai,Lahore,Dubai,2025-06-15 18:00,2025-06-15 20:30,42000.00,50,180,DELAYED");
        }
        System.out.println("[DataStore] Seeded flights.csv");
    }

    private void seedPassengers() throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(PASSENGERS_FILE, false))) {
            pw.println("P001,Ali Hassan,ali@example.com,+92-300-1234567,PK1234567");
            pw.println("P002,Sara Khan,sara@example.com,+92-321-7654321,PK9876543");
            pw.println("P003,Ahmed Raza,ahmed@example.com,+92-333-1111111,PK1111111");
        }
        System.out.println("[DataStore] Seeded passengers.csv");
    }

    private void seedAdmins() throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(ADMINS_FILE, false))) {
            pw.println("A001,Admin User,admin@skybook.com,+92-21-0000000,ADMIN123,Operations");
        }
        System.out.println("[DataStore] Seeded admins.csv");
    }
}
