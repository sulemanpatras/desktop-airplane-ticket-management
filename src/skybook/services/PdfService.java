package skybook.services;

import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import skybook.models.Flight;
import skybook.models.Ticket;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * PdfService: Generates ticket/report documents.
 *
 * NOW WITH: FileChooser dialog so the user picks where to save the file,
 * exactly like Chrome's download dialog.
 *
 * Uses plain-text "boarding pass" files (.txt).
 * To generate real PDFs: add iText7 (com.itextpdf:itext7-core) to classpath
 * and swap the PrintWriter block with the iText7 PdfWriter block (see comments below).
 *
 * Demonstrates: File Handling (FileChooser), Exception Handling, Service layer
 */
public class PdfService {

    private static final String DEFAULT_DIR = "skybook_data/tickets";
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public PdfService() {
        new File(DEFAULT_DIR).mkdirs();
    }

    // ─── TICKET PDF ──────────────────────────────────────────────────────────

    /**
     * Generates a boarding-pass text file.
     * Shows a FileChooser so the user picks the save location (like Chrome downloads).
     *
     * @param ticket  the ticket to print
     * @param flight  the associated flight
     * @param owner   the JavaFX window to parent the dialog to (pass primaryStage)
     * @return        the saved file path, or null if user cancelled
     */
    public String generateTicketPdf(Ticket ticket, Flight flight, Window owner) {
        // Default filename
        String defaultName = "Ticket_" + ticket.getId() + ".txt";

        File chosen = showSaveDialog(owner, defaultName, "Ticket Files (*.txt)", "*.txt");
        if (chosen == null) {
            // User cancelled — silently fall back to default location
            chosen = new File(DEFAULT_DIR + "/" + defaultName);
        }

        return writeTicketFile(ticket, flight, chosen);
    }

    /**
     * Overload for headless / background use (no dialog).
     */
    public String generateTicketPdf(Ticket ticket, Flight flight) {
        File out = new File(DEFAULT_DIR + "/Ticket_" + ticket.getId() + ".txt");
        return writeTicketFile(ticket, flight, out);
    }

    // ─── BOOKING REPORT ──────────────────────────────────────────────────────

    /**
     * Generates a summary report.
     * Shows a FileChooser so the user picks the save location.
     */
    public String generateBookingReport(List<Ticket> tickets, List<Flight> flights, Window owner) {
        String defaultName = "SkyBook_Report_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".txt";

        File chosen = showSaveDialog(owner, defaultName, "Report Files (*.txt)", "*.txt");
        if (chosen == null) {
            chosen = new File(DEFAULT_DIR + "/" + defaultName);
        }

        return writeReportFile(tickets, flights, chosen);
    }

    /**
     * Overload for headless use (no dialog).
     */
    public String generateBookingReport(List<Ticket> tickets, List<Flight> flights) {
        String defaultName = "SkyBook_Report_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".txt";
        File out = new File(DEFAULT_DIR + "/" + defaultName);
        return writeReportFile(tickets, flights, out);
    }

    // ─── FILE CHOOSER ────────────────────────────────────────────────────────

    /**
     * Shows a native Save File dialog (exactly like Chrome's download prompt).
     *
     * @param owner        parent window
     * @param defaultName  suggested filename
     * @param description  extension description shown in the filter dropdown
     * @param extension    e.g. "*.txt" or "*.pdf"
     * @return             the chosen File, or null if user cancelled
     */
    public static File showSaveDialog(Window owner, String defaultName,
                                      String description, String extension) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save File – SkyBook");
        chooser.setInitialFileName(defaultName);

        // Start in user's home/Downloads if it exists
        File downloads = new File(System.getProperty("user.home") + "/Downloads");
        if (downloads.exists()) {
            chooser.setInitialDirectory(downloads);
        } else {
            chooser.setInitialDirectory(new File(System.getProperty("user.home")));
        }

        chooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter(description, extension),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        return chooser.showSaveDialog(owner);
    }

    // ─── WRITERS ─────────────────────────────────────────────────────────────

    private String writeTicketFile(Ticket ticket, Flight flight, File outFile) {
        try {
            outFile.getParentFile().mkdirs();
            try (PrintWriter pw = new PrintWriter(new FileWriter(outFile))) {
                pw.println(buildBoardingPass(ticket, flight));
            }
            System.out.println("[PdfService] Ticket saved → " + outFile.getAbsolutePath());
            return outFile.getAbsolutePath();
        } catch (IOException e) {
            System.err.println("[PdfService] Failed to save ticket: " + e.getMessage());
            return null;
        }

        /*
         * ── iText7 PDF block (uncomment if iText7 is on classpath) ──────────
         * import com.itextpdf.kernel.pdf.*;
         * import com.itextpdf.layout.*;
         * import com.itextpdf.layout.element.*;
         *
         * PdfWriter writer = new PdfWriter(outFile);
         * PdfDocument pdfDoc = new PdfDocument(writer);
         * Document doc = new Document(pdfDoc);
         * doc.add(new Paragraph("SKYBOOK BOARDING PASS").setBold().setFontSize(18));
         * doc.add(new Paragraph("Ticket: " + ticket.getId()));
         * // ... add all fields
         * doc.close();
         * ────────────────────────────────────────────────────────────────────
         */
    }

    private String writeReportFile(List<Ticket> tickets, List<Flight> flights, File outFile) {
        try {
            outFile.getParentFile().mkdirs();
            try (PrintWriter pw = new PrintWriter(new FileWriter(outFile))) {
                pw.println("╔" + "═".repeat(58) + "╗");
                pw.println("║          SKYBOOK – BOOKING REPORT                        ║");
                pw.println("║  Generated: " + LocalDateTime.now().format(FMT) +
                           "                              ║");
                pw.println("╚" + "═".repeat(58) + "╝");
                pw.println();
                pw.printf("  %-10s | %-20s | %-8s | %-6s | %s%n",
                        "TICKET", "PASSENGER", "FLIGHT", "SEAT", "PRICE");
                pw.println("  " + "─".repeat(58));

                int confirmed = 0;
                double totalRevenue = 0;

                for (Ticket t : tickets) {
                    pw.printf("  %-10s | %-20s | %-8s | %-6s | $%-10.2f  [%s]%n",
                            t.getId(), t.getPassengerName(),
                            t.getFlightId(), t.getSeatNumber(),
                            t.getPricePaid(), t.getStatus());
                    if (t.isConfirmed()) { confirmed++; totalRevenue += t.getPricePaid(); }
                }

                pw.println();
                pw.println("─".repeat(60));
                pw.printf("  Total Tickets   : %d%n", tickets.size());
                pw.printf("  Confirmed       : %d%n", confirmed);
                pw.printf("  Cancelled       : %d%n", tickets.size() - confirmed);
                pw.printf("  Total Revenue   : $%.2f%n", totalRevenue);
                pw.println("─".repeat(60));
            }
            System.out.println("[PdfService] Report saved → " + outFile.getAbsolutePath());
            return outFile.getAbsolutePath();
        } catch (IOException e) {
            System.err.println("[PdfService] Failed to generate report: " + e.getMessage());
            return null;
        }
    }

    // ─── BOARDING PASS ────────────────────────────────────────────────────────

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