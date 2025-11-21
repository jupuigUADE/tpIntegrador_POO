# Database Query Examples

## Quick Commands to View Data

### View all tables and their data:
```bash
./view-database.sh
```

### View specific tables:
```bash
# Users
echo 'SELECT * FROM users;' | sqlite3 -header -column restaurant.db

# Mesas (Tables)
echo 'SELECT * FROM mesas;' | sqlite3 -header -column restaurant.db

# Ingredientes
echo 'SELECT * FROM ingredientes;' | sqlite3 -header -column restaurant.db

# Recipes with details
echo 'SELECT * FROM recipes;' | sqlite3 -header -column restaurant.db

# Stock levels
echo 'SELECT * FROM stock;' | sqlite3 -header -column restaurant.db

# Reservations
echo 'SELECT * FROM reservations;' | sqlite3 -header -column restaurant.db
```

### Advanced Queries:

#### See recipes with their ingredients:
```bash
echo "SELECT r.nombre as recipe, i.nombre as ingredient, ri.cantidad, i.unidad_medida 
FROM recipes r 
JOIN recipe_ingredients ri ON r.id = ri.recipe_id 
JOIN ingredientes i ON ri.ingrediente_id = i.id 
ORDER BY r.nombre, i.nombre;" | sqlite3 -header -column restaurant.db
```

#### See stock levels with ingredient names:
```bash
echo "SELECT i.nombre as ingredient, s.cantidad_actual as current, s.cantidad_minima as minimum,
CASE WHEN s.cantidad_actual <= s.cantidad_minima THEN 'LOW STOCK' ELSE 'OK' END as status
FROM stock s 
JOIN ingredientes i ON s.ingrediente_id = i.id 
ORDER BY s.cantidad_actual;" | sqlite3 -header -column restaurant.db
```

#### See available tables:
```bash
echo "SELECT id, capacidad as capacity, estado as status FROM mesas WHERE estado = 'LIBRE';" | sqlite3 -header -column restaurant.db
```

#### See pending reservations:
```bash
echo "SELECT customer_name, guests, reservation_time, status FROM reservations WHERE status = 'PENDING';" | sqlite3 -header -column restaurant.db
```

### Database Schema:
```bash
echo '.schema' | sqlite3 restaurant.db
```

### Table Structure:
```bash
# See structure of a specific table
echo '.schema users' | sqlite3 restaurant.db
echo '.schema recipes' | sqlite3 restaurant.db
```

### Interactive SQLite Shell:
```bash
sqlite3 restaurant.db
# Then you can run any SQL commands interactively
# Type .exit to quit
```

## Useful SQLite Commands in Interactive Mode:

```sql
-- List all tables
.tables

-- Show table schema
.schema tablename

-- Show all schemas
.schema

-- Show data with headers and columns
.mode column
.headers on
SELECT * FROM users;

-- Export data to CSV
.mode csv
.output users.csv
SELECT * FROM users;
.output stdout

-- Count records in all tables
SELECT 'users' as table_name, COUNT(*) as count FROM users
UNION ALL
SELECT 'mesas', COUNT(*) FROM mesas
UNION ALL  
SELECT 'ingredientes', COUNT(*) FROM ingredientes
UNION ALL
SELECT 'recipes', COUNT(*) FROM recipes
UNION ALL
SELECT 'stock', COUNT(*) FROM stock
UNION ALL
SELECT 'reservations', COUNT(*) FROM reservations;
```