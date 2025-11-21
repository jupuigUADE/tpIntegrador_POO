package org.models;

public class Mesa {
    private final int id;
    private final int capacidad;
    private TableStatus estado;

    public Mesa(int id, int capacidad) {
        this.id = id;
        this.capacidad = capacidad;
        this.estado = TableStatus.LIBRE;
    }

    public int getId() { return id; }
    public int getCapacidad() { return capacidad; }
    public TableStatus getEstado() { return estado; }
    public void setEstado(TableStatus estado) { this.estado = estado; }
}

