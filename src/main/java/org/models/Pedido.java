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
        // Try to start immediately if stock is available; if not, leave the job queued.
        try {
            if (recipe.canProduce(stock, quantity)) {
                // startJob will handle deductions and change status to IN_PROGRESS
                startJob(id);
            }
        } catch (Exception ignored) {
            // if start fails, keep job queued or let startJob mark it failed
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

    // Estimate completion time in minutes from now for a newly created job (without actually adding it).
    // This sums remaining time of the in-progress job and durations of queued jobs, then adds the new job duration.
    public synchronized long estimateCompletionForNewJob(Recipe recipe, int quantity) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        long minutesAhead = 0L;

        // consider jobs sorted by id
        List<ProductionJob> existing = listJobs();
        for (ProductionJob j : existing) {
            if (j.getStatus() == ProductionJob.Status.DONE || j.getStatus() == ProductionJob.Status.FAILED) continue;
            long dur = (long) j.getRecipe().getTiempoPreparacionMinutos() * (long) j.getQuantity();
            if (j.getStatus() == ProductionJob.Status.IN_PROGRESS) {
                if (j.getStartedAt() != null) {
                    long elapsed = java.time.Duration.between(j.getStartedAt(), now).toMinutes();
                    long remaining = dur - elapsed;
                    minutesAhead += Math.max(0L, remaining);
                } else {
                    minutesAhead += dur; // fallback
                }
            } else if (j.getStatus() == ProductionJob.Status.QUEUED) {
                minutesAhead += dur;
            }
        }

        // add new job duration
        minutesAhead += (long) recipe.getTiempoPreparacionMinutos() * (long) quantity;

        return minutesAhead;
    }

    // Estimate remaining minutes from now until the given jobId would be finished.
    // Returns -1 if job not found.
    public synchronized long estimateRemainingMinutesForJob(int jobId) {
        ProductionJob target = jobs.get(jobId);
        if (target == null) return -1L;

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        long minutesAhead = 0L;

        // consider jobs in order
        List<ProductionJob> existing = listJobs();
        for (ProductionJob j : existing) {
            if (j.getStatus() == ProductionJob.Status.DONE || j.getStatus() == ProductionJob.Status.FAILED) continue;
            int durMinutes = j.getRecipe().getTiempoPreparacionMinutos() * j.getQuantity();

            if (j.getStatus() == ProductionJob.Status.IN_PROGRESS) {
                if (j.getStartedAt() != null) {
                    long elapsed = java.time.Duration.between(j.getStartedAt(), now).toMinutes();
                    long remaining = Math.max(0L, durMinutes - elapsed);
                    minutesAhead += remaining;
                } else {
                    minutesAhead += durMinutes;
                }
            } else if (j.getStatus() == ProductionJob.Status.QUEUED) {
                minutesAhead += durMinutes;
            }

            if (j.getId() == jobId) break; // stop when we have included the target job
        }

        return minutesAhead;
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

        // After finishing a job, attempt to start the next queued job that can be produced
        // (iterate in queue order and start the first one with sufficient stock)
        for (Integer qid : new ArrayList<>(queue)) {
            ProductionJob next = jobs.get(qid);
            if (next == null) continue;
            if (next.getStatus() != ProductionJob.Status.QUEUED) continue;
            Recipe r = next.getRecipe();
            int qty = next.getQuantity();
            try {
                if (r.canProduce(stock, qty)) {
                    // startJob will remove it from the queue and notify listeners
                    boolean started = startJob(qid);
                    if (started) break; // only start one job now
                }
            } catch (Exception ignored) {
                // if check or start fails, try next queued job
            }
        }

        return true;
    }

    // Cancel a job (mark FAILED, set note) and notify listeners.
    public synchronized boolean cancelJob(int jobId) {
        ProductionJob job = jobs.get(jobId);
        if (job == null) return false;
        // if already finished, nothing to do
        if (job.getStatus() == ProductionJob.Status.DONE || job.getStatus() == ProductionJob.Status.FAILED) return false;
        job.setFinishedAt(LocalDateTime.now());
        job.setStatus(ProductionJob.Status.FAILED);
        job.setNote("Cancelled");
        // remove from queue if present
        queue.remove((Integer) jobId);
        for (Consumer<ProductionJob> l : jobListeners) { try { l.accept(job); } catch (Exception ignored) {} }
        return true;
    }
}

