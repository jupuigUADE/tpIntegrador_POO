package org.models;

import java.util.HashMap;
import java.util.Map;
import java.util.Collection;
import java.util.Optional;
import org.exceptions.ReservationNotFoundException;
import org.exceptions.TableNotFoundException;

public class ReservationService {
    private final Map<Integer, Reservation> reservations = new HashMap<>();
    private int nextId = 1;
    private final TableService tableService;

    public ReservationService(TableService tableService) {
        this.tableService = tableService;
    }

    public synchronized Reservation createReservation(String customer, int guests, java.time.LocalDateTime when) {
        int id = nextId++;
        Reservation r = new Reservation(id, customer, guests, when);
        reservations.put(id, r);
        return r;
    }

    public synchronized Collection<Reservation> getAll() {
        return reservations.values();
    }

    public synchronized Optional<Reservation> findById(int id) {
        return Optional.ofNullable(reservations.get(id));
    }

    // Try to assign an available table to the reservation; returns assigned table id or null
    public synchronized Integer assignTableForReservation(int reservationId) {
        Reservation r = reservations.get(reservationId);
        if (r == null) return null;
        if (r.getTableId() != null) return r.getTableId();
        Optional<Mesa> opt = tableService.findAvailableTable(r.getGuests());
        if (opt.isPresent()) {
            Mesa m = opt.get();
            try {
                boolean ok = tableService.assignTable(m.getId());
                if (ok) {
                    r.setTableId(m.getId());
                    r.setStatus(ReservationStatus.CONFIRMED);
                    return m.getId();
                }
            } catch (TableNotFoundException tnfe) {
                // Should not happen: table came from tableService; log or ignore
                return null;
            }
        }
        return null;
    }

    public synchronized boolean cancelReservation(int reservationId) {
        Reservation r = reservations.get(reservationId);
        if (r == null) return false;
        r.setStatus(ReservationStatus.CANCELLED);
        if (r.getTableId() != null) {
            try { tableService.releaseTable(r.getTableId()); } catch (TableNotFoundException ignored) {}
            r.setTableId(null);
        }
        return true;
    }

    public synchronized boolean seatReservation(int reservationId) {
        Reservation r = reservations.get(reservationId);
        if (r == null) return false;
        if (r.getTableId() == null) return false;
        try {
            boolean ok = tableService.occupyTable(r.getTableId());
            if (ok) {
                r.setStatus(ReservationStatus.SEATED);
                return true;
            }
            return false;
        } catch (TableNotFoundException tnfe) {
            return false;
        }
    }

    // Find a reservation that has the given tableId assigned
    public synchronized java.util.Optional<Reservation> findByTableId(int tableId) {
        return reservations.values().stream().filter(r -> r.getTableId() != null && r.getTableId() == tableId).findFirst();
    }

    // Assign a specific table to a reservation (if table is free). Returns true if assigned.
    public synchronized boolean assignTableToReservation(int reservationId, int tableId) throws ReservationNotFoundException {
        Reservation r = reservations.get(reservationId);
        if (r == null) throw new ReservationNotFoundException("Reservation ID " + reservationId + " not found");
        Mesa m = tableService.getMesas().get(tableId);
        if (m == null) return false;
        if (m.getEstado() != TableStatus.LIBRE) return false;
        try {
            boolean ok = tableService.assignTable(tableId);
            if (!ok) return false;
            r.setTableId(tableId);
            r.setStatus(ReservationStatus.CONFIRMED);
            return true;
        } catch (TableNotFoundException tnfe) {
            return false;
        }
    }
}
