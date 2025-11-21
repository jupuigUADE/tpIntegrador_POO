package org.db;

import org.models.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;

public class UserManager {
    private static final String DB_URL = "jdbc:sqlite:./restaurant.db";
    private Connection connection;
    
    public UserManager() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(DB_URL);
            createPasswordTable();
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("Error conectando a la base de datos: " + e.getMessage());
        }
    }
    
    private void createPasswordTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS user_passwords (
                username TEXT PRIMARY KEY,
                password_hash TEXT NOT NULL,
                FOREIGN KEY (username) REFERENCES users(username)
            )
            """;
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            System.out.println("Tabla de contraseñas de usuarios creada exitosamente");
        } catch (SQLException e) {
            System.err.println("Error creando tabla user_passwords: " + e.getMessage());
        }
    }
    
    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashedBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
    
    public boolean createUserWithPassword(String username, boolean isAdmin, String password) {
        try {
            connection.setAutoCommit(false);
            
            // First create the user (include role)
            String userSql = "INSERT OR IGNORE INTO users (username, admin, role) VALUES (?, ?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(userSql)) {
                pstmt.setString(1, username);
                pstmt.setBoolean(2, isAdmin);
                String role = "WAITER";
                if (isAdmin) role = "ADMIN";
                else if (username != null && username.toLowerCase().contains("chef")) role = "CHEF";
                pstmt.setString(3, role);
                pstmt.executeUpdate();
            }

            // Then add the password
            String passwordSql = "INSERT OR REPLACE INTO user_passwords (username, password_hash) VALUES (?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(passwordSql)) {
                pstmt.setString(1, username);
                pstmt.setString(2, hashPassword(password));
                pstmt.executeUpdate();
            }

            connection.commit();
            connection.setAutoCommit(true);
            return true;
        } catch (SQLException e) {
            try {
                connection.rollback();
                connection.setAutoCommit(true);
            } catch (SQLException ex) {
                System.err.println("Error revirtiendo transacción: " + ex.getMessage());
            }
            System.err.println("Error creando usuario con contraseña: " + e.getMessage());
            return false;
        }
    }

    public boolean authenticateUser(String username, String password) {
        String sql = "SELECT password_hash FROM user_passwords WHERE username = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    String inputHash = hashPassword(password);
                    return storedHash.equals(inputHash);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error autenticando usuario: " + e.getMessage());
        }
        
        return false;
    }
    
    public boolean changePassword(String username, String oldPassword, String newPassword) {
        if (!authenticateUser(username, oldPassword)) {
            return false;
        }
        
        String sql = "UPDATE user_passwords SET password_hash = ? WHERE username = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, hashPassword(newPassword));
            pstmt.setString(2, username);
            
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error changing password: " + e.getMessage());
            return false;
        }
    }
    
    public void initializeDefaultUsers() {
        // Create default users with passwords
        boolean admin = createUserWithPassword("admin", true, "admin123");
        boolean waiter = createUserWithPassword("waiter1", false, "waiter123");
        boolean chef = createUserWithPassword("chef1", false, "chef123");
        
        if (admin && waiter && chef) {
            System.out.println("✓ Default users initialized successfully:");
            System.out.println("- admin / admin123 (Admin)");
            System.out.println("- waiter1 / waiter123 (Regular)");
            System.out.println("- chef1 / chef123 (Regular)");
        } else {
            System.out.println("⚠ Some users may already exist or there was an error");
        }
    }
    
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("UserManager database connection closed");
            }
        } catch (SQLException e) {
            System.err.println("Error closing database connection: " + e.getMessage());
        }
    }
}