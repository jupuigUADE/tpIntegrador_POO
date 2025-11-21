package org.example;

import org.gui.LoginDialog;
import org.gui.MainGui;
import org.models.*;
import org.db.DatabaseHelper;

import javax.swing.SwingUtilities;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        // Build initial model data and load/persist inventory via database
        DatabaseHelper db = new DatabaseHelper();

        // initialize stock and get ingredient references
        StockInitResult init = loadOrSeedStock(db);
        StockGeneral sistemaStock = init.stock;
        Ingrediente harina = init.harina;
        Ingrediente queso = init.queso;
        Ingrediente tomate = init.tomate;
        Ingrediente levadura = init.levadura;

        // Persist stock changes to DB whenever StockGeneral is updated
        sistemaStock.addStockListener(stockItem -> {
            // insert or replace the stock record for this ingredient
            try {
                db.insertStock(stockItem);
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Failed to persist stock for ingredient {0}: {1}", new Object[] { stockItem.getIngrediente().getNombre(), ex.getMessage() });
            }
        });

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
        // Additional tables
        tableService.addMesa(new Mesa(5, 2));
        tableService.addMesa(new Mesa(6, 2));
        tableService.addMesa(new Mesa(7, 4));
        tableService.addMesa(new Mesa(8, 6));

        ReservationService reservationService = new ReservationService(tableService);

        // Production service
        Pedido productionService = new Pedido(sistemaStock);

        // Wire production integration so orders create production jobs and ETAs
        tableService.setProductionIntegration(productionService, menu);

        // Start Swing UI on EDT after prompting for login
        SwingUtilities.invokeLater(() -> {
            User user = LoginDialog.showLogin(null);
            if (user == null) {
                // User cancelled login; exit application
                System.out.println("Ningún usuario inició sesión. Cerrando aplicación.");
                System.exit(0);
            }

            MainGui gui = new MainGui(sistemaStock, menu, reservationService, tableService, productionService, user);
            gui.setVisible(true);
        });
    }

    // Helper container
    private static class StockInitResult {
        final StockGeneral stock;
        final Ingrediente harina, queso, tomate, levadura;
        StockInitResult(StockGeneral stock, Ingrediente harina, Ingrediente queso, Ingrediente tomate, Ingrediente levadura) {
            this.stock = stock; this.harina = harina; this.queso = queso; this.tomate = tomate; this.levadura = levadura;
        }
    }

    // Load stock from DB or seed defaults. Returns the StockGeneral plus ingredient references used by recipes.
    private static StockInitResult loadOrSeedStock(DatabaseHelper db) {
        StockGeneral sistemaStock = new StockGeneral();

        Ingrediente harina = null, queso = null, tomate = null, levadura = null;

        java.util.List<DetalleIngrediente> persisted = db.getAllStock();
        if (persisted != null && !persisted.isEmpty()) {
            for (DetalleIngrediente si : persisted) sistemaStock.agregarStock(si);
            java.util.Map<Integer, Ingrediente> ingrMap = new java.util.HashMap<>();
            for (DetalleIngrediente si : persisted) {
                Ingrediente i = si.getIngrediente(); if (i != null) ingrMap.put(i.getId(), i);
            }
            harina = ingrMap.get(101); queso = ingrMap.get(202); tomate = ingrMap.get(303); levadura = ingrMap.get(404);
        }

        if (harina == null) harina = new Ingrediente(101, "Harina de Trigo", Magnitud.KILOGRAMO);
        if (queso == null) queso = new Ingrediente(202, "Queso Mozzarella", Magnitud.KILOGRAMO);
        if (tomate == null) tomate = new Ingrediente(303, "Tomate", Magnitud.KILOGRAMO);
        if (levadura == null) levadura = new Ingrediente(404, "Levadura", Magnitud.GRAMO);

        if (persisted == null || persisted.isEmpty()) {
            DetalleIngrediente stockHarina = new DetalleIngrediente(harina, 50.0, 10);
            DetalleIngrediente stockQueso = new DetalleIngrediente(queso, 25.0, 5);
            DetalleIngrediente stockTomate = new DetalleIngrediente(tomate, 20.0, 5);
            DetalleIngrediente stockLevadura = new DetalleIngrediente(levadura, 1000.0, 200);
            sistemaStock.agregarStock(stockHarina); sistemaStock.agregarStock(stockQueso); sistemaStock.agregarStock(stockTomate); sistemaStock.agregarStock(stockLevadura);
            // persist seeded ingredients and stock
            try { db.insertIngrediente(harina); db.insertIngrediente(queso); db.insertIngrediente(tomate); db.insertIngrediente(levadura);
                  db.insertStock(stockHarina); db.insertStock(stockQueso); db.insertStock(stockTomate); db.insertStock(stockLevadura);
            } catch (Exception ex) { LOGGER.log(Level.WARNING, "Failed to persist seeded stock: {0}", ex.getMessage()); }
        }

        return new StockInitResult(sistemaStock, harina, queso, tomate, levadura);
    }
}
