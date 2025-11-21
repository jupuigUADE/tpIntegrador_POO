# Complete Restaurant Database Setup

This project now includes full SQLite database connectivity for storing and managing all restaurant data including users, tables, ingredients, recipes, stock, and reservations.

## What was added

1. **DatabaseHelper.java** - A comprehensive utility class for managing SQLite connections and operations
2. **DatabaseTest.java** - A complete test class demonstrating all database features
3. **SQLite JDBC dependencies** - Downloaded directly to the project
4. **Complete database schema** - All restaurant entities supported

## Database Features

- **Automatic table creation** for all entities
- **CRUD operations** for users, mesas, ingredientes, recipes, stock, and reservations
- **Relational integrity** with foreign keys
- **Transaction support** for complex operations
- **Connection management** with proper cleanup
- **Error handling** for all database operations

## Tables Created

### Users Table
```sql
CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    admin BOOLEAN NOT NULL DEFAULT 0
)
```

### Mesas Table
```sql
CREATE TABLE mesas (
    id INTEGER PRIMARY KEY,
    capacidad INTEGER NOT NULL,
    estado TEXT NOT NULL DEFAULT 'LIBRE'
)
```

### Ingredientes Table
```sql
CREATE TABLE ingredientes (
    id INTEGER PRIMARY KEY,
    nombre TEXT NOT NULL UNIQUE,
    unidad_medida TEXT NOT NULL
)
```

### Recipes Table
```sql
CREATE TABLE recipes (
    id INTEGER PRIMARY KEY,
    nombre TEXT NOT NULL,
    tiempo_preparacion_minutos INTEGER NOT NULL
)
```

### Recipe Ingredients Table
```sql
CREATE TABLE recipe_ingredients (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    recipe_id INTEGER NOT NULL,
    ingrediente_id INTEGER NOT NULL,
    cantidad REAL NOT NULL,
    FOREIGN KEY (recipe_id) REFERENCES recipes(id),
    FOREIGN KEY (ingrediente_id) REFERENCES ingredientes(id),
    UNIQUE(recipe_id, ingrediente_id)
)
```

### Stock Table
```sql
CREATE TABLE stock (
    ingrediente_id INTEGER PRIMARY KEY,
    cantidad_actual REAL NOT NULL DEFAULT 0,
    cantidad_minima INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (ingrediente_id) REFERENCES ingredientes(id)
)
```

### Reservations Table
```sql
CREATE TABLE reservations (
    id INTEGER PRIMARY KEY,
    customer_name TEXT NOT NULL,
    guests INTEGER NOT NULL,
    reservation_time TEXT NOT NULL,
    table_id INTEGER,
    status TEXT NOT NULL DEFAULT 'PENDING',
    FOREIGN KEY (table_id) REFERENCES mesas(id)
)
```

## How to run

### Option 1: Using the script
```bash
./run-database-test.sh
```

### Option 2: Manual compilation and execution
```bash
# Compile
javac -cp sqlite-jdbc-3.43.2.0.jar -d target/classes src/main/java/org/models/*.java src/main/java/org/example/*.java

# Run
java --enable-native-access=ALL-UNNAMED -cp "target/classes:sqlite-jdbc-3.43.2.0.jar:slf4j-api-2.0.9.jar:slf4j-simple-2.0.9.jar" org.example.DatabaseTest
```

## Using the DatabaseHelper in your code

### Basic Operations
```java
// Create database connection
DatabaseHelper db = new DatabaseHelper();

// Insert entities
User newUser = new User("john_doe", false);
db.insertUser(newUser);

Mesa mesa = new Mesa(5, 6);
db.insertMesa(mesa);

Ingrediente ingredient = new Ingrediente(1, "Tomato", "kg");
db.insertIngrediente(ingredient);

// Create and insert stock
Stock stock = new Stock(ingredient, 10.0, 2);
db.insertStock(stock);

// Always close when done
db.close();
```

### Recipe Management
```java
// Create recipe with ingredients
List<RecipeIngredient> ingredients = new ArrayList<>();
ingredients.add(new RecipeIngredient(flour, 0.5));
ingredients.add(new RecipeIngredient(tomato, 0.3));

Recipe pizza = new Recipe(1, "Pizza Margherita", ingredients, 25);
db.insertRecipe(pizza);

// Retrieve all recipes with their ingredients
List<Recipe> recipes = db.getAllRecipes();
```

### Reservation Management
```java
// Create reservation
LocalDateTime tomorrow = LocalDateTime.now().plusDays(1).withHour(19).withMinute(0);
Reservation reservation = new Reservation(1, "John Doe", 4, tomorrow);
db.insertReservation(reservation);

// Get all reservations
List<Reservation> reservations = db.getAllReservations();
```

## Database File

The SQLite database is stored as `restaurant.db` in the project root directory.

## Test Data Included

The test creates sample data for all entities:
- **Users**: admin, waiter1, chef1
- **Mesas**: 3 tables with different capacities and states
- **Ingredientes**: Tomate, Queso, Harina, Aceite
- **Stock**: Initial stock for all ingredients
- **Recipes**: Pizza Margherita and Pasta con Tomate with ingredients
- **Reservations**: Sample reservations with different statuses

## Dependencies Downloaded

- `sqlite-jdbc-3.43.2.0.jar` - SQLite JDBC driver
- `slf4j-api-2.0.9.jar` - SLF4J API (required by SQLite JDBC)
- `slf4j-simple-2.0.9.jar` - SLF4J simple implementation

## Database Integrity

- Foreign key constraints ensure data integrity
- Unique constraints prevent duplicate data
- Transactions ensure complex operations are atomic
- Proper error handling for constraint violations

## Notes

- The database file will be created automatically on first run
- All tables are created automatically if they don't exist
- You can inspect the database using any SQLite client or the `sqlite3` command-line tool
- All database operations include proper error handling
- Foreign key constraints are enforced to maintain data integrity