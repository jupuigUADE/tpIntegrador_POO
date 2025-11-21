package org.gui;

import org.models.Mesa;
import org.models.Reservation;
import org.models.ReservationService;
import org.models.ReservationStatus;
import org.models.TableService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Date;

public class ReservationsPanel extends JPanel {
    private final ReservationService reservationService;
    private final TableService tableService;

    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JTextField txtCustomer;
    private final JSpinner spGuests;
    private final JSpinner spDate;
    private final JButton btnCreate;
    private final JButton btnAssign;
    private final JButton btnSeat;
    private final JButton btnCancel;

    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public ReservationsPanel(ReservationService reservationService, TableService tableService) {
        super(new BorderLayout());
        this.reservationService = reservationService;
        this.tableService = tableService;

        String[] cols = {"ID", "Customer", "Guests", "When", "Table", "Status"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        table = new JTable(tableModel);

        JPanel top = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4,4,4,4);
        c.fill = GridBagConstraints.HORIZONTAL;

        txtCustomer = new JTextField(12);
        spGuests = new JSpinner(new SpinnerNumberModel(2, 1, 20, 1));
        spDate = new JSpinner(new SpinnerDateModel(new Date(), null, null, java.util.Calendar.MINUTE));
        JSpinner.DateEditor de = new JSpinner.DateEditor(spDate, "yyyy-MM-dd HH:mm");
        spDate.setEditor(de);

        btnCreate = new JButton("Create Reservation");
        btnAssign = new JButton("Assign Table");
        btnSeat = new JButton("Seat");
        btnCancel = new JButton("Cancel");

        c.gridx=0; c.gridy=0; top.add(new JLabel("Customer:"), c);
        c.gridx=1; top.add(txtCustomer, c);
        c.gridx=2; top.add(new JLabel("Guests:"), c);
        c.gridx=3; top.add(spGuests, c);
        c.gridx=0; c.gridy=1; top.add(new JLabel("When:"), c);
        c.gridx=1; c.gridwidth=2; top.add(spDate, c);
        c.gridwidth=1; c.gridx=3; top.add(btnCreate, c);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.add(btnAssign); actions.add(btnSeat); actions.add(btnCancel);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(actions, BorderLayout.SOUTH);

        btnCreate.addActionListener(e -> onCreate());
        btnAssign.addActionListener(e -> onAssign());
        btnSeat.addActionListener(e -> onSeat());
        btnCancel.addActionListener(e -> onCancel());

        refreshTable();
    }

    private LocalDateTime getSelectedDateTime() {
        Date d = (Date) spDate.getValue();
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(d.getTime()), ZoneId.systemDefault());
    }

    private void onCreate() {
        String customer = txtCustomer.getText().trim();
        int guests = (Integer) spGuests.getValue();
        if (customer.isEmpty()) { JOptionPane.showMessageDialog(this, "Enter customer name.", "Error", JOptionPane.ERROR_MESSAGE); return; }
        LocalDateTime when = getSelectedDateTime();
        Reservation r = reservationService.createReservation(customer, guests, when);
        refreshTable();
        JOptionPane.showMessageDialog(this, "Reservation created: " + r.getId());
    }

    private Integer getSelectedReservationId() {
        int row = table.getSelectedRow();
        if (row < 0) return null;
        Object idObj = tableModel.getValueAt(row, 0);
        try { return Integer.parseInt(String.valueOf(idObj)); } catch (Exception ex) { return null; }
    }

    private void onAssign() {
        Integer id = getSelectedReservationId();
        if (id == null) { JOptionPane.showMessageDialog(this, "Select reservation.", "Error", JOptionPane.ERROR_MESSAGE); return; }
        Integer mesa = reservationService.assignTableForReservation(id);
        if (mesa != null) {
            refreshTable();
            JOptionPane.showMessageDialog(this, "Assigned table " + mesa);
        } else {
            JOptionPane.showMessageDialog(this, "No available table found.", "Info", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void onSeat() {
        Integer id = getSelectedReservationId();
        if (id == null) { JOptionPane.showMessageDialog(this, "Select reservation.", "Error", JOptionPane.ERROR_MESSAGE); return; }
        boolean ok = reservationService.seatReservation(id);
        if (ok) { refreshTable(); JOptionPane.showMessageDialog(this, "Reservation seated."); }
        else JOptionPane.showMessageDialog(this, "Cannot seat reservation (maybe no assigned table).", "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void onCancel() {
        Integer id = getSelectedReservationId();
        if (id == null) { JOptionPane.showMessageDialog(this, "Select reservation.", "Error", JOptionPane.ERROR_MESSAGE); return; }
        boolean ok = reservationService.cancelReservation(id);
        if (ok) { refreshTable(); JOptionPane.showMessageDialog(this, "Reservation cancelled."); }
        else JOptionPane.showMessageDialog(this, "Failed to cancel.", "Error", JOptionPane.ERROR_MESSAGE);
    }

    public void refreshTable() {
        tableModel.setRowCount(0);
        Collection<Reservation> all = reservationService.getAll();
        for (Reservation r : all) {
            Object[] row = new Object[] {
                r.getId(), r.getCustomerName(), r.getGuests(), r.getWhen().format(dtf), r.getTableId() == null ? "-" : r.getTableId(), r.getStatus()
            };
            tableModel.addRow(row);
        }
    }
}

