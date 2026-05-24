package skybook.services;

import skybook.models.Flight;
import skybook.models.Ticket;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * EmailService — ZERO external dependencies.
 *
 * Sends HTML email over raw SMTP (port 587, STARTTLS) using only JDK classes.
 * No third-party jar (javax.mail / jakarta.mail) required.
 *
 * All emails are also logged to skybook_data/email_log.txt regardless of
 * whether the send succeeds.
 */
public class EmailService {

    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final int    SMTP_PORT = 587;
    private static final String USERNAME  = "sulemanpatras2@gmail.com";
    private static final String PASSWORD  = "mouf zvtb uvxu yjjy";   // Gmail App Password
    private static final String FROM_NAME = "SkyBook Airlines";

    private static final String LOG_FILE = "skybook_data/email_log.txt";
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ─── PUBLIC API ───────────────────────────────────────────────────────────

    public void sendBookingConfirmation(Ticket ticket, Flight flight) {
        String subject = "✈ Booking Confirmed – " + ticket.getId();
        String body    = buildConfirmationHtml(ticket, flight);
        sendEmail(ticket.getPassengerEmail(), ticket.getPassengerName(), subject, body);
    }

    public void sendCancellationNotice(Ticket ticket) {
        String subject = "SkyBook: Booking Cancelled – " + ticket.getId();
        String body    = buildCancellationHtml(ticket);
        sendEmail(ticket.getPassengerEmail(), ticket.getPassengerName(), subject, body);
    }

    /** Sends asynchronously so the UI never freezes. */
    public void sendEmail(String toEmail, String toName, String subject, String body) {
        logEmail(toEmail, subject, body);
        Thread t = new Thread(() -> {
            try {
                sendViaSMTP(toEmail, subject, body);
                System.out.println("[EmailService] ✓ Sent to " + toEmail);
            } catch (Exception e) {
                System.err.println("[EmailService] ✗ " + toEmail + " → " + e.getMessage());
            }
        });
        t.setName("skybook-email");
        t.setDaemon(true);
        t.start();
    }

    // ─── RAW SMTP / STARTTLS ─────────────────────────────────────────────────

    private void sendViaSMTP(String toEmail, String subject, String htmlBody) throws Exception {
        // Step 1: plain TCP connection on port 587
        Socket plain = new Socket();
        plain.connect(new InetSocketAddress(SMTP_HOST, SMTP_PORT), 10_000);
        plain.setSoTimeout(15_000);

        BufferedReader in  = mkReader(plain.getInputStream());
        PrintWriter    out = mkWriter(plain.getOutputStream());

        expect(in, "220");                          // greeting
        smtp(out, "EHLO skybook.local");
        swallowMultiline(in);                       // capabilities list

        // Step 2: upgrade to TLS
        smtp(out, "STARTTLS");
        expect(in, "220");

        javax.net.ssl.SSLSocketFactory ssf =
                (javax.net.ssl.SSLSocketFactory) javax.net.ssl.SSLSocketFactory.getDefault();
        javax.net.ssl.SSLSocket ssl =
                (javax.net.ssl.SSLSocket) ssf.createSocket(plain, SMTP_HOST, SMTP_PORT, true);
        ssl.startHandshake();

        in  = mkReader(ssl.getInputStream());
        out = mkWriter(ssl.getOutputStream());

        smtp(out, "EHLO skybook.local");
        swallowMultiline(in);

        // Step 3: AUTH LOGIN (Base64-encoded credentials)
        smtp(out, "AUTH LOGIN");
        expect(in, "334");
        smtp(out, b64(USERNAME));
        expect(in, "334");
        smtp(out, b64(PASSWORD));
        expect(in, "235");                          // authenticated

        // Step 4: envelope
        smtp(out, "MAIL FROM:<" + USERNAME + ">");
        expect(in, "250");
        smtp(out, "RCPT TO:<" + toEmail + ">");
        expect(in, "250");

        // Step 5: DATA
        smtp(out, "DATA");
        expect(in, "354");

        String boundary = "=_SkyBook_" + System.currentTimeMillis();

        out.print("From: =?UTF-8?B?" + b64(FROM_NAME) + "?= <" + USERNAME + ">\r\n");
        out.print("To: " + toEmail + "\r\n");
        out.print("Subject: " + subject + "\r\n");
        out.print("MIME-Version: 1.0\r\n");
        out.print("Content-Type: multipart/alternative; boundary=\"" + boundary + "\"\r\n");
        out.print("\r\n");

        // Plain-text part
        out.print("--" + boundary + "\r\n");
        out.print("Content-Type: text/plain; charset=UTF-8\r\n\r\n");
        String plain2 = htmlBody.replaceAll("<[^>]+>", "").replaceAll(" +", " ").trim();
        out.print(plain2 + "\r\n\r\n");

        // HTML part
        out.print("--" + boundary + "\r\n");
        out.print("Content-Type: text/html; charset=UTF-8\r\n\r\n");
        out.print(wrapHtml(htmlBody) + "\r\n\r\n");
        out.print("--" + boundary + "--\r\n");

        // End DATA
        out.print(".\r\n");
        out.flush();
        expect(in, "250");

        smtp(out, "QUIT");
        try { ssl.close(); } catch (Exception ignored) {}
        try { plain.close(); } catch (Exception ignored) {}
    }

    // ─── SMTP UTILS ──────────────────────────────────────────────────────────

    private void smtp(PrintWriter out, String cmd) {
        out.print(cmd + "\r\n");
        out.flush();
    }

    /** Reads one line and asserts it starts with the expected code. */
    private void expect(BufferedReader in, String code) throws Exception {
        String line = in.readLine();
        if (line == null || !line.startsWith(code))
            throw new Exception("SMTP expected " + code + ", got: " + line);
    }

    /** Reads a multi-line SMTP response until a line without '-' at position 3. */
    private void swallowMultiline(BufferedReader in) throws Exception {
        String line;
        do { line = in.readLine(); }
        while (line != null && line.length() > 3 && line.charAt(3) == '-');
    }

    private String b64(String s) {
        return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    private BufferedReader mkReader(InputStream is) {
        return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
    }

    private PrintWriter mkWriter(OutputStream os) {
        return new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8), false);
    }

    // ─── EMAIL BODIES ────────────────────────────────────────────────────────

    private String buildConfirmationHtml(Ticket ticket, Flight flight) {
        return "<h2 style='color:#38bdf8'>Your Booking is Confirmed!</h2>"
             + "<table style='border-collapse:collapse;width:100%;font-family:monospace'>"
             + row("Ticket ID",  ticket.getId())
             + row("Passenger",  ticket.getPassengerName())
             + row("Flight",     ticket.getFlightId())
             + row("Seat",       ticket.getSeatNumber())
             + row("Price Paid", String.format("$%.2f", ticket.getPricePaid()))
             + row("Booked At",  ticket.getBookedAt())
             + (flight != null ? row("Route",     flight.getSource() + " → " + flight.getDestination()) : "")
             + (flight != null ? row("Departure", flight.getDepartureTime()) : "")
             + (flight != null ? row("Arrival",   flight.getArrivalTime())   : "")
             + "</table>"
             + "<p>Thank you for choosing SkyBook Airlines. Have a safe flight! ✈</p>";
    }

    private String buildCancellationHtml(Ticket ticket) {
        return "<h2 style='color:#f87171'>Booking Cancelled</h2>"
             + "<p>Your booking <strong>" + ticket.getId() + "</strong> has been cancelled.</p>"
             + "<table style='border-collapse:collapse;width:100%;font-family:monospace'>"
             + row("Ticket ID", ticket.getId())
             + row("Passenger", ticket.getPassengerName())
             + row("Flight",    ticket.getFlightId())
             + row("Seat",      ticket.getSeatNumber())
             + "</table>"
             + "<p>We hope to see you again on SkyBook Airlines.</p>";
    }

    private String row(String label, String value) {
        return "<tr>"
             + "<td style='padding:6px 12px;border:1px solid #334155;color:#94a3b8'><b>" + label + "</b></td>"
             + "<td style='padding:6px 12px;border:1px solid #334155;color:#f1f5f9'>" + (value != null ? value : "") + "</td>"
             + "</tr>";
    }

    private String wrapHtml(String body) {
        return "<!DOCTYPE html><html><body style='"
             + "background:#0f172a;color:#f1f5f9;font-family:Arial,sans-serif;padding:32px'>"
             + "<div style='max-width:560px;margin:0 auto;"
             + "background:#1e293b;border-radius:12px;padding:32px'>"
             + "<div style='font-size:24px;font-weight:bold;color:#38bdf8;margin-bottom:8px'>✈ SkyBook</div>"
             + "<div style='color:#64748b;margin-bottom:24px'>Airline Ticket Management</div>"
             + body
             + "</div></body></html>";
    }

    // ─── LOG ─────────────────────────────────────────────────────────────────

    private void logEmail(String to, String subject, String body) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            pw.println("──────────────────────────────────────────");
            pw.println("Time    : " + LocalDateTime.now().format(FMT));
            pw.println("To      : " + to);
            pw.println("Subject : " + subject);
            String preview = body.replaceAll("<[^>]+>", "").replaceAll("\\s+", " ").trim();
            pw.println("Preview : " + preview.substring(0, Math.min(100, preview.length())));
            pw.println();
        } catch (IOException e) {
            System.err.println("[EmailService] Log error: " + e.getMessage());
        }
    }
}