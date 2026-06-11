package config;

import java.sql.*;

public class DBConnection {
    private static final String URL =
        "jdbc:mysql://localhost:3307/db_bookings?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASS = "";

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver tidak jumpa: " + e.getMessage());
        }
        return DriverManager.getConnection(URL, USER, PASS);
    }
}