

import java.sql.Connection;

public class TestConnection{
    public static void main(String[] args) {
        // Attempt to get a connection from DatabaseConnection
        Connection conn = DatabaseConnection.getConnection();

        // Check if the connection is successful
        if (conn != null) {
            System.out.println("Connection successful!");
        } else {
            System.out.println("Failed to connect to the database.");
        }


    }
}
