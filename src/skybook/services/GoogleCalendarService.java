package skybook.services;

import skybook.models.Flight;
import skybook.models.Ticket;

import java.awt.Desktop;
import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * GoogleCalendarService: Adds flight bookings to Google Calendar.
 *
 * APPROACH A (used here — zero OAuth complexity):
 *   Build a "Add to Google Calendar" URL and open it in the system browser.
 *   The user clicks "Save" in their browser — no API key needed.
 *
 * APPROACH B (full OAuth, optional — see comments at bottom):
 *   Use the Google Calendar Java API client library for server-side event creation.
 *   Requires: google-api-services-calendar, google-auth-library-oauth2-http.
 *
 * Demonstrates: URL building, Desktop API, Exception Handling, Service layer
 */
public class GoogleCalendarService {

    // Google Calendar event URL format
    private static final String GCal_BASE =
            "https://calendar.google.com/calendar/render?action=TEMPLATE";

    // Formatter for Google Calendar date-time: YYYYMMDDTHHmmssZ
    private static final DateTimeFormatter GCal_FMT =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

    // Formatter for parsing flight departure time from DataStore
    private static final DateTimeFormatter FLIGHT_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * Opens the user's browser with a pre-filled "Add to Google Calendar" form.
     * The user only needs to click "Save" — no login required in the app.
     *
     * @param ticket the booked ticket
     * @param flight the associated flight
     */
    public void addFlightToCalendar(Ticket ticket, Flight flight) {
        try {
            String url = buildCalendarUrl(ticket, flight);
            System.out.println("[GoogleCalendarService] Opening: " + url);

            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                // Fallback for Linux/headless: print URL
                System.out.println("[GoogleCalendarService] Open this URL in your browser:\n" + url);
            }

        } catch (Exception e) {
            System.err.println("[GoogleCalendarService] Could not open calendar: " + e.getMessage());
        }
    }

    /**
     * Builds the Google Calendar "Add Event" URL from ticket + flight data.
     */
    private String buildCalendarUrl(Ticket ticket, Flight flight) throws Exception {
        String title   = "✈ " + flight.getSource() + " → " + flight.getDestination()
                       + " | " + flight.getAirline();

        String details = "SkyBook Booking Confirmation\n"
                       + "Ticket ID : " + ticket.getId() + "\n"
                       + "Passenger : " + ticket.getPassengerName() + "\n"
                       + "Flight    : " + flight.getId() + "\n"
                       + "Seat      : " + ticket.getSeatNumber() + "\n"
                       + "Price     : $" + String.format("%.2f", ticket.getPricePaid()) + "\n"
                       + "Arrival   : " + flight.getArrivalTime();

        String location = flight.getSource() + " Airport → " + flight.getDestination() + " Airport";

        // Parse departure/arrival times
        String startDt = formatForGCal(flight.getDepartureTime());
        String endDt   = formatForGCal(flight.getArrivalTime());

        return GCal_BASE
             + "&text="     + encode(title)
             + "&dates="    + startDt + "/" + endDt
             + "&details="  + encode(details)
             + "&location=" + encode(location)
             + "&sf=true"
             + "&output=xml";
    }

    /**
     * Converts "yyyy-MM-dd HH:mm" or "yyyy-MM-ddTHH:mm" to GCal format "YYYYMMDDTHHmmss".
     */
    private String formatForGCal(String dateTimeStr) {
        try {
            // Handle both "2025-06-10 08:00" and "2025-06-10T08:00"
            dateTimeStr = dateTimeStr.replace("T", " ");
            if (dateTimeStr.length() == 16) dateTimeStr += ":00";
            LocalDateTime ldt = LocalDateTime.parse(dateTimeStr, FLIGHT_FMT);
            return ldt.format(GCal_FMT);
        } catch (Exception e) {
            // Fallback: today + 0 hours
            return LocalDateTime.now().format(GCal_FMT);
        }
    }

    private String encode(String value) throws Exception {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // APPROACH B: Full Google Calendar API (OAuth2) — OPTIONAL ADVANCED SETUP
    // ─────────────────────────────────────────────────────────────────────────
    //
    // If you want server-side calendar event creation without a browser:
    //
    // 1. Add to pom.xml / classpath:
    //    com.google.apis:google-api-services-calendar:v3-rev20230703-2.0.0
    //    com.google.auth:google-auth-library-oauth2-http:1.19.0
    //
    // 2. Create a GCP project → Enable Calendar API → Create OAuth2 credentials
    //    Download credentials.json → place in skybook_data/google_credentials.json
    //
    // 3. Uncomment and use the method below:
    //
    // private static final String TOKENS_DIR = "skybook_data/google_tokens";
    // private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    // private static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR);
    //
    // public void addEventViaApi(Ticket ticket, Flight flight) throws Exception {
    //     NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
    //     GoogleClientSecrets secrets = GoogleClientSecrets.load(JSON_FACTORY,
    //         new FileReader("skybook_data/google_credentials.json"));
    //
    //     GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
    //         transport, JSON_FACTORY, secrets, SCOPES)
    //         .setDataStoreFactory(new FileDataStoreFactory(new File(TOKENS_DIR)))
    //         .setAccessType("offline").build();
    //
    //     Credential cred = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
    //
    //     Calendar service = new Calendar.Builder(transport, JSON_FACTORY, cred)
    //         .setApplicationName("SkyBook").build();
    //
    //     EventDateTime start = new EventDateTime()
    //         .setDateTime(new com.google.api.client.util.DateTime(flight.getDepartureTime()))
    //         .setTimeZone("Asia/Karachi");
    //
    //     Event event = new Event()
    //         .setSummary("✈ " + flight.getSource() + " → " + flight.getDestination())
    //         .setDescription("Ticket: " + ticket.getId())
    //         .setLocation(flight.getSource() + " Airport")
    //         .setStart(start)
    //         .setEnd(new EventDateTime().setDateTime(...).setTimeZone("Asia/Karachi"));
    //
    //     service.events().insert("primary", event).execute();
    // }
}