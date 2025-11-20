import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    // Database connection details are loaded from environment variables when possible.
    // Fallbacks keep local development simple but avoid hardcoding real passwords here.
    private static final String URL = System.getenv().getOrDefault("FEDHA_DB_URL", "jdbc:mysql://localhost:3306/FedhaYouthGroup");
    private static final String USER = System.getenv().getOrDefault("FEDHA_DB_USER", "root");
    private static final String PASSWORD = System.getenv("FEDHA_DB_PASSWORD");
    private static Connection connection = null;

    // Static block to initialize the connection
    static {
        try {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Database connected successfully!");
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
        }
    }

    // Get the singleton connection instance
    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
            }
        } catch (SQLException e) {
            System.err.println("Failed to establish a database connection: " + e.getMessage());
        }
        return connection;
    }
}
