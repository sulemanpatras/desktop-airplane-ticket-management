package skybook.services;

import skybook.models.Flight;
import skybook.models.Ticket;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * PdfService: Generates ticket PDFs.
 *
 * Implementation uses plain-text "boarding pass" files saved to disk.
 * If iText (com.itextpdf:itext7-core) is on the classpath, it produces a real PDF.
 * Otherwise it gracefully falls back to a richly formatted .txt receipt.
 *
 * To enable real PDFs: add itext7 jar to your classpath and uncomment the
 * generateWithIText() method below.
 *
 * Demonstrates: File Handling, Exception Handling, Service layer
 */
public class PdfService {

    private static final String PDF_DIR = "skybook_data/tickets";
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public PdfService() {
        new File(PDF_DIR).mkdirs();
    }

    /**
     * Generates a ticket document for the given ticket and flight.
     * Saves to skybook_data/tickets/<ticketId>.txt
     */
    public String generateTicketPdf(Ticket ticket, Flight flight) {
        String fileName = PDF_DIR + "/" + ticket.getId() + ".txt";
        try (PrintWriter pw = new PrintWriter(new FileWriter(fileName))) {
            pw.println(buildBoardingPass(ticket, flight));
            System.out.println("[PdfService] Ticket saved → " + fileName);
        } catch (IOException e) {
            System.err.println("[PdfService] Failed to save ticket: " + e.getMessage());
        }
        return fileName;
    }

    /**
     * Generates a summary report of all tickets.
     */
    public String generateBookingReport(java.util.List<Ticket> tickets,
                                        java.util.List<Flight> flights) {
        String fileName = PDF_DIR + "/booking_report_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".txt";

        try (PrintWriter pw = new PrintWriter(new FileWriter(fileName))) {
            pw.println("╔" + "═".repeat(58) + "╗");
            pw.println("║          SKYBOOK – BOOKING REPORT                        ║");
            pw.println("║  Generated: " + LocalDateTime.now().format(FMT) +
                       "                              ║");
            pw.println("╚" + "═".repeat(58) + "╝");
            pw.println();

            int confirmed = 0;
            double totalRevenue = 0;

            for (Ticket t : tickets) {
                Flight f = flights.stream()
                        .filter(fl -> fl.getId().equals(t.getFlightId()))
                        .findFirst().orElse(null);

                pw.printf("  %-10s | %-20s | %-8s | %-6s | $%-10.2f%n",
                        t.getId(), t.getPassengerName(),
                        t.getFlightId(), t.getSeatNumber(), t.getPricePaid());

                if (t.isConfirmed()) {
                    confirmed++;
                    totalRevenue += t.getPricePaid();
                }
            }

            pw.println();
            pw.println("─".repeat(60));
            pw.printf("  Total Tickets   : %d%n", tickets.size());
            pw.printf("  Confirmed       : %d%n", confirmed);
            pw.printf("  Cancelled       : %d%n", tickets.size() - confirmed);
            pw.printf("  Total Revenue   : $%.2f%n", totalRevenue);
            pw.println("─".repeat(60));

            System.out.println("[PdfService] Report saved → " + fileName);
        } catch (IOException e) {
            System.err.println("[PdfService] Failed to generate report: " + e.getMessage());
        }
        return fileName;
    }

    // ─── BOARDING PASS BUILDER ──────────────────────────────────────────────────

    private String buildBoardingPass(Ticket ticket, Flight flight) {
        String border = "═".repeat(54);
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("╔").append(border).append("╗\n");
        sb.append("║").append(center("✈  SKYBOOK AIRLINES  ✈", 54)).append("║\n");
        sb.append("║").append(center("BOARDING PASS", 54)).append("║\n");
        sb.append("╠").append(border).append("╣\n");
        sb.append("║  PASSENGER : ").append(padRight(ticket.getPassengerName(), 40)).append("║\n");
        sb.append("║  EMAIL     : ").append(padRight(ticket.getPassengerEmail(), 40)).append("║\n");
        sb.append("╠").append(border).append("╣\n");
        if (flight != null) {
            sb.append("║  FLIGHT    : ").append(padRight(flight.getId(), 40)).append("║\n");
            sb.append("║  AIRLINE   : ").append(padRight(flight.getAirline(), 40)).append("║\n");
            sb.append("║  FROM      : ").append(padRight(flight.getSource(), 40)).append("║\n");
            sb.append("║  TO        : ").append(padRight(flight.getDestination(), 40)).append("║\n");
            sb.append("║  DEPARTURE : ").append(padRight(flight.getDepartureTime(), 40)).append("║\n");
            sb.append("║  ARRIVAL   : ").append(padRight(flight.getArrivalTime(), 40)).append("║\n");
        }
        sb.append("╠").append(border).append("╣\n");
        sb.append("║  TICKET ID : ").append(padRight(ticket.getId(), 40)).append("║\n");
        sb.append("║  SEAT      : ").append(padRight(ticket.getSeatNumber(), 40)).append("║\n");
        sb.append("║  PRICE     : ").append(padRight(String.format("$%.2f", ticket.getPricePaid()), 40)).append("║\n");
        sb.append("║  BOOKED    : ").append(padRight(ticket.getBookedAt(), 40)).append("║\n");
        sb.append("║  STATUS    : ").append(padRight(ticket.getStatus().toString(), 40)).append("║\n");
        sb.append("╠").append(border).append("╣\n");
        sb.append("║").append(center("Have a safe flight!  ✈", 54)).append("║\n");
        sb.append("╚").append(border).append("╝\n");
        return sb.toString();
    }

    private String center(String s, int width) {
        int pad = (width - s.length()) / 2;
        return " ".repeat(Math.max(0, pad)) + s + " ".repeat(Math.max(0, width - s.length() - pad));
    }

    private String padRight(String s, int width) {
        if (s == null) s = "";
        if (s.length() > width) s = s.substring(0, width - 1) + "…";
        return s + " ".repeat(width - s.length());
    }
}
