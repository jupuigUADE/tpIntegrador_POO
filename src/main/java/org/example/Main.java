package org.example;

import org.gui.MainGui;
import org.models.*;

import javax.swing.SwingUtilities;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        // Build initial model data
        StockGeneral sistemaStock = new StockGeneral();

        Ingrediente harina = new Ingrediente(101, "Harina de Trigo", Magnitud.KILOGRAMO);
        Ingrediente queso = new Ingrediente(202, "Queso Mozzarella", Magnitud.KILOGRAMO);
        Ingrediente tomate = new Ingrediente(303, "Tomate", Magnitud.KILOGRAMO);
        Ingrediente levadura = new Ingrediente(404, "Levadura", Magnitud.GRAMO);

        DetalleIngrediente stockHarina = new DetalleIngrediente(harina, 50.0, 10);
        DetalleIngrediente stockQueso = new DetalleIngrediente(queso, 25.0, 5);
        DetalleIngrediente stockTomate = new DetalleIngrediente(tomate, 20.0, 5);
        DetalleIngrediente stockLevadura = new DetalleIngrediente(levadura, 1000.0, 200);

        sistemaStock.agregarStock(stockHarina);
        sistemaStock.agregarStock(stockQueso);
        sistemaStock.agregarStock(stockTomate);
        sistemaStock.agregarStock(stockLevadura);

        // Build menu / recipes
        Menu menu = new Menu();

        Recipe pizza = new Recipe(
                1,
                "Pizza Margarita",
                List.of(
                        new RecipeIngredient(harina, 0.3),
                        new RecipeIngredient(levadura, 5.0),
                        new RecipeIngredient(tomate, 0.2),
                        new RecipeIngredient(queso, 0.25)
                ),
                20,
                15
        );

        Recipe pan = new Recipe(
                2,
                "Pan Casero",
                List.of(
                        new RecipeIngredient(harina, 0.5),
                        new RecipeIngredient(levadura, 7.0)
                ),
                40,
                8
        );

        menu.addRecipe(pizza);
        menu.addRecipe(pan);

        // Tables and reservations
        TableService tableService = new TableService();
        tableService.addMesa(new Mesa(1, 2));
        tableService.addMesa(new Mesa(2, 4));
        tableService.addMesa(new Mesa(3, 4));
        tableService.addMesa(new Mesa(4, 6));

        ReservationService reservationService = new ReservationService(tableService);

        // Production service
        Pedido productionService = new Pedido(sistemaStock);

        // Start Swing UI on EDT after prompting for login
        SwingUtilities.invokeLater(() -> {
            User user = org.example.LoginDialog.showLogin(null);
            if (user == null) {
                // User cancelled login; exit application
                System.out.println("Ningún usuario inició sesión. Cerrando aplicación.");
                System.exit(0);
            }

            MainGui gui = new MainGui(sistemaStock, menu, reservationService, tableService, productionService, user);
            gui.setVisible(true);
        });
    }
}