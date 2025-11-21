package org.models;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.exceptions.InvalidQuantityException;
import org.exceptions.StockNotFoundException;
import org.exceptions.InsufficientStockException;
import org.exceptions.MovementNotSupportedException;

public class StockGeneral {
    private final HashMap<Integer, DetalleIngrediente> inventario;
    private static final Logger logger = Logger.getLogger(StockGeneral.class.getName());

    public StockGeneral() {
        this.inventario = new HashMap<>();
    }

    public synchronized void agregarStock(DetalleIngrediente stock) {
        int id = stock.getIngrediente().getId();
        inventario.put(id, stock);
        logger.log(Level.INFO, "Agregado al inventario: {0}", stock.getIngrediente().getNombre());
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
                return;
            case SALIDA:
                if (stock.getCantidadActual() >= cantidad) {
                    stock.registrarSalida(cantidad);
                    logger.log(Level.INFO, "SALIDA {0} {1}", new Object[]{cantidad, stock.getIngrediente().getUnidadMedida()});
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