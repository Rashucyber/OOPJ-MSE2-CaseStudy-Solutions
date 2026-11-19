import java.sql.*;
import java.util.Scanner;

public class OrderManagementSystem {
    // Database details - apna MySQL password yahan dalein
    static final String URL = "jdbc:mysql://localhost:3306/ecommerce_db";
    static final String USER = "root";
    static final String PASS = "Thomas@#0987"; 

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
            System.out.println("Connected to ecommerce_db successfully!");

            while (true) {
                System.out.println("\n--- ORDER MANAGEMENT SYSTEM ---");
                System.out.println("1. Insert Order Record");
                System.out.println("2. View Orders by Status (Quantity > 2, Sorted by Price Desc)");
                System.out.println("3. Increase Order Quantity");
                System.out.println("4. Update Order Status");
                System.out.println("5. View Total Order Value & Delete Cancelled Order");
                System.out.println("6. Exit");
                System.out.print("Enter your choice: ");
                
                int choice = sc.nextInt();
                sc.nextLine(); // buffer clear

                switch (choice) {
                    case 1:
                        insertOrder(conn, sc);
                        break;
                    case 2:
                        displayOrdersByStatus(conn, sc);
                        break;
                    case 3:
                        increaseQuantity(conn, sc);
                        break;
                    case 4:
                        updateOrderStatus(conn, sc);
                        break;
                    case 5:
                        totalValueAndDeleteCancelled(conn, sc);
                        break;
                    case 6:
                        System.out.println("Exiting application...");
                        return;
                    default:
                        System.out.println("Invalid choice! Try again.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Database Connection Error: " + e.getMessage());
        }
    }

    // (a) Insert order using PreparedStatement & handle duplicate ID
    static void insertOrder(Connection conn, Scanner sc) {
        System.out.print("Enter Order ID: ");
        int id = sc.nextInt();
        sc.nextLine();
        System.out.print("Enter Customer Name: ");
        String customer = sc.nextLine();
        System.out.print("Enter Product Name: ");
        String product = sc.nextLine();
        System.out.print("Enter Quantity: ");
        int qty = sc.nextInt();
        System.out.print("Enter Unit Price: ");
        double price = sc.nextDouble();
        sc.nextLine();
        System.out.print("Enter Status (Pending/Shipped/Delivered/Cancelled): ");
        String status = sc.nextLine();

        String sql = "INSERT INTO Orders (order_id, customer_name, product_name, quantity, unit_price, status) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.setString(2, customer);
            pstmt.setString(3, product);
            pstmt.setInt(4, qty);
            pstmt.setDouble(5, price);
            pstmt.setString(6, status);
            pstmt.executeUpdate();
            System.out.println("Order inserted successfully!");
        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("Error: Order ID " + id + " already exists! Duplicate IDs are not allowed.");
        } catch (SQLException e) {
            System.out.println("Error inserting order: " + e.getMessage());
        }
    }

    // (b) Display orders by status & quantity > 2, sorted by unit_price DESC
    static void displayOrdersByStatus(Connection conn, Scanner sc) throws SQLException {
        System.out.print("Enter Status to filter (e.g., Pending, Shipped): ");
        String status = sc.nextLine();

        String sql = "SELECT * FROM Orders WHERE status = ? AND quantity > 2 ORDER BY unit_price DESC";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            ResultSet rs = pstmt.executeQuery();

            System.out.println("\n--- Filtered Orders ---");
            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.printf("ID: %d | Customer: %s | Product: %s | Qty: %d | Price: %.2f | Status: %s%n",
                        rs.getInt("order_id"), rs.getString("customer_name"), rs.getString("product_name"),
                        rs.getInt("quantity"), rs.getDouble("unit_price"), rs.getString("status"));
            }
            if (!found) {
                System.out.println("No orders found matching the criteria.");
            }
        }
    }

    // (c) Increase quantity for a given Order ID
    static void increaseQuantity(Connection conn, Scanner sc) throws SQLException {
        System.out.print("Enter Order ID: ");
        int id = sc.nextInt();
        System.out.print("Enter Additional Quantity to add: ");
        int addQty = sc.nextInt();

        String sql = "UPDATE Orders SET quantity = quantity + ? WHERE order_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, addQty);
            pstmt.setInt(2, id);
            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                System.out.println("Quantity updated successfully!");
            } else {
                System.out.println("Order ID " + id + " does not exist.");
            }
        }
    }

    // (d) Update status only if order exists & status is valid, then display details
    static void updateOrderStatus(Connection conn, Scanner sc) throws SQLException {
        System.out.print("Enter Order ID: ");
        int id = sc.nextInt();
        sc.nextLine();
        System.out.print("Enter New Status (Pending, Shipped, Delivered): ");
        String newStatus = sc.nextLine();

        if (!newStatus.equalsIgnoreCase("Pending") && 
            !newStatus.equalsIgnoreCase("Shipped") && 
            !newStatus.equalsIgnoreCase("Delivered")) {
            System.out.println("Invalid status! Only Pending, Shipped, or Delivered are allowed.");
            return;
        }

        String updateSql = "UPDATE Orders SET status = ? WHERE order_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
            pstmt.setString(1, newStatus);
            pstmt.setInt(2, id);
            int rows = pstmt.executeUpdate();

            if (rows > 0) {
                System.out.println("Status updated! Updated Order Details:");
                String selectSql = "SELECT * FROM Orders WHERE order_id = ?";
                try (PreparedStatement selStmt = conn.prepareStatement(selectSql)) {
                    selStmt.setInt(1, id);
                    ResultSet rs = selStmt.executeQuery();
                    if (rs.next()) {
                        System.out.printf("ID: %d | Customer: %s | Product: %s | Qty: %d | Price: %.2f | Status: %s%n",
                                rs.getInt("order_id"), rs.getString("customer_name"), rs.getString("product_name"),
                                rs.getInt("quantity"), rs.getDouble("unit_price"), rs.getString("status"));
                    }
                }
            } else {
                System.out.println("Order ID " + id + " not found.");
            }
        }
    }

    // (e) Display total order value & delete order only if status is Cancelled
    static void totalValueAndDeleteCancelled(Connection conn, Scanner sc) throws SQLException {
        String sumSql = "SELECT SUM(quantity * unit_price) AS total_value FROM Orders";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sumSql)) {
            if (rs.next()) {
                System.out.printf("Total Value of All Orders: %.2f%n", rs.getDouble("total_value"));
            }
        }

        System.out.print("Enter Order ID to delete (only if Cancelled): ");
        int id = sc.nextInt();

        String checkSql = "SELECT status FROM Orders WHERE order_id = ?";
        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setInt(1, id);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                String status = rs.getString("status");
                if (status.equalsIgnoreCase("Cancelled")) {
                    String delSql = "DELETE FROM Orders WHERE order_id = ?";
                    try (PreparedStatement delStmt = conn.prepareStatement(delSql)) {
                        delStmt.setInt(1, id);
                        delStmt.executeUpdate();
                        System.out.println("Cancelled order ID " + id + " deleted successfully!");
                    }
                } else {
                    System.out.println("Cannot delete! Order status is '" + status + "', not 'Cancelled'.");
                }
            } else {
                System.out.println("Order ID " + id + " does not exist.");
            }
        }
    }
}
