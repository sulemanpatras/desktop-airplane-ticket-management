package skybook.services;

import skybook.models.Flight;
import skybook.models.Ticket;
import skybook.models.User;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;

/**
 * EmailService — sends HTML emails over raw SMTP (STARTTLS, port 587).
 *
 * FIX 3: sendBookingConfirmation now fires to BOTH passenger AND admin.
 * FIX 4: sendCancellationNotice fires to BOTH passenger AND admin with role-appropriate bodies.
 * FIX 5: sendProfileUpdateNotification fires to user ONLY.
 * FIX 7: sendAdminUserCreated fires to new user ONLY.
 */
public class EmailService {

    private static final String SMTP_HOST  = "smtp.gmail.com";
    private static final int    SMTP_PORT  = 587;
    private static final String USERNAME   = "sulemanpatras2@gmail.com";
    private static final String PASSWORD   = "mouf zvtb uvxu yjjy";
    private static final String FROM_NAME  = "SkyBook Airlines";
    private static final String ADMIN_EMAIL = "admin@skybook.com";   // FIX 3/4: admin recipient

    private static final String LOG_FILE = "skybook_data/email_log.txt";
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ─── PUBLIC API ───────────────────────────────────────────────────────────

    /**
     * FIX 3: Sends booking confirmation to the passenger AND a booking-alert to admin.
     */
    public void sendBookingConfirmation(Ticket ticket, Flight flight) {
        // Email to passenger
        String passengerSubject = "✈ Booking Confirmed – " + ticket.getId();
        String passengerBody    = buildConfirmationHtml(ticket, flight);
        sendEmail(ticket.getPassengerEmail(), ticket.getPassengerName(), passengerSubject, passengerBody);

        // Email to admin
        String adminSubject = "New Booking Alert – " + ticket.getId() + " by " + ticket.getPassengerName();
        String adminBody    = buildAdminBookingAlertHtml(ticket, flight);
        sendEmail(ADMIN_EMAIL, "SkyBook Admin", adminSubject, adminBody);
    }

    /**
     * FIX 4: Sends cancellation notices to BOTH passenger AND admin.
     */
    public void sendCancellationNotice(Ticket ticket) {
        // Passenger: "Your ticket is cancelled"
        String passengerSubject = "SkyBook: Booking Cancelled – " + ticket.getId();
        String passengerBody    = buildPassengerCancellationHtml(ticket);
        sendEmail(ticket.getPassengerEmail(), ticket.getPassengerName(), passengerSubject, passengerBody);

        // Admin: "A passenger cancelled their booking"
        String adminSubject = "Cancellation Alert – " + ticket.getPassengerName() + " cancelled " + ticket.getId();
        String adminBody    = buildAdminCancellationHtml(ticket);
        sendEmail(ADMIN_EMAIL, "SkyBook Admin", adminSubject, adminBody);
    }

    /**
     * FIX 5: Profile update confirmation — sent to user ONLY.
     */
    public void sendProfileUpdateNotification(User user, List<String> changes) {
        String subject = "SkyBook: Your profile has been updated";
        String body    = buildProfileUpdateHtml(user, changes);
        sendEmail(user.getEmail(), user.getFullName(), subject, body);
    }

    /**
     * FIX 7: Admin created/updated a user — send welcome/update email to that user only.
     */
    public void sendAdminUserCreated(User user, String temporaryPassword) {
        String subject = "Welcome to SkyBook – Your account is ready";
        String body    = buildAdminCreatedUserHtml(user, temporaryPassword);
        sendEmail(user.getEmail(), user.getFullName(), subject, body);
    }

    /** Generic async send. */
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
        Socket plain = new Socket();
        plain.connect(new InetSocketAddress(SMTP_HOST, SMTP_PORT), 10_000);
        plain.setSoTimeout(15_000);

        BufferedReader in  = mkReader(plain.getInputStream());
        PrintWriter    out = mkWriter(plain.getOutputStream());

        expect(in, "220");
        smtp(out, "EHLO skybook.local");
        swallowMultiline(in);

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

        smtp(out, "AUTH LOGIN");
        expect(in, "334");
        smtp(out, b64(USERNAME));
        expect(in, "334");
        smtp(out, b64(PASSWORD));
        expect(in, "235");

        smtp(out, "MAIL FROM:<" + USERNAME + ">");
        expect(in, "250");
        smtp(out, "RCPT TO:<" + toEmail + ">");
        expect(in, "250");

        smtp(out, "DATA");
        expect(in, "354");

        String boundary = "=_SkyBook_" + System.currentTimeMillis();

        out.print("From: =?UTF-8?B?" + b64(FROM_NAME) + "?= <" + USERNAME + ">\r\n");
        out.print("To: " + toEmail + "\r\n");
        out.print("Subject: " + subject + "\r\n");
        out.print("MIME-Version: 1.0\r\n");
        out.print("Content-Type: multipart/alternative; boundary=\"" + boundary + "\"\r\n");
        out.print("\r\n");

        out.print("--" + boundary + "\r\n");
        out.print("Content-Type: text/plain; charset=UTF-8\r\n\r\n");
        String plainText = htmlBody.replaceAll("<[^>]+>", "").replaceAll(" +", " ").trim();
        out.print(plainText + "\r\n\r\n");

        out.print("--" + boundary + "\r\n");
        out.print("Content-Type: text/html; charset=UTF-8\r\n\r\n");
        out.print(wrapHtml(htmlBody) + "\r\n\r\n");
        out.print("--" + boundary + "--\r\n");

        out.print(".\r\n");
        out.flush();
        expect(in, "250");

        smtp(out, "QUIT");
        try { ssl.close(); } catch (Exception ignored) {}
        try { plain.close(); } catch (Exception ignored) {}
    }

    // ─── SMTP UTILS ──────────────────────────────────────────────────────────

    private void smtp(PrintWriter out, String cmd) { out.print(cmd + "\r\n"); out.flush(); }

    private void expect(BufferedReader in, String code) throws Exception {
        String line = in.readLine();
        if (line == null || !line.startsWith(code))
            throw new Exception("SMTP expected " + code + ", got: " + line);
    }

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

    /** FIX 3 — passenger booking confirmation */
    private String buildConfirmationHtml(Ticket ticket, Flight flight) {
        return "<h2 style='color:#38bdf8'>Your Booking is Confirmed!</h2>"
             + "<p>Hello <strong>" + ticket.getPassengerName() + "</strong>, your flight has been booked successfully.</p>"
             + "<table style='border-collapse:collapse;width:100%;font-family:monospace'>"
             + row("Ticket ID",  ticket.getId())
             + row("Flight",     ticket.getFlightId())
             + row("Seat",       ticket.getSeatNumber())
             + row("Price Paid", String.format("PKR %.2f", ticket.getPricePaid()))
             + row("Booked At",  ticket.getBookedAt())
             + (flight != null ? row("Route",     flight.getSource() + " → " + flight.getDestination()) : "")
             + (flight != null ? row("Airline",   flight.getAirline()) : "")
             + (flight != null ? row("Departure", flight.getDepartureTime()) : "")
             + (flight != null ? row("Arrival",   flight.getArrivalTime())   : "")
             + "</table>"
             + "<p>Thank you for choosing SkyBook Airlines. Have a safe flight! ✈</p>";
    }

    /** FIX 3 — admin booking alert */
    private String buildAdminBookingAlertHtml(Ticket ticket, Flight flight) {
        return "<h2 style='color:#a78bfa'>New Booking Received</h2>"
             + "<p>A passenger has booked a ticket. Details below:</p>"
             + "<table style='border-collapse:collapse;width:100%;font-family:monospace'>"
             + row("Ticket ID",       ticket.getId())
             + row("Passenger Name",  ticket.getPassengerName())
             + row("Passenger Email", ticket.getPassengerEmail())
             + row("Flight",          ticket.getFlightId())
             + row("Seat",            ticket.getSeatNumber())
             + row("Price",           String.format("PKR %.2f", ticket.getPricePaid()))
             + row("Booked At",       ticket.getBookedAt())
             + (flight != null ? row("Route", flight.getSource() + " → " + flight.getDestination()) : "")
             + "</table>";
    }

    /** FIX 4 — passenger cancellation notice */
    private String buildPassengerCancellationHtml(Ticket ticket) {
        return "<h2 style='color:#f87171'>Your Ticket Has Been Cancelled</h2>"
             + "<p>Hello <strong>" + ticket.getPassengerName() + "</strong>,</p>"
             + "<p>Your booking <strong>" + ticket.getId() + "</strong> has been successfully cancelled.</p>"
             + "<table style='border-collapse:collapse;width:100%;font-family:monospace'>"
             + row("Ticket ID", ticket.getId())
             + row("Flight",    ticket.getFlightId())
             + row("Seat",      ticket.getSeatNumber())
             + "</table>"
             + "<p>We hope to see you again on SkyBook Airlines.</p>";
    }

    /** FIX 4 — admin cancellation alert */
    private String buildAdminCancellationHtml(Ticket ticket) {
        return "<h2 style='color:#f87171'>Cancellation Alert</h2>"
             + "<p>A passenger has cancelled their booking.</p>"
             + "<table style='border-collapse:collapse;width:100%;font-family:monospace'>"
             + row("Ticket ID",       ticket.getId())
             + row("Passenger Name",  ticket.getPassengerName())
             + row("Passenger Email", ticket.getPassengerEmail())
             + row("Flight",          ticket.getFlightId())
             + row("Seat",            ticket.getSeatNumber())
             + row("Price Paid",      String.format("PKR %.2f", ticket.getPricePaid()))
             + "</table>";
    }

    /** FIX 5 — profile update confirmation sent to user only */
    private String buildProfileUpdateHtml(User user, List<String> changes) {
        StringBuilder changeList = new StringBuilder("<ul>");
        for (String c : changes) changeList.append("<li>").append(c).append("</li>");
        changeList.append("</ul>");

        return "<h2 style='color:#34d399'>Profile Updated</h2>"
             + "<p>Hello <strong>" + user.getFullName() + "</strong>,</p>"
             + "<p>Your SkyBook profile has been updated. The following changes were made:</p>"
             + changeList
             + "<table style='border-collapse:collapse;width:100%;font-family:monospace'>"
             + row("Username", user.getUsername())
             + row("Email",    user.getEmail())
             + row("Name",     user.getFullName())
             + "</table>"
             + "<p>If you did not make these changes, please contact support immediately.</p>";
    }

    /** FIX 7 — admin created a new user account, notify that user */
    private String buildAdminCreatedUserHtml(User user, String temporaryPassword) {
        return "<h2 style='color:#38bdf8'>Welcome to SkyBook!</h2>"
             + "<p>Hello <strong>" + user.getFullName() + "</strong>,</p>"
             + "<p>An account has been created for you on the SkyBook system.</p>"
             + "<table style='border-collapse:collapse;width:100%;font-family:monospace'>"
             + row("Username", user.getUsername())
             + row("Email",    user.getEmail())
             + row("Role",     user.getRole().name())
             + (temporaryPassword != null ? row("Temporary Password", temporaryPassword) : "")
             + "</table>"
             + "<p>Please log in and change your password immediately.</p>"
             + "<p>Welcome aboard! ✈</p>";
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

    /**
     * FIX: Sends booking confirmation with the boarding pass PDF attached.
     */
    public void sendBookingConfirmationWithAttachment(Ticket ticket, Flight flight, String pdfPath) {
        // Email to passenger WITH attachment
        String passengerSubject = "✈ Booking Confirmed – " + ticket.getId();
        String passengerBody    = buildConfirmationHtml(ticket, flight);
        sendEmailWithAttachment(ticket.getPassengerEmail(), ticket.getPassengerName(),
                passengerSubject, passengerBody, pdfPath);

        // Email to admin (no attachment needed)
        String adminSubject = "New Booking Alert – " + ticket.getId() + " by " + ticket.getPassengerName();
        String adminBody    = buildAdminBookingAlertHtml(ticket, flight);
        sendEmail(ADMIN_EMAIL, "SkyBook Admin", adminSubject, adminBody);
    }

    /**
     * Sends an email with a single file attachment via raw SMTP + STARTTLS.
     */
    public void sendEmailWithAttachment(String toEmail, String toName,
                                        String subject, String htmlBody,
                                        String attachmentPath) {
        logEmail(toEmail, subject, htmlBody);
        Thread t = new Thread(() -> {
            try {
                sendViaSMTPWithAttachment(toEmail, subject, htmlBody, attachmentPath);
                System.out.println("[EmailService] ✓ Sent with attachment to " + toEmail);
            } catch (Exception e) {
                System.err.println("[EmailService] ✗ " + toEmail + " → " + e.getMessage());
            }
        });
        t.setName("skybook-email-attach");
        t.setDaemon(true);
        t.start();
    }

    private void sendViaSMTPWithAttachment(String toEmail, String subject,
                                            String htmlBody, String attachmentPath) throws Exception {
        Socket plain = new Socket();
        plain.connect(new InetSocketAddress(SMTP_HOST, SMTP_PORT), 10_000);
        plain.setSoTimeout(15_000);

        BufferedReader in  = mkReader(plain.getInputStream());
        PrintWriter    out = mkWriter(plain.getOutputStream());

        expect(in, "220");
        smtp(out, "EHLO skybook.local");
        swallowMultiline(in);

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

        smtp(out, "AUTH LOGIN");
        expect(in, "334");
        smtp(out, b64(USERNAME));
        expect(in, "334");
        smtp(out, b64(PASSWORD));
        expect(in, "235");

        smtp(out, "MAIL FROM:<" + USERNAME + ">");
        expect(in, "250");
        smtp(out, "RCPT TO:<" + toEmail + ">");
        expect(in, "250");

        smtp(out, "DATA");
        expect(in, "354");

        String boundary  = "=_SkyBook_Mixed_"  + System.currentTimeMillis();
        String boundary2 = "=_SkyBook_Alt_"    + System.currentTimeMillis();

        out.print("From: =?UTF-8?B?" + b64(FROM_NAME) + "?= <" + USERNAME + ">\r\n");
        out.print("To: " + toEmail + "\r\n");
        out.print("Subject: " + subject + "\r\n");
        out.print("MIME-Version: 1.0\r\n");
        out.print("Content-Type: multipart/mixed; boundary=\"" + boundary + "\"\r\n");
        out.print("\r\n");

        // ── Part 1: HTML body ────────────────────────────────────────────────
        out.print("--" + boundary + "\r\n");
        out.print("Content-Type: multipart/alternative; boundary=\"" + boundary2 + "\"\r\n\r\n");

        out.print("--" + boundary2 + "\r\n");
        out.print("Content-Type: text/plain; charset=UTF-8\r\n\r\n");
        String plain2 = htmlBody.replaceAll("<[^>]+>", "").replaceAll(" +", " ").trim();
        out.print(plain2 + "\r\n\r\n");

        out.print("--" + boundary2 + "\r\n");
        out.print("Content-Type: text/html; charset=UTF-8\r\n\r\n");
        out.print(wrapHtml(htmlBody) + "\r\n\r\n");
        out.print("--" + boundary2 + "--\r\n\r\n");

        // ── Part 2: attachment ───────────────────────────────────────────────
        if (attachmentPath != null) {
            File attachFile = new File(attachmentPath);
            if (attachFile.exists()) {
                byte[] fileBytes = java.nio.file.Files.readAllBytes(attachFile.toPath());
                String encoded   = Base64.getMimeEncoder(76, new byte[]{'\r', '\n'})
                                        .encodeToString(fileBytes);
                String fileName  = attachFile.getName();

                out.print("--" + boundary + "\r\n");
                out.print("Content-Type: text/plain; name=\"" + fileName + "\"\r\n");
                out.print("Content-Transfer-Encoding: base64\r\n");
                out.print("Content-Disposition: attachment; filename=\"" + fileName + "\"\r\n\r\n");
                out.print(encoded + "\r\n\r\n");
            }
        }

        out.print("--" + boundary + "--\r\n");
        out.print(".\r\n");
        out.flush();
        expect(in, "250");

        smtp(out, "QUIT");
        try { ssl.close();  } catch (Exception ignored) {}
        try { plain.close(); } catch (Exception ignored) {}
    }
}