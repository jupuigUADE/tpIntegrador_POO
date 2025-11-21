package org.db;

import org.db.UserManager;

public class InitializeUsers {
    public static void main(String[] args) {
        System.out.println("=== Initializing User Authentication System ===");
        
        UserManager userManager = new UserManager();
        
        // Initialize default users with passwords
        userManager.initializeDefaultUsers();
        
        System.out.println("\n=== Testing Authentication ===");
        
        // Test authentication
        System.out.println("Testing admin/admin123: " + 
            userManager.authenticateUser("admin", "admin123"));
        System.out.println("Testing waiter1/waiter123: " + 
            userManager.authenticateUser("waiter1", "waiter123"));
        System.out.println("Testing chef1/chef123: " + 
            userManager.authenticateUser("chef1", "chef123"));
        System.out.println("Testing admin/wrongpass: " + 
            userManager.authenticateUser("admin", "wrongpass"));
        
        userManager.close();
        
        System.out.println("\n=== User Authentication System Ready ===");
        System.out.println("You can now log in to the application with:");
        System.out.println("• admin / admin123 (Administrator)");
        System.out.println("• waiter1 / waiter123 (Waiter)");
        System.out.println("• chef1 / chef123 (Chef)");
    }
}