package org.models;

import java.time.LocalDateTime;

public class Reservation {
    private final int id;
    private final String customerName;
    private final int guests;
    private final LocalDateTime when;
    private Integer tableId; // nullable until assigned
    private ReservationStatus status;

    public Reservation(int id, String customerName, int guests, LocalDateTime when) {
        this.id = id;
        this.customerName = customerName;
        this.guests = guests;
        this.when = when;
        this.status = ReservationStatus.PENDING;
    }

    public int getId() { return id; }
    public String getCustomerName() { return customerName; }
    public int getGuests() { return guests; }
    public LocalDateTime getWhen() { return when; }
    public Integer getTableId() { return tableId; }
    public void setTableId(Integer tableId) { this.tableId = tableId; }
    public ReservationStatus getStatus() { return status; }
    public void setStatus(ReservationStatus status) { this.status = status; }
}

