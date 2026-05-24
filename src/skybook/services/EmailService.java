package skybook.services;

import skybook.models.Flight;
import skybook.models.Ticket;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * EmailService: Simulates sending emails by writing to an email log file.
 * In a real app this would use JavaMail API (javax.mail).
 * Demonstrates: File Handling, Service layer
 */
public class EmailService {

    private static final String EMAIL_LOG = "skybook_data/email_log.txt";
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public EmailService() {
        // Ensure directory exists
        new File("skybook_data").mkdirs();
    }

    /**
     * Sends a booking confirmation email (simulated).
     */
    public void sendBookingConfirmation(Ticket ticket, Flight flight) {
        String subject = "✈ Booking Confirmed – " + ticket.getId();

        String body = buildEmailBody(
            ticket.getPassengerName(),
            "Your booking is confirmed! Here are your details:\n\n" +
            ticket.getReceiptText(flight) +
            "\nThank you for flying with SkyBook!\n" +
            "For support, reply to this email or call +92-21-SKYBOOK.\n"
        );

        logEmail(ticket.getPassengerEmail(), subject, body);
        System.out.println("[EmailService] Confirmation sent to: " + ticket.getPassengerEmail());
    }

    /**
     * Sends a cancellation notice email (simulated).
     */
    public void sendCancellationNotice(Ticket ticket) {
        String subject = "Ticket Cancelled – " + ticket.getId();

        String body = buildEmailBody(
            ticket.getPassengerName(),
            "Your ticket " + ticket.getId() + " has been cancelled.\n\n" +
            "Flight: " + ticket.getFlightId() + "\n" +
            "Seat: " + ticket.getSeatNumber() + "\n" +
            "Refund (if applicable) will be processed in 5-7 business days.\n\n" +
            "We hope to see you fly with SkyBook again soon!\n"
        );

        logEmail(ticket.getPassengerEmail(), subject, body);
        System.out.println("[EmailService] Cancellation notice sent to: " + ticket.getPassengerEmail());
    }

    /**
     * Sends a flight status update email.
     */
    public void sendFlightStatusUpdate(String toEmail, String passengerName,
                                       Flight flight, String oldStatus) {
        String subject = "Flight Update – " + flight.getId() + " is now " + flight.getStatus();

        String body = buildEmailBody(
            passengerName,
            "This is an update regarding your upcoming flight:\n\n" +
            "Flight   : " + flight.getId() + "\n" +
            "Route    : " + flight.getRouteDisplay() + "\n" +
            "Status   : " + oldStatus + " → " + flight.getStatus() + "\n" +
            "Departure: " + flight.getDepartureTime() + "\n\n" +
            "We apologize for any inconvenience.\n"
        );

        logEmail(toEmail, subject, body);
        System.out.println("[EmailService] Status update sent to: " + toEmail);
    }

    // ─── PRIVATE HELPERS ────────────────────────────────────────────────────────

    private String buildEmailBody(String recipientName, String content) {
        return  "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "FROM    : noreply@skybook.com\n" +
                "TO      : " + recipientName + "\n" +
                "DATE    : " + LocalDateTime.now().format(FMT) + "\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                "Dear " + recipientName + ",\n\n" +
                content +
                "\n\nBest regards,\nThe SkyBook Team\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n";
    }

    private void logEmail(String toEmail, String subject, String body) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(EMAIL_LOG, true))) {
            pw.println("═".repeat(60));
            pw.println("TO      : " + toEmail);
            pw.println("SUBJECT : " + subject);
            pw.println("SENT AT : " + LocalDateTime.now().format(FMT));
            pw.println("─".repeat(60));
            pw.println(body);
            pw.println();
        } catch (IOException e) {
            System.err.println("[EmailService] Failed to log email: " + e.getMessage());
        }
    }
}
