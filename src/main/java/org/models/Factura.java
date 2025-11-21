package org.models;

import java.util.List;
import java.util.ArrayList;

public class Factura {
    private final int tableId;
    private final List<OrderItem> items;
    private final double subtotal;
    private final double total;

    public Factura(int tableId, List<OrderItem> items, Menu menu) {
        this.tableId = tableId;
        this.items = new ArrayList<>();
        double sum = 0.0;
        if (items != null) {
            for (OrderItem oi : items) {
                // skip cancelled items
                if (oi.getEstado() == EstadoPedido.CANCELLED) continue;
                this.items.add(oi);
                Integer rid = oi.getRecipeId();
                double unit = 0.0;
                if (rid != null && menu != null) {
                    Recipe r = menu.getRecipeById(rid);
                    if (r != null) unit = r.getPrecio();
                }
                sum += unit * oi.getQuantity();
            }
        }
        this.subtotal = sum;
        this.total = sum; // no taxes by default
    }

    public int getTableId() { return tableId; }
    public List<OrderItem> getItems() { return List.copyOf(items); }
    public double getSubtotal() { return subtotal; }
    public double getTotal() { return total; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Factura - Table ").append(tableId).append('\n');
        sb.append("--------------------------\n");
        for (OrderItem oi : items) {
            sb.append(oi.toString()).append('\n');
        }
        sb.append("--------------------------\n");
        sb.append("Subtotal: ").append(String.format("%.2f", subtotal)).append('\n');
        sb.append("Total:    ").append(String.format("%.2f", total)).append('\n');
        return sb.toString();
    }
}

