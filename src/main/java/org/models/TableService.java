package org.models;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.exceptions.TableNotFoundException;

public class TableService {
    private final Map<Integer, Mesa> mesas = new HashMap<>();

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
}
