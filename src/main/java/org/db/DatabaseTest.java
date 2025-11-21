package org.db;

import org.db.DatabaseHelper;
import org.models.*;
import java.util.List;
import java.util.ArrayList;
import java.time.LocalDateTime;

public class DatabaseTest {
    public static void main(String[] args) {
        System.out.println("=== Complete Restaurant Database Test ===");
        
        // Create database helper
        DatabaseHelper db = new DatabaseHelper();
        
        // Test connection
        db.testConnection();
        
        // Test User operations
        System.out.println("\n=== Testing User Operations ===");
        testUsers(db);
        
        // Test Mesa operations
        System.out.println("\n=== Testing Mesa Operations ===");
        testMesas(db);
        
        // Test Ingrediente operations
        System.out.println("\n=== Testing Ingrediente Operations ===");
        testIngredientes(db);
        
        // Test Stock operations
        System.out.println("\n=== Testing Stock Operations ===");
        testStock(db);
        
        // Test Recipe operations
        System.out.println("\n=== Testing Recipe Operations ===");
        testRecipes(db);
        
        // Test Reservation operations
        System.out.println("\n=== Testing Reservation Operations ===");
        testReservations(db);
        
        // Close database connection
        db.close();
        
        System.out.println("\n=== Complete Database Test Finished ===");
    }
    
    private static void testUsers(DatabaseHelper db) {
        // Insert some test users
        Admin admin = new Admin("admin");
        Waiter waiter = new Waiter("waiter1");
        Chef chef = new Chef("chef1");

        if (db.insertUser(admin)) {
            System.out.println("✓ Admin user inserted successfully");
        }
        if (db.insertUser(waiter)) {
            System.out.println("✓ Waiter user inserted successfully");
        }
        if (db.insertUser(chef)) {
            System.out.println("✓ Chef user inserted successfully");
        }
        
        // Retrieve all users
        List<User> users = db.getAllUsers();
        System.out.println("\nAll users in database:");
        for (User user : users) {
            System.out.println("- " + user.getUsername() + 
                             (user.isAdmin() ? " (Admin)" : user.isChef() ? " (Chef)" : " (Waiter)"));
        }
    }
    
    private static void testMesas(DatabaseHelper db) {
        // Insert some test mesas
        Mesa mesa1 = new Mesa(1, 4);
        Mesa mesa2 = new Mesa(2, 2);
        Mesa mesa3 = new Mesa(3, 6);
        mesa2.setEstado(TableStatus.OCUPADA);
        
        db.insertMesa(mesa1);
        db.insertMesa(mesa2);
        db.insertMesa(mesa3);
        System.out.println("✓ 3 mesas inserted successfully");
        
        // Retrieve all mesas
        List<Mesa> mesas = db.getAllMesas();
        System.out.println("\nAll mesas in database:");
        for (Mesa mesa : mesas) {
            System.out.println("- Mesa " + mesa.getId() + 
                             " (Capacity: " + mesa.getCapacidad() + 
                             ", Status: " + mesa.getEstado() + ")");
        }
    }
    
    private static void testIngredientes(DatabaseHelper db) {
        // Insert some test ingredientes
        Ingrediente tomate = new Ingrediente(1, "Tomate", Magnitud.KILOGRAMO);
        Ingrediente queso = new Ingrediente(2, "Queso", Magnitud.KILOGRAMO);
        Ingrediente harina = new Ingrediente(3, "Harina", Magnitud.KILOGRAMO);
        Ingrediente aceite = new Ingrediente(4, "Aceite", Magnitud.LITRO);
        
        db.insertIngrediente(tomate);
        db.insertIngrediente(queso);
        db.insertIngrediente(harina);
        db.insertIngrediente(aceite);
        System.out.println("✓ 4 ingredientes inserted successfully");
        
        // Retrieve all ingredientes
        List<Ingrediente> ingredientes = db.getAllIngredientes();
        System.out.println("\nAll ingredientes in database:");
        for (Ingrediente ingrediente : ingredientes) {
            System.out.println("- " + ingrediente.getNombre() + 
                             " (ID: " + ingrediente.getId() + 
                             ", Unit: " + ingrediente.getUnidadMedida() + ")");
        }
    }
    
    private static void testStock(DatabaseHelper db) {
        // Get ingredientes to create stock
        List<Ingrediente> ingredientes = db.getAllIngredientes();
        
        // Create stock for each ingrediente
        for (Ingrediente ingrediente : ingredientes) {
            double cantidadInicial = 10.0; // Start with 10 units
            int cantidadMinima = 2; // Minimum 2 units
            DetalleIngrediente stock = new DetalleIngrediente(ingrediente, cantidadInicial, cantidadMinima);
            db.insertStock(stock);
        }
        System.out.println("✓ Stock created for all ingredientes");
        
        // Retrieve all stock
        List<DetalleIngrediente> stockList = db.getAllStock();
        System.out.println("\nCurrent stock levels:");
        for (DetalleIngrediente stock : stockList) {
            System.out.println("- " + stock.getIngrediente().getNombre() +
                             ": " + stock.getCantidadActual() + " " + 
                             stock.getIngrediente().getUnidadMedida() +
                             (stock.necesitaReorden() ? " (NEEDS REORDER)" : ""));
        }
        
        // Test stock update
        if (!stockList.isEmpty()) {
            DetalleIngrediente firstStock = stockList.get(0);
            int ingredienteId = firstStock.getIngrediente().getId();
            double newAmount = 1.5; // Update to low amount
            
            if (db.updateStock(ingredienteId, newAmount)) {
                System.out.println("✓ Updated stock for " + firstStock.getIngrediente().getNombre() + 
                                 " to " + newAmount);
            }
        }
    }
    
    private static void testRecipes(DatabaseHelper db) {
        // Get ingredientes for recipes
        Ingrediente tomate = db.getIngredienteById(1);
        Ingrediente queso = db.getIngredienteById(2);
        Ingrediente harina = db.getIngredienteById(3);
        
        if (tomate != null && queso != null && harina != null) {
            // Create pizza recipe
            List<RecipeIngredient> pizzaIngredientes = new ArrayList<>();
            pizzaIngredientes.add(new RecipeIngredient(harina, 0.5)); // 0.5 kg harina
            pizzaIngredientes.add(new RecipeIngredient(tomate, 0.2)); // 0.2 kg tomate
            pizzaIngredientes.add(new RecipeIngredient(queso, 0.3)); // 0.3 kg queso
            
            Recipe pizza = new Recipe(1, "Pizza Margherita", pizzaIngredientes, 25, 15);
            
            if (db.insertRecipe(pizza)) {
                System.out.println("✓ Pizza recipe inserted successfully");
            }
            
            // Create pasta recipe
            List<RecipeIngredient> pastaIngredientes = new ArrayList<>();
            pastaIngredientes.add(new RecipeIngredient(harina, 0.3)); // 0.3 kg harina
            pastaIngredientes.add(new RecipeIngredient(tomate, 0.4)); // 0.4 kg tomate
            
            Recipe pasta = new Recipe(2, "Pasta con Tomate", pastaIngredientes, 15, 12);
            
            if (db.insertRecipe(pasta)) {
                System.out.println("✓ Pasta recipe inserted successfully");
            }
        }
        
        // Retrieve all recipes
        List<Recipe> recipes = db.getAllRecipes();
        System.out.println("\nAll recipes in database:");
        for (Recipe recipe : recipes) {
            System.out.println("- " + recipe.getNombre() + 
                             " (ID: " + recipe.getId() + 
                             ", Prep time: " + recipe.getTiempoPreparacionMinutos() + " min)");
            for (RecipeIngredient ri : recipe.getIngredientes()) {
                System.out.println("  * " + ri.getIngrediente().getNombre() + 
                                 ": " + ri.getCantidad() + " " + 
                                 ri.getIngrediente().getUnidadMedida());
            }
        }
    }
    
    private static void testReservations(DatabaseHelper db) {
        // Create some test reservations
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1).withHour(19).withMinute(0);
        LocalDateTime dayAfter = LocalDateTime.now().plusDays(2).withHour(20).withMinute(30);
        
        Reservation res1 = new Reservation(1, "Juan Pérez", 4, tomorrow);
        Reservation res2 = new Reservation(2, "María García", 2, dayAfter);
        res2.setTableId(2); // Assign table 2
        res2.setStatus(ReservationStatus.CONFIRMED);
        
        if (db.insertReservation(res1)) {
            System.out.println("✓ Reservation 1 inserted successfully");
        }
        if (db.insertReservation(res2)) {
            System.out.println("✓ Reservation 2 inserted successfully");
        }
        
        // Retrieve all reservations
        List<Reservation> reservations = db.getAllReservations();
        System.out.println("\nAll reservations in database:");
        for (Reservation reservation : reservations) {
            System.out.println("- " + reservation.getCustomerName() + 
                             " (" + reservation.getGuests() + " guests)" +
                             " at " + reservation.getWhen() +
                             " [Status: " + reservation.getStatus() + 
                             (reservation.getTableId() != null ? 
                                 ", Table: " + reservation.getTableId() : ", No table assigned") + "]");
        }
    }
}