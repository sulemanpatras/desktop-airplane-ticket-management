package skybook.services;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import skybook.models.Flight;
import skybook.models.Ticket;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * PdfService: Generates real PDF boarding passes and reports using iText7.
 */
public class PdfService {

    private static final String DEFAULT_DIR = "skybook_data/tickets";
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // Brand colors matching SkyBook dark UI
    private static final DeviceRgb COLOR_DARK_BG   = new DeviceRgb(0x0f, 0x17, 0x2a);
    private static final DeviceRgb COLOR_CARD_BG    = new DeviceRgb(0x1e, 0x29, 0x3b);
    private static final DeviceRgb COLOR_ACCENT     = new DeviceRgb(0x38, 0xbd, 0xf8); // sky blue
    private static final DeviceRgb COLOR_GREEN      = new DeviceRgb(0x34, 0xd3, 0x99);
    private static final DeviceRgb COLOR_MUTED      = new DeviceRgb(0x94, 0xa3, 0xb8);
    private static final DeviceRgb COLOR_WHITE      = new DeviceRgb(0xf1, 0xf5, 0xf9);
    private static final DeviceRgb COLOR_PURPLE     = new DeviceRgb(0xa7, 0x8b, 0xfa);
    private static final DeviceRgb COLOR_RED        = new DeviceRgb(0xf8, 0x71, 0x71);

    public PdfService() {
        new File(DEFAULT_DIR).mkdirs();
    }

    // ─── TICKET PDF ──────────────────────────────────────────────────────────

    /**
     * Shows a FileChooser and saves a real PDF boarding pass.
     * Returns the saved path, or null if cancelled.
     */
    public String generateTicketPdf(Ticket ticket, Flight flight, Window owner) {
        String defaultName = "Ticket_" + ticket.getId() + ".pdf";
        File chosen = showSaveDialog(owner, defaultName, "PDF Files (*.pdf)", "*.pdf");
        if (chosen == null) {
            chosen = new File(DEFAULT_DIR + "/" + defaultName);
        }
        return writeTicketPdf(ticket, flight, chosen);
    }

    /**
     * Headless version — saves to default directory, no dialog.
     */
    public String generateTicketPdf(Ticket ticket, Flight flight) {
        File out = new File(DEFAULT_DIR + "/Ticket_" + ticket.getId() + ".pdf");
        return writeTicketPdf(ticket, flight, out);
    }

    // ─── REPORT PDF ──────────────────────────────────────────────────────────

    public String generateBookingReport(List<Ticket> tickets, List<Flight> flights, Window owner) {
        String defaultName = "SkyBook_Report_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".pdf";
        File chosen = showSaveDialog(owner, defaultName, "PDF Files (*.pdf)", "*.pdf");
        if (chosen == null) {
            chosen = new File(DEFAULT_DIR + "/" + defaultName);
        }
        return writeReportPdf(tickets, flights, chosen);
    }

    public String generateBookingReport(List<Ticket> tickets, List<Flight> flights) {
        String defaultName = "SkyBook_Report_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".pdf";
        File out = new File(DEFAULT_DIR + "/" + defaultName);
        return writeReportPdf(tickets, flights, out);
    }

    // ─── FILE CHOOSER ────────────────────────────────────────────────────────

    public static File showSaveDialog(Window owner, String defaultName,
                                      String description, String extension) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save File – SkyBook");
        chooser.setInitialFileName(defaultName);

        File downloads = new File(System.getProperty("user.home") + "/Downloads");
        chooser.setInitialDirectory(downloads.exists() ? downloads
                : new File(System.getProperty("user.home")));

        chooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter(description, extension),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        return chooser.showSaveDialog(owner);
    }

    // ─── WRITE TICKET PDF ────────────────────────────────────────────────────

    private String writeTicketPdf(Ticket ticket, Flight flight, File outFile) {
        try {
            outFile.getParentFile().mkdirs();

            PdfWriter   writer  = new PdfWriter(outFile);
            PdfDocument pdfDoc  = new PdfDocument(writer);
            Document    doc     = new Document(pdfDoc, PageSize.A5.rotate());
            doc.setMargins(24, 24, 24, 24);

            PdfFont bold    = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont mono    = PdfFontFactory.createFont(StandardFonts.COURIER_BOLD);

            // ── Header bar ────────────────────────────────────────────────
            Table header = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                    .useAllAvailableWidth();

            Cell logoCell = new Cell()
                    .add(new Paragraph("✈  SKYBOOK AIRLINES")
                            .setFont(bold).setFontSize(18).setFontColor(COLOR_ACCENT))
                    .add(new Paragraph("Boarding Pass")
                            .setFont(regular).setFontSize(10).setFontColor(COLOR_MUTED))
                    .setBackgroundColor(COLOR_DARK_BG)
                    .setBorder(Border.NO_BORDER)
                    .setPadding(12);

            String statusText  = ticket.isConfirmed() ? "CONFIRMED" : "CANCELLED";
            DeviceRgb statusClr = ticket.isConfirmed() ? COLOR_GREEN : COLOR_RED;

            Cell statusCell = new Cell()
                    .add(new Paragraph(statusText)
                            .setFont(bold).setFontSize(22).setFontColor(statusClr)
                            .setTextAlignment(TextAlignment.RIGHT))
                    .add(new Paragraph(ticket.getId())
                            .setFont(mono).setFontSize(11).setFontColor(COLOR_MUTED)
                            .setTextAlignment(TextAlignment.RIGHT))
                    .setBackgroundColor(COLOR_DARK_BG)
                    .setBorder(Border.NO_BORDER)
                    .setPadding(12);

            header.addCell(logoCell).addCell(statusCell);
            doc.add(header);

            // ── Route row ─────────────────────────────────────────────────
            Table routeTable = new Table(UnitValue.createPercentArray(new float[]{2, 1, 2}))
                    .useAllAvailableWidth()
                    .setMarginTop(8);

            Cell fromCell = new Cell()
                    .add(new Paragraph(flight != null ? flight.getSource() : "—")
                            .setFont(bold).setFontSize(28).setFontColor(COLOR_WHITE))
                    .add(new Paragraph("ORIGIN")
                            .setFont(regular).setFontSize(9).setFontColor(COLOR_MUTED))
                    .setBackgroundColor(COLOR_CARD_BG)
                    .setBorder(Border.NO_BORDER)
                    .setPadding(14);

            Cell arrowCell = new Cell()
                    .add(new Paragraph("✈")
                            .setFont(bold).setFontSize(24).setFontColor(COLOR_ACCENT)
                            .setTextAlignment(TextAlignment.CENTER))
                    .setBackgroundColor(COLOR_CARD_BG)
                    .setBorder(Border.NO_BORDER)
                    .setPaddingTop(18)
                    .setTextAlignment(TextAlignment.CENTER);

            Cell toCell = new Cell()
                    .add(new Paragraph(flight != null ? flight.getDestination() : "—")
                            .setFont(bold).setFontSize(28).setFontColor(COLOR_WHITE)
                            .setTextAlignment(TextAlignment.RIGHT))
                    .add(new Paragraph("DESTINATION")
                            .setFont(regular).setFontSize(9).setFontColor(COLOR_MUTED)
                            .setTextAlignment(TextAlignment.RIGHT))
                    .setBackgroundColor(COLOR_CARD_BG)
                    .setBorder(Border.NO_BORDER)
                    .setPadding(14);

            routeTable.addCell(fromCell).addCell(arrowCell).addCell(toCell);
            doc.add(routeTable);

            // ── Details grid ──────────────────────────────────────────────
            Table details = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1, 1}))
                    .useAllAvailableWidth()
                    .setMarginTop(6);

            addDetailCell(details, "PASSENGER",  ticket.getPassengerName(),   bold, regular);
            addDetailCell(details, "EMAIL",      ticket.getPassengerEmail(),  bold, regular);
            addDetailCell(details, "SEAT",       ticket.getSeatNumber(),      bold, regular);
            addDetailCell(details, "PRICE",
                    String.format("PKR %.2f", ticket.getPricePaid()),         bold, regular);

            if (flight != null) {
                addDetailCell(details, "AIRLINE",    flight.getAirline(),         bold, regular);
                addDetailCell(details, "FLIGHT",     flight.getId(),              bold, regular);
                addDetailCell(details, "DEPARTURE",  flight.getDepartureTime(),   bold, regular);
                addDetailCell(details, "ARRIVAL",    flight.getArrivalTime(),     bold, regular);
            }

            doc.add(details);

            // ── Footer ────────────────────────────────────────────────────
            doc.add(new Paragraph("Booked: " + ticket.getBookedAt() +
                    "   |   Thank you for flying SkyBook Airlines!")
                    .setFont(regular).setFontSize(8).setFontColor(COLOR_MUTED)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(10)
                    .setBorderTop(new SolidBorder(COLOR_MUTED, 0.5f))
                    .setPaddingTop(6));

            doc.close();
            System.out.println("[PdfService] PDF saved → " + outFile.getAbsolutePath());
            return outFile.getAbsolutePath();

        } catch (Exception e) {
            System.err.println("[PdfService] Failed to write PDF: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private void addDetailCell(Table table, String label, String value,
                                PdfFont bold, PdfFont regular) {
        Cell cell = new Cell()
                .add(new Paragraph(label)
                        .setFont(regular).setFontSize(8).setFontColor(COLOR_MUTED))
                .add(new Paragraph(value != null ? value : "—")
                        .setFont(bold).setFontSize(11).setFontColor(COLOR_WHITE))
                .setBackgroundColor(COLOR_CARD_BG)
                .setBorder(Border.NO_BORDER)
                .setBorderLeft(new SolidBorder(COLOR_ACCENT, 2))
                .setMargin(3)
                .setPadding(8);
        table.addCell(cell);
    }

    // ─── WRITE REPORT PDF ────────────────────────────────────────────────────

    private String writeReportPdf(List<Ticket> tickets, List<Flight> flights, File outFile) {
        try {
            outFile.getParentFile().mkdirs();

            PdfWriter   writer = new PdfWriter(outFile);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document    doc    = new Document(pdfDoc, PageSize.A4);
            doc.setMargins(32, 32, 32, 32);

            PdfFont bold    = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont mono    = PdfFontFactory.createFont(StandardFonts.COURIER);

            // Title
            doc.add(new Paragraph("✈  SkyBook Airlines")
                    .setFont(bold).setFontSize(22).setFontColor(COLOR_ACCENT));
            doc.add(new Paragraph("Booking Report  —  Generated: " +
                    LocalDateTime.now().format(FMT))
                    .setFont(regular).setFontSize(10).setFontColor(COLOR_MUTED)
                    .setMarginBottom(16));

            // Table header
            Table table = new Table(UnitValue.createPercentArray(
                    new float[]{2f, 3f, 2f, 1.5f, 2f, 2f}))
                    .useAllAvailableWidth();

            for (String h : new String[]{"TICKET", "PASSENGER", "FLIGHT", "SEAT", "PRICE", "STATUS"}) {
                table.addHeaderCell(new Cell()
                        .add(new Paragraph(h).setFont(bold).setFontSize(9).setFontColor(COLOR_WHITE))
                        .setBackgroundColor(COLOR_CARD_BG)
                        .setBorder(Border.NO_BORDER)
                        .setPadding(6));
            }

            int confirmed = 0;
            double totalRevenue = 0;

            for (Ticket t : tickets) {
                DeviceRgb rowBg = t.isConfirmed()
                        ? new DeviceRgb(0x1e, 0x29, 0x3b)
                        : new DeviceRgb(0x1a, 0x1a, 0x2e);
                DeviceRgb stClr = t.isConfirmed() ? COLOR_GREEN : COLOR_RED;

                table.addCell(ticketCell(t.getId(),            mono,    COLOR_ACCENT, rowBg));
                table.addCell(ticketCell(t.getPassengerName(), regular, COLOR_WHITE,  rowBg));
                table.addCell(ticketCell(t.getFlightId(),      mono,    COLOR_MUTED,  rowBg));
                table.addCell(ticketCell(t.getSeatNumber(),    regular, COLOR_WHITE,  rowBg));
                table.addCell(ticketCell(
                        String.format("PKR %.0f", t.getPricePaid()), bold, COLOR_ACCENT, rowBg));
                table.addCell(ticketCell(t.getStatus().toString(), bold, stClr, rowBg));

                if (t.isConfirmed()) { confirmed++; totalRevenue += t.getPricePaid(); }
            }

            doc.add(table);

            // Summary
            doc.add(new Paragraph(
                    "Total: " + tickets.size() + "  |  Confirmed: " + confirmed +
                    "  |  Cancelled: " + (tickets.size() - confirmed) +
                    "  |  Revenue: PKR " + String.format("%,.2f", totalRevenue))
                    .setFont(bold).setFontSize(11).setFontColor(COLOR_GREEN)
                    .setMarginTop(14)
                    .setBorderTop(new SolidBorder(COLOR_MUTED, 0.5f))
                    .setPaddingTop(8));

            doc.close();
            System.out.println("[PdfService] Report saved → " + outFile.getAbsolutePath());
            return outFile.getAbsolutePath();

        } catch (Exception e) {
            System.err.println("[PdfService] Failed to write report: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private Cell ticketCell(String text, PdfFont font, DeviceRgb color, DeviceRgb bg) {
        return new Cell()
                .add(new Paragraph(text != null ? text : "—")
                        .setFont(font).setFontSize(9).setFontColor(color))
                .setBackgroundColor(bg)
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(new DeviceRgb(0x33, 0x41, 0x55), 0.5f))
                .setPadding(5);
    }
}