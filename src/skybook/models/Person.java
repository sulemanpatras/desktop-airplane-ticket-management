package skybook.models;

/**
 * Abstract base class for all persons in the system.
 * Demonstrates: Abstraction, Encapsulation, Inheritance
 */
public abstract class Person {

    // Encapsulation - private fields with getters/setters
    private String id;
    private String name;
    private String email;
    private String phone;

    // Default constructor
    public Person() {
        this.id = "P000";
        this.name = "Unknown";
        this.email = "";
        this.phone = "";
    }

    // Parameterized constructor
    public Person(String id, String name, String email, String phone) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    // Constructor overloading - without phone
    public Person(String id, String name, String email) {
        this(id, name, email, "N/A");
    }

    // Abstract methods - must be implemented in child classes (Abstraction)
    public abstract String getRole();
    public abstract String getSummary();

    // Method overloading (Polymorphism)
    public String getDisplayInfo() {
        return String.format("[%s] %s <%s>", getRole(), name, email);
    }

    public String getDisplayInfo(boolean includePhone) {
        if (includePhone)
            return String.format("[%s] %s <%s> | Phone: %s", getRole(), name, email, phone);
        return getDisplayInfo();
    }

    // Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    @Override
    public String toString() {
        return getDisplayInfo();
    }
}
