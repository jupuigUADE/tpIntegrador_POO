package org.models;

// Small helper linking an Ingrediente with the quantity required in a recipe
public class RecipeIngredient {
    private final Ingrediente ingrediente;
    private final double cantidad; // in the unidad of the ingrediente

    public RecipeIngredient(Ingrediente ingrediente, double cantidad) {
        this.ingrediente = ingrediente;
        this.cantidad = cantidad;
    }

    public Ingrediente getIngrediente() { return ingrediente; }
    public double getCantidad() { return cantidad; }
}

