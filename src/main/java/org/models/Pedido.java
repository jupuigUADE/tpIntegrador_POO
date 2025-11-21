package org.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.exceptions.InvalidQuantityException;
import org.exceptions.StockNotFoundException;
import org.exceptions.InsufficientStockException;
import org.exceptions.MovementNotSupportedException;

public class Pedido {
    private final Map<Integer, ProductionJob> jobs = new HashMap<>();
    private final List<Integer> queue = new ArrayList<>();
    private int nextId = 1;
    private final StockGeneral stock;
    // simple listeners for job creation/changes
    private final List<Consumer<ProductionJob>> jobListeners = new ArrayList<>();

    public Pedido(StockGeneral stock) {
        this.stock = stock;
    }

    public synchronized ProductionJob createJob(Recipe recipe, int quantity) {
        int id = nextId++;
        ProductionJob job = new ProductionJob(id, recipe, quantity);
        jobs.put(id, job);
        queue.add(id);
        // notify listeners
        for (Consumer<ProductionJob> l : jobListeners) {
            try { l.accept(job); } catch (Exception ignored) {}
        }
        return job;
    }

    // allow listeners to be notified when new jobs are created
    public synchronized void addJobListener(Consumer<ProductionJob> listener) {
        jobListeners.add(listener);
    }
    public synchronized void removeJobListener(Consumer<ProductionJob> listener) {
        jobListeners.remove(listener);
    }

    public synchronized List<ProductionJob> listJobs() {
        List<ProductionJob> res = new ArrayList<>(jobs.values());
        Collections.sort(res, (a,b) -> Integer.compare(a.getId(), b.getId()));
        return res;
    }

    public synchronized List<Integer> getQueue() {
        return new ArrayList<>(queue);
    }

    // Attempt to start a job: will check stock and deduct ingredients atomically (attempt rollback on failure)
    public synchronized boolean startJob(int jobId) {
        ProductionJob job = jobs.get(jobId);
        if (job == null) return false;
        if (job.getStatus() != ProductionJob.Status.QUEUED) return false;

        Recipe recipe = job.getRecipe();
        int qty = job.getQuantity();

        // Check availability
        if (!recipe.canProduce(stock, qty)) {
            job.setStatus(ProductionJob.Status.FAILED);
            job.setFinishedAt(LocalDateTime.now());
            job.setNote("Insufficient stock when starting");
            // notify listeners of status change
            for (Consumer<ProductionJob> l : jobListeners) { try { l.accept(job); } catch (Exception ignored) {} }
            return false;
        }

        // Deduct ingredients; if any deduction fails, rollback previous deductions
        Map<Integer, Double> deducted = new HashMap<>();
        for (RecipeIngredient ri : recipe.getIngredientes()) {
            int idIng = ri.getIngrediente().getId();
            double amount = ri.getCantidad() * qty;
            try {
                Double remaining = stock.registrarMovimiento(idIng, amount, TipoMovimiento.SALIDA);
                if (remaining == null) {
                    // rollback
                    for (Map.Entry<Integer, Double> d : deducted.entrySet()) {
                        try { stock.registrarMovimiento(d.getKey(), d.getValue(), TipoMovimiento.ENTRADA); } catch (Exception ignored) {}
                    }
                    job.setStatus(ProductionJob.Status.FAILED);
                    job.setFinishedAt(LocalDateTime.now());
                    job.setNote("Failed to deduct ingredients");
                    // notify listeners
                    for (Consumer<ProductionJob> l : jobListeners) { try { l.accept(job); } catch (Exception ignored) {} }
                    return false;
                }
                deducted.put(idIng, amount);
            } catch (InsufficientStockException | StockNotFoundException | InvalidQuantityException | MovementNotSupportedException ex) {
                // rollback
                for (Map.Entry<Integer, Double> d : deducted.entrySet()) {
                    try { stock.registrarMovimiento(d.getKey(), d.getValue(), TipoMovimiento.ENTRADA); } catch (Exception ignored) {}
                }
                job.setStatus(ProductionJob.Status.FAILED);
                job.setFinishedAt(LocalDateTime.now());
                job.setNote("Failed to deduct ingredients: " + ex.getMessage());
                // notify listeners
                for (Consumer<ProductionJob> l : jobListeners) { try { l.accept(job); } catch (Exception ignored) {} }
                return false;
            }
        }

        // All deducted OK — mark in progress
        job.setStatus(ProductionJob.Status.IN_PROGRESS);
        job.setStartedAt(LocalDateTime.now());
        queue.remove((Integer) jobId);
        // notify listeners
        for (Consumer<ProductionJob> l : jobListeners) { try { l.accept(job); } catch (Exception ignored) {} }
        return true;
    }

    public synchronized boolean finishJob(int jobId, boolean success) {
        ProductionJob job = jobs.get(jobId);
        if (job == null) return false;
        if (job.getStatus() != ProductionJob.Status.IN_PROGRESS) return false;
        job.setFinishedAt(LocalDateTime.now());
        job.setStatus(success ? ProductionJob.Status.DONE : ProductionJob.Status.FAILED);
        // notify listeners
        for (Consumer<ProductionJob> l : jobListeners) { try { l.accept(job); } catch (Exception ignored) {} }
        return true;
    }
}
