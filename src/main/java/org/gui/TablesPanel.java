package org.gui;

import org.models.Mesa;
import org.models.Reservation;
import org.models.ReservationService;
import org.models.TableService;
import org.exceptions.TableNotFoundException;
import org.exceptions.ReservationNotFoundException;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class TablesPanel extends JPanel {
    private final TableService tableService;
    private final ReservationService reservationService;
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JButton btnRefresh;
    private final JButton btnAssign;
    private final JButton btnRelease;

    // Floor map panel and buttons per table
    private final JPanel floorPanel;
    private final Map<Integer, JButton> tableButtons = new HashMap<>();

    public TablesPanel(TableService tableService, ReservationService reservationService) {
        super(new BorderLayout());
        this.tableService = tableService;
        this.reservationService = reservationService;

        String[] cols = {"Table ID", "Capacity", "Status", "Reservation ID", "Customer"};
        tableModel = new DefaultTableModel(cols, 0) { @Override public boolean isCellEditable(int r, int c) { return false; } };
        table = new JTable(tableModel);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnRefresh = new JButton("Refresh");
        btnAssign = new JButton("Assign Reservation");
        btnRelease = new JButton("Release Table");
        top.add(btnRefresh); top.add(btnAssign); top.add(btnRelease);

        // Floor panel on the right shows a graphical view of tables
        floorPanel = new JPanel();
        floorPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setLeftComponent(new JScrollPane(table));
        split.setRightComponent(new JScrollPane(floorPanel));
        split.setResizeWeight(0.6);

        add(top, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);

        btnRefresh.addActionListener(e -> refreshTable());
        btnAssign.addActionListener(e -> onAssign());
        btnRelease.addActionListener(e -> onRelease());

        refreshTable();
    }

    private Integer getSelectedTableId() {
        int row = table.getSelectedRow();
        if (row < 0) return null;
        Object idObj = tableModel.getValueAt(row, 0);
        try { return Integer.parseInt(String.valueOf(idObj)); } catch (Exception ex) { return null; }
    }

    private void onAssign() {
        Integer tableId = getSelectedTableId();
        if (tableId == null) { JOptionPane.showMessageDialog(this, "Select a table first.", "Error", JOptionPane.ERROR_MESSAGE); return; }
        // Show a selector of reservations that are not cancelled and not already assigned
        java.util.List<Reservation> candidates = reservationService.getAll().stream()
                .filter(r -> r.getStatus() != org.models.ReservationStatus.CANCELLED && r.getTableId() == null)
                .toList();
        if (candidates.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No available reservations to assign.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JComboBox<String> combo = new JComboBox<>();
        Map<String, Integer> keyToId = new HashMap<>();
        for (Reservation r : candidates) {
            String key = r.getId() + " - " + r.getCustomerName() + " (" + r.getGuests() + ")";
            combo.addItem(key);
            keyToId.put(key, r.getId());
        }
        int res = JOptionPane.showConfirmDialog(this, combo, "Select reservation to assign", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return;
        String chosen = (String) combo.getSelectedItem();
        if (chosen == null) return;
        int resId = keyToId.get(chosen);
        try {
            boolean ok = reservationService.assignTableToReservation(resId, tableId);
            if (ok) {
                JOptionPane.showMessageDialog(this, "Table " + tableId + " assigned to reservation " + resId);
                refreshTable();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to assign table. Ensure table is free and reservation exists.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (ReservationNotFoundException rnfe) {
            JOptionPane.showMessageDialog(this, "Reservation not found: " + rnfe.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onRelease() {
        Integer tableId = getSelectedTableId();
        if (tableId == null) { JOptionPane.showMessageDialog(this, "Select a table first.", "Error", JOptionPane.ERROR_MESSAGE); return; }
        try {
            boolean ok = tableService.releaseTable(tableId);
            if (ok) {
                // also clear reservation assignment if any
                var opt = reservationService.findByTableId(tableId);
                opt.ifPresent(r -> r.setTableId(null));
                JOptionPane.showMessageDialog(this, "Table " + tableId + " released.");
                refreshTable();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to release table.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (TableNotFoundException tnfe) {
            JOptionPane.showMessageDialog(this, "Table not found: " + tnfe.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Dialog to create a new reservation and assign to a table
    private void createAndAssignReservation(int tableId, int capacidad) {
        JPanel panel = new JPanel(new GridLayout(0, 2));
        JTextField customerField = new JTextField();
        JSpinner guestsSpinner = new JSpinner(new SpinnerNumberModel(1, 1, capacidad, 1));
        JSpinner dateSpinner = new JSpinner(new SpinnerDateModel());
        panel.add(new JLabel("Customer Name:")); panel.add(customerField);
        panel.add(new JLabel("Guests:")); panel.add(guestsSpinner);
        panel.add(new JLabel("Date & Time:")); panel.add(dateSpinner);
        int res = JOptionPane.showConfirmDialog(this, panel, "New Reservation", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return;
        String customer = customerField.getText().trim();
        int guests = (Integer) guestsSpinner.getValue();
        java.util.Date date = (java.util.Date) dateSpinner.getValue();
        java.time.LocalDateTime when = java.time.LocalDateTime.ofInstant(date.toInstant(), java.time.ZoneId.systemDefault());
        if (customer.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Customer name required.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        // Create reservation
        Reservation r = reservationService.createReservation(customer, guests, when);
        r.setTableId(tableId);
        r.setStatus(org.models.ReservationStatus.CONFIRMED);
        try {
            tableService.assignTable(tableId);
        } catch (TableNotFoundException tnfe) {
            JOptionPane.showMessageDialog(this, "Table not found when assigning: " + tnfe.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        JOptionPane.showMessageDialog(this, "Table " + tableId + " reserved for " + customer);
        refreshTable();
    }

    public void refreshTable() {
        tableModel.setRowCount(0);
        tableButtons.clear();
        floorPanel.removeAll();
        for (Map.Entry<Integer, Mesa> e : tableService.getMesas().entrySet()) {
            Mesa m = e.getValue();
            String reservationId = "-";
            String customer = "-";
            var opt = reservationService.findByTableId(m.getId());
            if (opt.isPresent()) {
                Reservation r = opt.get();
                reservationId = String.valueOf(r.getId());
                customer = r.getCustomerName();
            }
            Object[] row = new Object[] { m.getId(), m.getCapacidad(), m.getEstado(), reservationId, customer };
            tableModel.addRow(row);

            // Create a visual button for the floor map
            JButton tb = new JButton("Table " + m.getId() + "\n(" + m.getCapacidad() + ")");
            tb.setPreferredSize(new Dimension(100, 60));
            tb.setMargin(new Insets(4,4,4,4));
            tb.setVerticalTextPosition(SwingConstants.CENTER);
            tb.setHorizontalTextPosition(SwingConstants.CENTER);
            // color by status
            switch (m.getEstado()) {
                case LIBRE -> tb.setBackground(new Color(200,255,200));
                case RESERVADA -> tb.setBackground(new Color(255,230,150));
                case OCUPADA -> tb.setBackground(new Color(255,150,150));
                default -> tb.setBackground(UIManager.getColor("Button.background"));
            }
            // select table in table when clicked
            tb.addActionListener(ae -> {
                // select row in table
                for (int rowIdx = 0; rowIdx < tableModel.getRowCount(); rowIdx++) {
                    if (String.valueOf(tableModel.getValueAt(rowIdx,0)).equals(String.valueOf(m.getId()))) {
                        table.setRowSelectionInterval(rowIdx, rowIdx);
                        table.scrollRectToVisible(table.getCellRect(rowIdx, 0, true));
                        break;
                    }
                }
                // Interactive reservation/free logic
                switch (m.getEstado()) {
                    case LIBRE -> createAndAssignReservation(m.getId(), m.getCapacidad());
                    case RESERVADA, OCUPADA -> onRelease();
                    default -> JOptionPane.showMessageDialog(this, "Unknown table status.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            });
            floorPanel.add(tb);
            tableButtons.put(m.getId(), tb);
        }
        floorPanel.revalidate();
        floorPanel.repaint();
    }
}
