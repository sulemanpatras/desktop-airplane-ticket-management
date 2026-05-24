# ✈ SkyBook — Airplane Ticket Management System

A complete **OOP Java + JavaFX** airline ticket management system built for the
IU Faculty of Engineering OOP Lab Final Project.

---

## 📁 Project Structure

```
SkyBook/
├── src/skybook/
│   ├── models/          ← OOP entity classes
│   │   ├── Person.java          (Abstract base class)
│   │   ├── Passenger.java       (Extends Person)
│   │   ├── Admin.java           (Extends Person)
│   │   ├── Flight.java
│   │   └── Ticket.java
│   ├── exceptions/      ← Custom exceptions
│   │   ├── NoSeatsAvailableException.java
│   │   ├── FlightNotFoundException.java
│   │   └── InvalidBookingException.java
│   ├── services/        ← Business logic & file I/O
│   │   ├── BookingService.java
│   │   ├── DataStore.java       (File handling)
│   │   ├── EmailService.java    (Email simulation)
│   │   └── PdfService.java      (Ticket PDF/TXT generation)
│   └── ui/              ← JavaFX screens
│       ├── MainApp.java         (Entry point)
│       ├── DashboardScreen.java (PieChart + BarChart)
│       ├── SearchFlightsScreen.java
│       ├── MyBookingsScreen.java
│       ├── ManageFlightsScreen.java
│       └── AllBookingsScreen.java
├── skybook_data/        ← Auto-created data files
│   ├── flights.csv
│   ├── tickets.csv
│   ├── passengers.csv
│   ├── admins.csv
│   ├── email_log.txt
│   └── tickets/         ← Boarding pass files
└── build_and_run.sh
```

---

## ✅ OOP Concepts Demonstrated

| Concept | Where |
|---|---|
| **Classes & Objects** | Person, Passenger, Admin, Flight, Ticket (5+ classes) |
| **Inheritance** | Person → Passenger, Person → Admin |
| **Polymorphism** | `getRole()`, `getSummary()` overridden; `getDisplayInfo()` overloaded |
| **Abstraction** | `Person` is abstract with abstract methods |
| **Encapsulation** | Private fields + getters/setters in all classes |
| **ArrayLists** | `List<Flight>`, `List<Ticket>`, `List<Passenger>` |
| **Exception Handling** | 3 custom exceptions + try-catch in BookingService, UI |
| **File Handling** | DataStore reads/writes CSV files for all entities |
| **Constructors** | Default, parameterized, overloaded in every class |
| **Packages** | `models`, `exceptions`, `services`, `ui` |
| **JavaFX GUI** | TextFields, Buttons, Labels, ComboBoxes, GridPane, VBox, HBox |
| **Charts** | PieChart (ticket status) + BarChart (seats per flight) |

---

## 🚀 How to Run

### Prerequisites
- JDK 17 or later
- JavaFX SDK 17+ ([download here](https://gluonhq.com/products/javafx/))

### Option 1: Using IntelliJ IDEA (Recommended)
1. Open the project in IntelliJ
2. Go to **File → Project Structure → Libraries** → Add JavaFX lib folder
3. Edit run configuration: add VM options:
   ```
   --module-path /path/to/javafx-sdk/lib --add-modules javafx.controls,javafx.fxml
   ```
4. Run `skybook.ui.MainApp`

### Option 2: Using the Build Script
```bash
export JAVAFX_HOME=/path/to/javafx-sdk/lib
chmod +x build_and_run.sh
./build_and_run.sh
```

---

## 📂 Data Files (Auto-Created)

| File | Purpose |
|---|---|
| `skybook_data/flights.csv` | All flight records |
| `skybook_data/tickets.csv` | All ticket records |
| `skybook_data/passengers.csv` | Registered passengers |
| `skybook_data/admins.csv` | Admin accounts |
| `skybook_data/email_log.txt` | Simulated email outbox |
| `skybook_data/tickets/*.txt` | Boarding pass receipts |

---

## 📧 Email Simulation

Real JavaMail API requires an SMTP server. Instead, SkyBook writes all emails
to `skybook_data/email_log.txt`. Each entry includes:
- To, Subject, Sent At
- Full formatted email body with passenger details

To upgrade to real email: add `javax.mail` to the classpath and replace
`logEmail()` in `EmailService.java` with an SMTP send call.

---

## 🎫 PDF Ticket Generation

Tickets are saved as formatted boarding-pass text files under
`skybook_data/tickets/<ticketId>.txt`.

To enable real PDF output:
1. Add iText 7 to your classpath:
   ```xml
   <!-- Maven -->
   <dependency>
     <groupId>com.itextpdf</groupId>
     <artifactId>itext7-core</artifactId>
     <version>7.2.5</version>
   </dependency>
   ```
2. Use `PdfWriter` and `Document` from iText to write the same fields.

---

## 👥 Default Users

**Admin:** `admin@skybook.com` / Code: `ADMIN123`

**Sample Passengers:**
- `ali@example.com`
- `sara@example.com`
- `ahmed@example.com`
