package org.models;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.exceptions.InvalidQuantityException;
import org.exceptions.StockNotFoundException;
import org.exceptions.InsufficientStockException;
import org.exceptions.MovementNotSupportedException;

public class Recipe {
    private final int id;
    private final String nombre;
    private final List<RecipeIngredient> ingredientes;
    private final int tiempoPreparacionMinutos;
    private final int precio;
    private final Mesa mesa;

    public Recipe(int id, String nombre, List<RecipeIngredient> ingredientes, int tiempoPreparacionMinutos, int precio) {
        this.id = id;
        this.nombre = nombre;
        this.ingredientes = ingredientes;
        this.tiempoPreparacionMinutos = tiempoPreparacionMinutos;
        this.precio = precio;
        this.mesa = null;
    }

    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public List<RecipeIngredient> getIngredientes() { return ingredientes; }
    public int getTiempoPreparacionMinutos() { return tiempoPreparacionMinutos; }
    public int getPrecio() { return precio; }

    @Override
    public String toString() {
        return nombre;
    }

    public boolean canProduce(StockGeneral stock, int cantidad) {
        for (RecipeIngredient ri : ingredientes) {
            DetalleIngrediente s = stock.obtenerStockPorId(ri.getIngrediente().getId());
            if (s == null) return false;
            double required = ri.getCantidad() * cantidad;
            if (s.getCantidadActual() < required) return false;
        }
        return true;
    }

    public boolean produce(StockGeneral stock, int cantidad) {
        if (!canProduce(stock, cantidad)) return false;
        Map<Integer, Double> deducted = new HashMap<>();
        for (RecipeIngredient ri : ingredientes) {
            int idIng = ri.getIngrediente().getId();
            double amount = ri.getCantidad() * cantidad;
            try {
                Double remaining = stock.registrarMovimiento(idIng, amount, TipoMovimiento.SALIDA);
                if (remaining == null) {
                    // treat as failure and rollback
                    for (Map.Entry<Integer, Double> d : deducted.entrySet()) {
                        try { stock.registrarMovimiento(d.getKey(), d.getValue(), TipoMovimiento.ENTRADA); } catch (Exception ignored) {}
                    }
                    return false;
                }
                deducted.put(idIng, amount);
            } catch (InsufficientStockException | StockNotFoundException | InvalidQuantityException | MovementNotSupportedException ex) {
                // rollback previous deductions
                for (Map.Entry<Integer, Double> d : deducted.entrySet()) {
                    try { stock.registrarMovimiento(d.getKey(), d.getValue(), TipoMovimiento.ENTRADA); } catch (Exception ignored) {}
                }
                return false;
            }
        }
        return true;
    }
}
