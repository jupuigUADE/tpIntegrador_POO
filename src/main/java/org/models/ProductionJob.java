package org.models;

import java.time.LocalDateTime;

public class ProductionJob {
    public enum Status { QUEUED, IN_PROGRESS, DONE, FAILED }

    private final int id;
    private final Recipe recipe;
    private final int quantity;
    private Status status;
    private final LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String note;

    public ProductionJob(int id, Recipe recipe, int quantity) {
        this.id = id;
        this.recipe = recipe;
        this.quantity = quantity;
        this.status = Status.QUEUED;
        this.createdAt = LocalDateTime.now();
    }

    public int getId() { return id; }
    public Recipe getRecipe() { return recipe; }
    public int getQuantity() { return quantity; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}

