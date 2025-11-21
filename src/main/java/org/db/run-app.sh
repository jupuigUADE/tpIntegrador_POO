#!/bin/bash

# Restaurant Application Launcher

echo "=== Starting Restaurant Management System ==="

# Check if database exists
if [ ! -f "restaurant.db" ]; then
    echo "Database not found. Initializing..."
    java --enable-native-access=ALL-UNNAMED -cp "target/classes:sqlite-jdbc-3.43.2.0.jar:slf4j-api-2.0.9.jar:slf4j-simple-2.0.9.jar" org.example.InitializeUsers
    echo "Database initialized with demo users."
    echo ""
fi

echo "🚀 Launching Restaurant Application..."
echo ""
echo "Login credentials:"
echo "• admin / admin123 (Administrator)"
echo "• waiter1 / waiter123 (Waiter)"
echo "• chef1 / chef123 (Chef)"
echo ""

# Compile if needed
if [ ! -d "target/classes" ] || [ ! "$(find target/classes -name '*.class' 2>/dev/null)" ]; then
    echo "Compiling application..."
    mkdir -p target/classes
    javac -cp sqlite-jdbc-3.43.2.0.jar -d target/classes src/main/java/org/models/*.java src/main/java/org/example/*.java
    
    if [ $? -ne 0 ]; then
        echo "❌ Compilation failed!"
        exit 1
    fi
    echo "✅ Compilation successful"
fi

# Run the application
java --enable-native-access=ALL-UNNAMED -cp "target/classes:sqlite-jdbc-3.43.2.0.jar:slf4j-api-2.0.9.jar:slf4j-simple-2.0.9.jar" org.example.Main