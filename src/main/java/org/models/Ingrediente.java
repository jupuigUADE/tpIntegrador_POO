package org.models;

// Ingrediente.java
public class Ingrediente {
    private int id;
    private String nombre;
    private final Magnitud magnitud; // Ej: kg, litro, unidad

    public Ingrediente(int id, String nombre, Magnitud magnitud) {
        this.id = id;
        this.nombre = nombre;
        this.magnitud = magnitud;
    }

    // Getters
    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public Magnitud getUnidadMedida() { return magnitud; }
}