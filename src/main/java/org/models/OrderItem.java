package org.models;

public class OrderItem {
    private final String name;
    private final int quantity;
    private final Integer recipeId; // optional link to a Recipe
    private Integer productionJobId; // filled when a production job is created
    // estimated time-to-complete in minutes (nullable)
    private Long estimatedMinutes;
    private EstadoPedido estado = EstadoPedido.QUEUED;

    public OrderItem(String name, int quantity) {
        this(name, quantity, null);
    }

    public OrderItem(String name, int quantity, Integer recipeId) {
        this.name = name;
        this.quantity = quantity;
        this.recipeId = recipeId;
    }

    public String getName() { return name; }
    public int getQuantity() { return quantity; }
    public Integer getRecipeId() { return recipeId; }
    public Integer getProductionJobId() { return productionJobId; }
    public void setProductionJobId(Integer id) { this.productionJobId = id; }
    public Long getEstimatedMinutes() { return estimatedMinutes; }
    public void setEstimatedMinutes(Long m) { this.estimatedMinutes = m; }
    public EstadoPedido getEstado() { return estado; }
    public void setEstado(EstadoPedido estado) { this.estado = estado; }

    @Override
    public String toString() {
        String s = quantity + " x " + name;
        if (productionJobId != null) s += " (job#" + productionJobId + ")";
        if (estimatedMinutes != null) s += " - ETA: " + estimatedMinutes + " min";
        if (estado != null && estado != EstadoPedido.QUEUED) s += " - " + estado;
        return s;
    }
}
