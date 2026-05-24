package skybook.models;

/**
 * Admin class - child of Person.
 * Demonstrates: Inheritance, Polymorphism (method overriding)
 */
public class Admin extends Person {

    private String adminCode;
    private String department;

    // Default constructor
    public Admin() {
        super();
        this.adminCode = "ADM000";
        this.department = "General";
    }

    // Parameterized constructor
    public Admin(String id, String name, String email, String phone, String adminCode, String department) {
        super(id, name, email, phone);
        this.adminCode = adminCode;
        this.department = department;
    }

    // Constructor overloading
    public Admin(String id, String name, String email, String adminCode) {
        super(id, name, email);
        this.adminCode = adminCode;
        this.department = "Operations";
    }

    // Method overriding (Polymorphism)
    @Override
    public String getRole() {
        return "ADMIN";
    }

    @Override
    public String getSummary() {
        return String.format("Admin %s | Code: %s | Dept: %s",
                getName(), adminCode, department);
    }

    // Method overloading
    public boolean authenticate(String code) {
        return this.adminCode.equals(code);
    }

    public boolean authenticate(String code, String dept) {
        return this.adminCode.equals(code) && this.department.equalsIgnoreCase(dept);
    }

    // CSV serialization
    public String toCSV() {
        return String.join(",", getId(), getName(), getEmail(), getPhone(), adminCode, department);
    }

    public static Admin fromCSV(String csvLine) {
        String[] parts = csvLine.split(",", -1);
        if (parts.length < 6) return null;
        return new Admin(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5]);
    }

    public String getAdminCode() { return adminCode; }
    public void setAdminCode(String adminCode) { this.adminCode = adminCode; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
}
