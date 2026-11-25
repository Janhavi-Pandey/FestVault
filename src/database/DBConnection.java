package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DBConnection {
    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/finova?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private DBConnection() { }

    public static Connection getConnection() throws SQLException {
        String url = System.getenv().getOrDefault("FINOVA_DB_URL", DEFAULT_URL);
        String user = System.getenv().getOrDefault("FINOVA_DB_USER", "root");
        String password = System.getenv().getOrDefault("FINOVA_DB_PASSWORD", "");
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL Connector/J is missing from the lib folder/classpath.", e);
        }
        return DriverManager.getConnection(url, user, password);
    }
}
