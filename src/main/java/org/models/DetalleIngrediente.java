package org.models;

public class DetalleIngrediente {
    private final Ingrediente ingrediente;
    private double cantidadActual;
    private final int cantidadMinima; // Cantidad mínima para hacer un pedido

    public DetalleIngrediente(Ingrediente ingrediente, double cantidadInicial, int cantidadMinima) {
        this.ingrediente = ingrediente;
        this.cantidadActual = cantidadInicial;
        this.cantidadMinima = cantidadMinima;
    }

    // Métodos para actualizar stock
    public void registrarEntrada(double cantidad) {
        this.cantidadActual += cantidad;
    }

    public void registrarSalida(double cantidad) {
        this.cantidadActual -= cantidad;
    }

    // Getter para verificar si se necesita reordenar
    public boolean necesitaReorden() {
        return this.cantidadActual <= this.cantidadMinima;
    }

    // Getters
    public Ingrediente getIngrediente() { return ingrediente; }
    public double getCantidadActual() { return cantidadActual; }
    public int getCantidadMinima() { return cantidadMinima; }
}