package org.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.function.Consumer;
import org.exceptions.InvalidQuantityException;
import org.exceptions.StockNotFoundException;
import org.exceptions.InsufficientStockException;
import org.exceptions.MovementNotSupportedException;

public class StockGeneral {
    private final HashMap<Integer, DetalleIngrediente> inventario;
    private final List<Consumer<DetalleIngrediente>> listeners = new ArrayList<>();
    private static final Logger logger = Logger.getLogger(StockGeneral.class.getName());

    public StockGeneral() {
        this.inventario = new HashMap<>();
    }

    // Listener registration so external UI or services can react to stock changes
    public synchronized void addStockListener(Consumer<DetalleIngrediente> listener) {
        if (listener != null) listeners.add(listener);
    }

    public synchronized void removeStockListener(Consumer<DetalleIngrediente> listener) {
        listeners.remove(listener);
    }

    private synchronized void notifyListeners(DetalleIngrediente stock) {
        for (Consumer<DetalleIngrediente> l : new ArrayList<>(listeners)) {
            try { l.accept(stock); } catch (Exception ignored) {}
        }
    }

    public synchronized void agregarStock(DetalleIngrediente stock) {
        int id = stock.getIngrediente().getId();
        inventario.put(id, stock);
        logger.log(Level.INFO, "Agregado al inventario: {0}", stock.getIngrediente().getNombre());
        notifyListeners(stock);
    }

    public synchronized DetalleIngrediente obtenerStockPorId(int ingredienteId) {
        return inventario.get(ingredienteId);
    }

    // Throws exceptions on error conditions
    public synchronized void modificarStock(int ingredienteId, double cantidad, TipoMovimiento tipo)
            throws InvalidQuantityException, StockNotFoundException, InsufficientStockException, MovementNotSupportedException {
        if (cantidad <= 0) {
            logger.log(Level.WARNING, "Cantidad inválida: {0}", cantidad);
            throw new InvalidQuantityException("Cantidad inválida: " + cantidad);
        }
        DetalleIngrediente stock = obtenerStockPorId(ingredienteId);
        if (stock == null) {
            logger.log(Level.WARNING, "Ingrediente ID {0} no existe en el inventario.", ingredienteId);
            throw new StockNotFoundException("Ingrediente ID " + ingredienteId + " no existe en el inventario.");
        }

        switch (tipo) {
            case ENTRADA:
                stock.registrarEntrada(cantidad);
                logger.log(Level.INFO, "ENTRADA {0} {1}", new Object[]{cantidad, stock.getIngrediente().getUnidadMedida()});
                notifyListeners(stock);
                return;
            case SALIDA:
                if (stock.getCantidadActual() >= cantidad) {
                    stock.registrarSalida(cantidad);
                    logger.log(Level.INFO, "SALIDA {0} {1}", new Object[]{cantidad, stock.getIngrediente().getUnidadMedida()});

                    // Automatic reorder: if after the salida the stock is at or below minimum, place a replenishment
                    if (stock.necesitaReorden()) {
                        try {
                            autoReorder(stock);
                        } catch (Exception ex) {
                            logger.log(Level.SEVERE, "Auto-reorder failed for {0}: {1}", new Object[]{stock.getIngrediente().getNombre(), ex.getMessage()});
                        }
                    }

                    // Notify listeners after any change (including auto-reorder that may have happened)
                    notifyListeners(stock);

                    return;
                } else {
                    logger.log(Level.WARNING, "Stock insuficiente para ID {0}: falta {1}", new Object[]{ingredienteId, (cantidad - stock.getCantidadActual())});
                    throw new InsufficientStockException("Stock insuficiente para ID " + ingredienteId + ": falta " + (cantidad - stock.getCantidadActual()));
                }
            default:
                logger.log(Level.WARNING, "Tipo de movimiento no soportado: {0}", tipo);
                throw new MovementNotSupportedException("Tipo de movimiento no soportado: " + tipo);
        }
    }

    // Simple auto-reorder implementation: top up to (cantidad_minima * REORDER_MULTIPLIER)
    private static final int REORDER_MULTIPLIER = 5;

    private void autoReorder(DetalleIngrediente stock) {
        int min = stock.getCantidadMinima();
        double current = stock.getCantidadActual();
        double target = min * REORDER_MULTIPLIER;
        double needed = target - current;
        if (needed <= 0) {
            // Nothing to do
            logger.log(Level.INFO, "Auto-reorder not needed for {0}. current={1}, target={2}", new Object[]{stock.getIngrediente().getNombre(), current, target});
            return;
        }

        // In a real system this would create a purchase order; here we simulate immediate replenishment
        stock.registrarEntrada(needed);
        logger.log(Level.INFO, "AUTO-REORDER: Replenished {0} by {1} {2} (new qty={3})",
                new Object[]{stock.getIngrediente().getNombre(), needed, stock.getIngrediente().getUnidadMedida(), stock.getCantidadActual()});
        // notify after simulated replenishment
        notifyListeners(stock);
    }

    public synchronized Double registrarMovimiento(int ingredienteId, double cantidad, TipoMovimiento tipo)
            throws InvalidQuantityException, StockNotFoundException, InsufficientStockException, MovementNotSupportedException {
        modificarStock(ingredienteId, cantidad, tipo);
        DetalleIngrediente s = obtenerStockPorId(ingredienteId);
        return s != null ? s.getCantidadActual() : null;
    }

    public synchronized Map<Integer, DetalleIngrediente> getInventario() {
        return Collections.unmodifiableMap(new HashMap<>(inventario));
    }
}