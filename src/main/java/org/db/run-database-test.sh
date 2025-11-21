#!/bin/bash

# Complete Restaurant Database Test Script

echo "=== Compiling Complete Restaurant Database Project ==="

# Create target/classes directory if it doesn't exist
mkdir -p target/classes

# Compile all Java source files
javac -cp sqlite-jdbc-3.43.2.0.jar -d target/classes src/main/java/org/models/*.java src/main/java/org/example/*.java

if [ $? -eq 0 ]; then
    echo "✓ Compilation successful"
    
    echo "=== Running Complete Restaurant Database Test ==="
    java --enable-native-access=ALL-UNNAMED -cp "target/classes:sqlite-jdbc-3.43.2.0.jar:slf4j-api-2.0.9.jar:slf4j-simple-2.0.9.jar" org.example.DatabaseTest
    
    echo ""
    echo "=== Database Schema Information ==="
    echo "Tables created:"
    echo "SELECT name FROM sqlite_master WHERE type='table';" | sqlite3 restaurant.db
    
    echo ""
    echo "=== Quick Data Summary ==="
    echo "Users: $(echo 'SELECT COUNT(*) FROM users;' | sqlite3 restaurant.db)"
    echo "Mesas: $(echo 'SELECT COUNT(*) FROM mesas;' | sqlite3 restaurant.db)"
    echo "Ingredientes: $(echo 'SELECT COUNT(*) FROM ingredientes;' | sqlite3 restaurant.db)"
    echo "Recipes: $(echo 'SELECT COUNT(*) FROM recipes;' | sqlite3 restaurant.db)"
    echo "Stock entries: $(echo 'SELECT COUNT(*) FROM stock;' | sqlite3 restaurant.db)"
    echo "Reservations: $(echo 'SELECT COUNT(*) FROM reservations;' | sqlite3 restaurant.db)"
else
    echo "✗ Compilation failed"
    exit 1
fi