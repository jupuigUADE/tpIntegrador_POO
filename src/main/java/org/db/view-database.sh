#!/bin/bash

# Database Viewer Script - View all tables and their data

DB_FILE="restaurant.db"

if [ ! -f "$DB_FILE" ]; then
    echo "❌ Database file '$DB_FILE' not found!"
    echo "Run './run-database-test.sh' first to create the database."
    exit 1
fi

echo "=== Restaurant Database Contents ==="
echo "Database file: $DB_FILE"
echo "Created: $(ls -la $DB_FILE | awk '{print $6, $7, $8}')"
echo ""

echo "📊 DATABASE OVERVIEW"
echo "==================="
echo "Total tables: $(echo "SELECT COUNT(*) FROM sqlite_master WHERE type='table';" | sqlite3 $DB_FILE)"
echo ""

# Function to display table data with formatting
show_table() {
    local table_name=$1
    local count=$(echo "SELECT COUNT(*) FROM $table_name;" | sqlite3 $DB_FILE 2>/dev/null)
    
    if [ $? -eq 0 ]; then
        echo "📋 TABLE: $table_name ($count records)"
        echo "$(printf '=%.0s' {1..50})"
        
        if [ $count -gt 0 ]; then
            echo "SELECT * FROM $table_name;" | sqlite3 -header -column $DB_FILE
        else
            echo "(empty table)"
        fi
        echo ""
    fi
}

# Get all user tables (exclude sqlite internal tables)
tables=$(echo "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name;" | sqlite3 $DB_FILE)

for table in $tables; do
    show_table $table
done

echo "🔍 QUICK STATS"
echo "=============="
echo "Users: $(echo 'SELECT COUNT(*) FROM users;' | sqlite3 $DB_FILE)"
echo "Mesas: $(echo 'SELECT COUNT(*) FROM mesas;' | sqlite3 $DB_FILE)"
echo "Ingredientes: $(echo 'SELECT COUNT(*) FROM ingredientes;' | sqlite3 $DB_FILE)"
echo "Recipes: $(echo 'SELECT COUNT(*) FROM recipes;' | sqlite3 $DB_FILE)"
echo "Recipe Ingredients: $(echo 'SELECT COUNT(*) FROM recipe_ingredients;' | sqlite3 $DB_FILE)"
echo "Stock Entries: $(echo 'SELECT COUNT(*) FROM stock;' | sqlite3 $DB_FILE)"
echo "Reservations: $(echo 'SELECT COUNT(*) FROM reservations;' | sqlite3 $DB_FILE)"

echo ""
echo "💡 TIP: To see only a specific table, use:"
echo "   echo 'SELECT * FROM tablename;' | sqlite3 -header -column restaurant.db"
echo ""
echo "💡 To see the database schema:"
echo "   echo '.schema' | sqlite3 restaurant.db"