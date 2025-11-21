package org.db;

import org.models.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class DatabaseHelper {
    private static final String DB_URL = "jdbc:sqlite:./restaurant.db";
    private Connection connection;

    public DatabaseHelper() {
        try {
            // Cargar driver JDBC de SQLite
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(DB_URL);
            initializeTables();
        } catch (SQLException e) {
            System.err.println("Error conectando a la base de datos: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.err.println("Driver JDBC de SQLite no encontrado: " + e.getMessage());
        }
    }

    private void initializeTables() {
        createUsersTable();
        // Ensure older DBs get the 'role' column if it was missing
        ensureUserRoleColumnExists();
        createMesasTable();
        createIngredientesTable();
        createRecipesTable();
        createRecipeIngredientsTable();
        createStockTable();
        createReservationsTable();
    }

    // Migration: add 'role' column to users table if missing (keeps default 'WAITER')
    private void ensureUserRoleColumnExists() {
        try (Statement stmt = connection.createStatement()) {
            // Check if 'role' exists in users table
            ResultSet rs = stmt.executeQuery("PRAGMA table_info(users);");
            boolean hasRole = false;
            while (rs.next()) {
                String colName = rs.getString("name");
                if ("role".equalsIgnoreCase(colName)) {
                    hasRole = true;
                    break;
                }
            }

            if (!hasRole) {
                // Add column with default to avoid breaking existing code
                stmt.execute("ALTER TABLE users ADD COLUMN role TEXT NOT NULL DEFAULT 'WAITER';");
                System.out.println("Migrated users table: added 'role' column with default 'WAITER'");
            }
        } catch (SQLException e) {
            System.err.println("Error ensuring role column in users table: " + e.getMessage());
        }
    }

    private void createUsersTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL UNIQUE,
                admin BOOLEAN NOT NULL DEFAULT 0,
                role TEXT NOT NULL DEFAULT 'WAITER'
            )
            """;
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            System.out.println("Tabla de usuarios creada exitosamente");
        } catch (SQLException e) {
            System.err.println("Error creando tabla de usuarios: " + e.getMessage());
        }
    }

    private void createMesasTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS mesas (
                id INTEGER PRIMARY KEY,
                capacidad INTEGER NOT NULL,
                estado TEXT NOT NULL DEFAULT 'LIBRE'
            )
            """;
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            System.out.println("Tabla de mesas creada exitosamente");
        } catch (SQLException e) {
            System.err.println("Error creando tabla de mesas: " + e.getMessage());
        }
    }

    private void createIngredientesTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS ingredientes (
                id INTEGER PRIMARY KEY,
                nombre TEXT NOT NULL UNIQUE,
                unidad_medida TEXT NOT NULL
            )
            """;
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            System.out.println("Tabla de ingredientes creada exitosamente");
        } catch (SQLException e) {
            System.err.println("Error creando tabla de ingredientes: " + e.getMessage());
        }
    }

    private void createRecipesTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS recipes (
                id INTEGER PRIMARY KEY,
                nombre TEXT NOT NULL,
                tiempo_preparacion_minutos INTEGER NOT NULL
            )
            """;
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            System.out.println("Tabla de recetas creada exitosamente");
        } catch (SQLException e) {
            System.err.println("Error creando tabla de recetas: " + e.getMessage());
        }
    }

    private void createRecipeIngredientsTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS recipe_ingredients (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                recipe_id INTEGER NOT NULL,
                ingrediente_id INTEGER NOT NULL,
                cantidad REAL NOT NULL,
                FOREIGN KEY (recipe_id) REFERENCES recipes(id),
                FOREIGN KEY (ingrediente_id) REFERENCES ingredientes(id),
                UNIQUE(recipe_id, ingrediente_id)
            )
            """;
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            System.out.println("Tabla de ingredientes de recetas creada exitosamente");
        } catch (SQLException e) {
            System.err.println("Error creando tabla recipe_ingredients: " + e.getMessage());
        }
    }

    private void createStockTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS stock (
                ingrediente_id INTEGER PRIMARY KEY,
                cantidad_actual REAL NOT NULL DEFAULT 0,
                cantidad_minima INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY (ingrediente_id) REFERENCES ingredientes(id)
            )
            """;
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            System.out.println("Tabla de inventario creada exitosamente");
        } catch (SQLException e) {
            System.err.println("Error creando tabla de inventario: " + e.getMessage());
        }
    }

    private void createReservationsTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS reservations (
                id INTEGER PRIMARY KEY,
                customer_name TEXT NOT NULL,
                guests INTEGER NOT NULL,
                reservation_time TEXT NOT NULL,
                table_id INTEGER,
                status TEXT NOT NULL DEFAULT 'PENDING',
                FOREIGN KEY (table_id) REFERENCES mesas(id)
            )
            """;
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            System.out.println("Tabla de reservas creada exitosamente");
        } catch (SQLException e) {
            System.err.println("Error creando tabla de reservas: " + e.getMessage());
        }
    }

    // User CRUD operations
    public boolean insertUser(User user) {
        String sql = "INSERT OR IGNORE INTO users (username, admin, role) VALUES (?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, user.getUsername());
            pstmt.setBoolean(2, user.isAdmin());
            // determine role string based on concrete subclass
            String role = "WAITER";
            if (user.isAdmin()) role = "ADMIN";
            else if (user.isChef()) role = "CHEF";
            pstmt.setString(3, role);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error insertando usuario: " + e.getMessage());
            return false;
        }
    }

    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT username, admin, role FROM users";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                String username = rs.getString("username");
                boolean admin = rs.getBoolean("admin");
                String role = rs.getString("role");
                users.add(createUserFromDb(username, admin, role));
            }
        } catch (SQLException e) {
            System.err.println("Error obteniendo usuarios: " + e.getMessage());
        }
        
        return users;
    }

    public User getUserByUsername(String username) {
        String sql = "SELECT username, admin, role FROM users WHERE username = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    boolean admin = rs.getBoolean("admin");
                    String role = rs.getString("role");
                    return createUserFromDb(username, admin, role);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error obteniendo usuario: " + e.getMessage());
        }
        
        return null;
    }

    // Helper to map DB row to concrete User subclass. Uses explicit role if available, falls back to username heuristic.
    private User createUserFromDb(String username, boolean admin, String role) {
        String uname = username == null ? "" : username.toLowerCase();
        if (role != null) {
            String r = role.toUpperCase();
            switch (r) {
                case "ADMIN": return new org.models.Admin(username);
                case "CHEF": return new org.models.Chef(username);
                case "WAITER": return new org.models.Waiter(username);
            }
        }
        // fallback: keep previous heuristic
        if (admin) return new org.models.Admin(username);
        if (uname.contains("chef")) return new org.models.Chef(username);
        return new org.models.Waiter(username);
    }

    // Mesa CRUD operations
    public boolean insertMesa(Mesa mesa) {
        String sql = "INSERT INTO mesas (id, capacidad, estado) VALUES (?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, mesa.getId());
            pstmt.setInt(2, mesa.getCapacidad());
            pstmt.setString(3, mesa.getEstado().toString());

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error insertando mesa: " + e.getMessage());
            return false;
        }
    }

    public List<Mesa> getAllMesas() {
        List<Mesa> mesas = new ArrayList<>();
        String sql = "SELECT id, capacidad, estado FROM mesas";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int id = rs.getInt("id");
                int capacidad = rs.getInt("capacidad");
                String estadoStr = rs.getString("estado");

                Mesa mesa = new Mesa(id, capacidad);
                mesa.setEstado(TableStatus.valueOf(estadoStr));
                mesas.add(mesa);
            }
        } catch (SQLException e) {
            System.err.println("Error obteniendo mesas: " + e.getMessage());
        }

        return mesas;
    }

    // Ingrediente CRUD operations
    public boolean insertIngrediente(Ingrediente ingrediente) {
        String sql = "INSERT INTO ingredientes (id, nombre, unidad_medida) VALUES (?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, ingrediente.getId());
            pstmt.setString(2, ingrediente.getNombre());
            pstmt.setString(3, ingrediente.getUnidadMedida().name());
            
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error insertando ingrediente: " + e.getMessage());
            return false;
        }
    }

    public List<Ingrediente> getAllIngredientes() {
        List<Ingrediente> ingredientes = new ArrayList<>();
        String sql = "SELECT id, nombre, unidad_medida FROM ingredientes";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                int id = rs.getInt("id");
                String nombre = rs.getString("nombre");
                String unidadMedida = rs.getString("unidad_medida");
                ingredientes.add(new Ingrediente(id, nombre, Magnitud.valueOf(unidadMedida)));
            }
        } catch (SQLException e) {
            System.err.println("Error obteniendo ingredientes: " + e.getMessage());
        }
        
        return ingredientes;
    }

    public Ingrediente getIngredienteById(int id) {
        String sql = "SELECT id, nombre, unidad_medida FROM ingredientes WHERE id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String nombre = rs.getString("nombre");
                    String unidadMedida = rs.getString("unidad_medida");
                    return new Ingrediente(id, nombre, Magnitud.valueOf(unidadMedida));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error obteniendo ingrediente: " + e.getMessage());
        }
        
        return null;
    }

    // Recipe CRUD operations
    public boolean insertRecipe(Recipe recipe) {
        String sqlRecipe = "INSERT INTO recipes (id, nombre, tiempo_preparacion_minutos) VALUES (?, ?, ?)";
        String sqlIngredients = "INSERT INTO recipe_ingredients (recipe_id, ingrediente_id, cantidad) VALUES (?, ?, ?)";
        
        try {
            connection.setAutoCommit(false);
            
            // Insert recipe
            try (PreparedStatement pstmt = connection.prepareStatement(sqlRecipe)) {
                pstmt.setInt(1, recipe.getId());
                pstmt.setString(2, recipe.getNombre());
                pstmt.setInt(3, recipe.getTiempoPreparacionMinutos());
                pstmt.executeUpdate();
            }
            
            // Insert recipe ingredients
            try (PreparedStatement pstmt = connection.prepareStatement(sqlIngredients)) {
                for (RecipeIngredient ri : recipe.getIngredientes()) {
                    pstmt.setInt(1, recipe.getId());
                    pstmt.setInt(2, ri.getIngrediente().getId());
                    pstmt.setDouble(3, ri.getCantidad());
                    pstmt.executeUpdate();
                }
            }
            
            connection.commit();
            connection.setAutoCommit(true);
            return true;
        } catch (SQLException e) {
            try {
                connection.rollback();
                connection.setAutoCommit(true);
            } catch (SQLException ex) {
                System.err.println("Error rolling back transaction: " + ex.getMessage());
            }
            System.err.println("Error insertando receta: " + e.getMessage());
            return false;
        }
    }

    public List<Recipe> getAllRecipes() {
        List<Recipe> recipes = new ArrayList<>();
        String sql = """
            SELECT r.id, r.nombre, r.tiempo_preparacion_minutos,
                   ri.ingrediente_id, ri.cantidad,
                   i.nombre as ingrediente_nombre, i.unidad_medida
            FROM recipes r
            LEFT JOIN recipe_ingredients ri ON r.id = ri.recipe_id
            LEFT JOIN ingredientes i ON ri.ingrediente_id = i.id
            ORDER BY r.id, ri.ingrediente_id
            """;
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            Map<Integer, Recipe> recipeMap = new HashMap<>();
            
            while (rs.next()) {
                int recipeId = rs.getInt("id");
                String nombre = rs.getString("nombre");
                int tiempoPrep = rs.getInt("tiempo_preparacion_minutos");
                
                Recipe recipe = recipeMap.get(recipeId);
                if (recipe == null) {
                    List<RecipeIngredient> ingredientes = new ArrayList<>();
                    recipe = new Recipe(recipeId, nombre, ingredientes, tiempoPrep, 0); // Default precio 0
                    recipeMap.put(recipeId, recipe);
                }
                
                // Add ingredient if exists
                int ingredienteId = rs.getInt("ingrediente_id");
                if (!rs.wasNull()) {
                    double cantidad = rs.getDouble("cantidad");
                    String ingredienteNombre = rs.getString("ingrediente_nombre");
                    String unidadMedida = rs.getString("unidad_medida");
                    
                    Ingrediente ingrediente = new Ingrediente(ingredienteId, ingredienteNombre, Magnitud.valueOf(unidadMedida));
                    RecipeIngredient ri = new RecipeIngredient(ingrediente, cantidad);
                    recipe.getIngredientes().add(ri);
                }
            }
            
            recipes.addAll(recipeMap.values());
        } catch (SQLException e) {
            System.err.println("Error obteniendo recetas: " + e.getMessage());
        }
        
        return recipes;
    }

    // Stock CRUD operations
    public boolean insertStock(DetalleIngrediente stock) {
        String sql = "INSERT OR REPLACE INTO stock (ingrediente_id, cantidad_actual, cantidad_minima) VALUES (?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, stock.getIngrediente().getId());
            pstmt.setDouble(2, stock.getCantidadActual());
            pstmt.setInt(3, stock.getCantidadMinima());

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error insertando inventario: " + e.getMessage());
            return false;
        }
    }

    public List<DetalleIngrediente> getAllStock() {
        List<DetalleIngrediente> stockList = new ArrayList<>();
        String sql = """
            SELECT s.cantidad_actual, s.cantidad_minima,
                   i.id, i.nombre, i.unidad_medida
            FROM stock s
            JOIN ingredientes i ON s.ingrediente_id = i.id
            """;

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String nombre = rs.getString("nombre");
                String unidadMedida = rs.getString("unidad_medida");
                double cantidadActual = rs.getDouble("cantidad_actual");
                int cantidadMinima = rs.getInt("cantidad_minima");

                Ingrediente ingrediente = new Ingrediente(id, nombre, Magnitud.valueOf(unidadMedida));
                DetalleIngrediente stock = new DetalleIngrediente(ingrediente, cantidadActual, cantidadMinima);
                stockList.add(stock);
            }
        } catch (SQLException e) {
            System.err.println("Error obteniendo inventario: " + e.getMessage());
        }

        return stockList;
    }

    public boolean updateStock(int ingredienteId, double nuevaCantidad) {
        String sql = "UPDATE stock SET cantidad_actual = ? WHERE ingrediente_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setDouble(1, nuevaCantidad);
            pstmt.setInt(2, ingredienteId);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error actualizando inventario: " + e.getMessage());
            return false;
        }
    }

    // Reservation CRUD operations
    public boolean insertReservation(Reservation reservation) {
        String sql = "INSERT INTO reservations (id, customer_name, guests, reservation_time, table_id, status) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, reservation.getId());
            pstmt.setString(2, reservation.getCustomerName());
            pstmt.setInt(3, reservation.getGuests());
            pstmt.setString(4, reservation.getWhen().toString());
            if (reservation.getTableId() != null) {
                pstmt.setInt(5, reservation.getTableId());
            } else {
                pstmt.setNull(5, java.sql.Types.INTEGER);
            }
            pstmt.setString(6, reservation.getStatus().toString());
            
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error insertando reserva: " + e.getMessage());
            return false;
        }
    }

    public List<Reservation> getAllReservations() {
        List<Reservation> reservations = new ArrayList<>();
        String sql = "SELECT id, customer_name, guests, reservation_time, table_id, status FROM reservations";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                int id = rs.getInt("id");
                String customerName = rs.getString("customer_name");
                int guests = rs.getInt("guests");
                String timeStr = rs.getString("reservation_time");
                int tableIdValue = rs.getInt("table_id");
                Integer tableId = rs.wasNull() ? null : tableIdValue;
                String statusStr = rs.getString("status");
                
                Reservation reservation = new Reservation(id, customerName, guests, 
                    java.time.LocalDateTime.parse(timeStr));
                reservation.setTableId(tableId);
                reservation.setStatus(ReservationStatus.valueOf(statusStr));
                reservations.add(reservation);
            }
        } catch (SQLException e) {
            System.err.println("Error obteniendo reservas: " + e.getMessage());
        }
        
        return reservations;
    }

    public Connection getConnection() {
        return connection;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Conexión a base de datos cerrada");
            }
        } catch (SQLException e) {
            System.err.println("Error cerrando conexión a base de datos: " + e.getMessage());
        }
    }

    // Simple test method
    public void testConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                System.out.println("✓ ¡Conexión a base de datos funcionando!");
                
                // Test basic query
                String sql = "SELECT 1 as test";
                try (Statement stmt = connection.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {
                    
                    if (rs.next()) {
                        System.out.println("✓ Prueba de consulta exitosa");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("✗ Prueba de conexión a base de datos falló: " + e.getMessage());
        }
    }
}
