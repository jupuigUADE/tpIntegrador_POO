package org.models;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import org.exceptions.TableNotFoundException;

public class TableService {
    private final Map<Integer, Mesa> mesas = new HashMap<>();
    // Orders per table
    private final Map<Integer, List<OrderItem>> orders = new HashMap<>();

    // Simple listeners notified when orders change
    private final List<Runnable> orderListeners = new ArrayList<>();

    // Optional production integration
    private Pedido productionService = null;
    private Menu menu = null;

    public synchronized void setProductionIntegration(Pedido productionService, Menu menu) {
        this.productionService = productionService;
        this.menu = menu;

        // register a listener so when jobs update we refresh order ETAs
        productionService.addJobListener(job -> {
            // update matching order items for this job
            synchronized (this) {
                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                for (List<OrderItem> list : orders.values()) {
                    for (OrderItem oi : list) {
                        Integer pid = oi.getProductionJobId();
                        if (pid != null && pid.equals(job.getId())) {
                            switch (job.getStatus()) {
                                case IN_PROGRESS -> {
                                    if (job.getStartedAt() != null) {
                                        long dur = (long) job.getRecipe().getTiempoPreparacionMinutos() * (long) job.getQuantity();
                                        long elapsed = java.time.Duration.between(job.getStartedAt(), now).toMinutes();
                                        long remaining = Math.max(0L, dur - elapsed);
                                        oi.setEstimatedMinutes(remaining);
                                    }
                                    oi.setEstado(EstadoPedido.IN_PROGRESS);
                                }
                                case DONE -> {
                                    oi.setEstimatedMinutes(0L);
                                    oi.setEstado(EstadoPedido.DONE);
                                }
                                case QUEUED -> {
                                    // queued -> mark as queued
                                    oi.setEstado(EstadoPedido.QUEUED);
                                }
                                case FAILED -> {
                                    // map failed jobs to CANCELLED for orders (cancellations and failures)
                                    oi.setEstimatedMinutes(null);
                                    oi.setEstado(EstadoPedido.CANCELLED);
                                }
                            }
                        }
                    }
                }
            }
            notifyOrderListeners();
        });
    }

    public void addMesa(Mesa m) {
        mesas.put(m.getId(), m);
    }

    public Optional<Mesa> findAvailableTable(int guests) {
        return mesas.values().stream()
                .filter(m -> m.getEstado() == TableStatus.LIBRE && m.getCapacidad() >= guests)
                .findFirst();
    }

    // Throws TableNotFoundException if the mesaId is unknown
    public boolean assignTable(int mesaId) throws TableNotFoundException {
        Mesa m = mesas.get(mesaId);
        if (m == null) throw new TableNotFoundException("Mesa ID " + mesaId + " no encontrada");
        if (m.getEstado() != TableStatus.LIBRE) return false;
        m.setEstado(TableStatus.RESERVADA);
        return true;
    }

    public boolean occupyTable(int mesaId) throws TableNotFoundException {
        Mesa m = mesas.get(mesaId);
        if (m == null) throw new TableNotFoundException("Mesa ID " + mesaId + " no encontrada");
        m.setEstado(TableStatus.OCUPADA);
        return true;
    }

    public boolean releaseTable(int mesaId) throws TableNotFoundException {
        Mesa m = mesas.get(mesaId);
        if (m == null) throw new TableNotFoundException("Mesa ID " + mesaId + " no encontrada");
        m.setEstado(TableStatus.LIBRE);
        return true;
    }

    public Map<Integer, Mesa> getMesas() { return mesas; }

    // Orders API
    public synchronized void addOrder(int mesaId, OrderItem item) throws TableNotFoundException {
        if (!mesas.containsKey(mesaId)) throw new TableNotFoundException("Mesa ID " + mesaId + " no encontrada");
        // mark table as occupied when someone places an order (they are sitting)
        Mesa m = mesas.get(mesaId);
        if (m != null && m.getEstado() != TableStatus.OCUPADA) {
            m.setEstado(TableStatus.OCUPADA);
        }
        // Treat this OrderItem as a new order: ensure its state is QUEUED and clear any previous job linkage
        item.setEstado(EstadoPedido.QUEUED);
        item.setEstimatedMinutes(null);
        item.setProductionJobId(null);

        orders.computeIfAbsent(mesaId, k -> new ArrayList<>()).add(item);

        // If integrated with production and item links to a recipe, create a production job automatically
        if (productionService != null && menu != null && item.getRecipeId() != null) {
            Recipe r = menu.getRecipeById(item.getRecipeId());
            if (r != null) {
                // Estimate ETA in minutes before creating the job
                long etaMinutes = productionService.estimateCompletionForNewJob(r, item.getQuantity());
                ProductionJob job = productionService.createJob(r, item.getQuantity());
                if (job != null) {
                    // Link the created job to the order item and set ETA. Note: createJob notifies production listeners
                    // immediately, so we update the order item state here to reflect the new queued job.
                    item.setProductionJobId(job.getId());
                    item.setEstimatedMinutes(etaMinutes);
                    item.setEstado(EstadoPedido.QUEUED);
                }
            }
        }
        notifyOrderListeners();
    }

    public synchronized List<OrderItem> getOrders(int mesaId) throws TableNotFoundException {
        if (!mesas.containsKey(mesaId)) throw new TableNotFoundException("Mesa ID " + mesaId + " no encontrada");
        return Collections.unmodifiableList(orders.getOrDefault(mesaId, new ArrayList<>()));
    }

    public synchronized void clearOrders(int mesaId) throws TableNotFoundException {
        if (!mesas.containsKey(mesaId)) throw new TableNotFoundException("Mesa ID " + mesaId + " no encontrada");
        // If integrated with production, cancel any linked production jobs before removing orders
        List<OrderItem> list = orders.get(mesaId);
        if (list != null && !list.isEmpty()) {
            for (OrderItem oi : list) {
                // mark cancelled locally
                oi.setEstado(EstadoPedido.CANCELLED);
                Integer pid = oi.getProductionJobId();
                if (pid != null && productionService != null) {
                    try { productionService.cancelJob(pid); } catch (Exception ignored) {}
                }
            }
        }
        // remove orders entry
        orders.remove(mesaId);
        notifyOrderListeners();
    }

    // Mark all orders for a table as CANCELLED and cancel their production jobs if present
    public synchronized void cancelOrdersForTable(int mesaId) throws TableNotFoundException {
        if (!mesas.containsKey(mesaId)) throw new TableNotFoundException("Mesa ID " + mesaId + " no encontrada");
        List<OrderItem> list = orders.get(mesaId);
        if (list == null || list.isEmpty()) return;
        for (OrderItem oi : list) {
            oi.setEstado(EstadoPedido.CANCELLED);
            Integer pid = oi.getProductionJobId();
            if (pid != null && productionService != null) {
                try {
                    productionService.cancelJob(pid);
                } catch (Exception ignored) {}
            }
        }
        // keep the orders in the map but marked cancelled so UI shows them as cancelled
        notifyOrderListeners();
    }

    // Remove a single order item by index for the given table. Cancels linked production job if present.
    public synchronized boolean removeOrderItem(int mesaId, int index) throws TableNotFoundException {
        if (!mesas.containsKey(mesaId)) throw new TableNotFoundException("Mesa ID " + mesaId + " no encontrada");
        List<OrderItem> list = orders.get(mesaId);
        if (list == null || list.isEmpty()) return false;
        if (index < 0 || index >= list.size()) return false;
        OrderItem oi = list.remove(index);
        Integer pid = oi.getProductionJobId();
        if (pid != null && productionService != null) {
            try { productionService.cancelJob(pid); } catch (Exception ignored) {}
        }
        // if list becomes empty, keep empty list or remove the entry to match clearOrders behaviour
        if (list.isEmpty()) orders.remove(mesaId);
        notifyOrderListeners();
        return true;
    }

    public synchronized void addOrderListener(Runnable l) {
        orderListeners.add(l);
    }

    public synchronized void removeOrderListener(Runnable l) {
        orderListeners.remove(l);
    }

    private void notifyOrderListeners() {
        for (Runnable r : new ArrayList<>(orderListeners)) {
            try { r.run(); } catch (Exception ignored) {}
        }
    }
}
