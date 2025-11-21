package org.gui;

import org.models.Mesa;
import org.models.Menu;
import org.models.Reservation;
import org.models.ReservationService;
import org.models.TableService;
import org.models.OrderItem;
import org.models.Recipe;
import org.models.Factura;
import org.exceptions.TableNotFoundException;
import org.exceptions.ReservationNotFoundException;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import javax.swing.SwingUtilities;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;

public class TablesPanel extends JPanel {
    private final TableService tableService;
    private final ReservationService reservationService;
    private final Menu menu;
     private final DefaultTableModel tableModel;
     private final JTable table;
     private final JButton btnRefresh;
     private final JButton btnAssign;
     private final JButton btnSeat;
     private final JButton btnRelease;

    // Floor map panel and buttons per table
    private final JPanel floorPanel;
    private final Map<Integer, JButton> tableButtons = new HashMap<>();

    // Orders UI
    private final DefaultListModel<OrderItem> ordersModel;
    private final JList<OrderItem> ordersList;
    private final JButton btnAddOrder;
    private final JButton btnClearOrders;
    private final JButton btnEditOrders;
    private final JButton btnGetBill;
    private final JButton btnBillAndClose;
    private final JButton btnViewOrders;

    public TablesPanel(TableService tableService, ReservationService reservationService, Menu menu) {
        super(new BorderLayout());
        this.tableService = tableService;
        this.reservationService = reservationService;
        this.menu = menu;

        String[] cols = {"Table ID", "Capacity", "Status", "Reservation ID", "Customer"};
        tableModel = new DefaultTableModel(cols, 0) { @Override public boolean isCellEditable(int r, int c) { return false; } };
        table = new JTable(tableModel);
        // Make table columns resize to fit the viewport width so no horizontal scroll appears
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);

        // Orders
        ordersModel = new DefaultListModel<>();
        ordersList = new JList<>(ordersModel);
        ordersList.setVisibleRowCount(8);
        btnAddOrder = new JButton("Add Order");
        btnClearOrders = new JButton("Clear Orders");
        btnEditOrders = new JButton("Edit Orders");
        btnGetBill = new JButton("Get Bill");
        btnBillAndClose = new JButton("Bill & Close");
        btnViewOrders = new JButton("View Orders");
        // only enabled when a table that is actually occupied is selected
        btnViewOrders.setEnabled(false);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnRefresh = new JButton("Refresh");
        btnAssign = new JButton("Assign Reservation");
        btnSeat = new JButton("Seat Reservation");
        btnRelease = new JButton("Release Table");
        // no icons for toolbar buttons (keep simple)
         top.add(btnRefresh); top.add(btnAssign); top.add(btnSeat); top.add(btnRelease);

        // Floor panel on the right shows a graphical view of tables
        floorPanel = new JPanel();
        // layout set dynamically in refreshTable() to create a grid that fits all table buttons
        floorPanel.setLayout(new GridLayout(0, 1, 10, 10));

        // Combine floor map and orders into right side
        JPanel rightCombined = new JPanel(new BorderLayout());
        rightCombined.add(new JScrollPane(floorPanel), BorderLayout.CENTER);

        JPanel ordersPanel = new JPanel(new BorderLayout());
        ordersPanel.setBorder(BorderFactory.createTitledBorder("Table Orders"));
        ordersPanel.add(new JScrollPane(ordersList), BorderLayout.CENTER);
        JPanel ordersButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        // Simplified UI: expose a single "View Orders" entry and a quick "Bill & Close" action
        ordersButtons.add(btnViewOrders);
        ordersButtons.add(btnBillAndClose);
        ordersPanel.add(ordersButtons, BorderLayout.SOUTH);

        // Create scroll panes and avoid horizontal scroll on the main table by using auto-resize
        JScrollPane leftScroll = new JScrollPane(table);
        leftScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        JScrollPane rightScroll = new JScrollPane(rightCombined);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setLeftComponent(leftScroll);
        split.setRightComponent(rightScroll);
        split.setResizeWeight(0.6);

        add(top, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);

        // Use small wrapper methods with ActionEvent to avoid IDE/static-analysis warnings about unused lambda parameters
        btnRefresh.addActionListener(this::onRefreshAction);
        btnAssign.addActionListener(this::onAssignAction);
        btnSeat.addActionListener(this::onSeatAction);
        btnRelease.addActionListener(this::onReleaseAction);
        btnAddOrder.addActionListener(this::onAddOrderAction);
        btnEditOrders.addActionListener(this::onEditOrdersAction);
        btnClearOrders.addActionListener(this::onClearOrdersAction);
        btnGetBill.addActionListener(this::onGetBillAction);
        btnBillAndClose.addActionListener(this::onBillAndCloseAction);
        btnViewOrders.addActionListener(this::onViewOrdersAction);

        // Update orders when table selection changes and update View Orders button enabled state
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                refreshOrdersForSelectedTable();
                updateViewOrdersEnabled();
            }
        });

        // Listen to order changes so UI updates when orders are added from other panels
        tableService.addOrderListener(() -> SwingUtilities.invokeLater(() -> {
            refreshTable();
            refreshOrdersForSelectedTable();
        }));

        // Right-click on table rows to view order details
        table.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { maybeShowPopup(e); }
            @Override public void mouseReleased(MouseEvent e) { maybeShowPopup(e); }
            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0) table.setRowSelectionInterval(row, row);
                    Integer tableId = getSelectedTableId();
                    if (tableId != null) {
                        JPopupMenu pm = new JPopupMenu();
                        // only offer View Orders if the table is occupied
                        Mesa mPopup = tableService.getMesas().get(tableId);
                        if (mPopup != null && mPopup.getEstado() == org.models.TableStatus.OCUPADA) {
                            JMenuItem mi = new JMenuItem(new AbstractAction("View Orders") {
                                @Override public void actionPerformed(ActionEvent e) { viewOrdersDialog(tableId); }
                            });
                            pm.add(mi);
                        }
                        pm.show(table, e.getX(), e.getY());
                     }
                 }
             }
         });

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

    // New: seat a reservation for the selected table (mark it occupied)
    private void onSeat() {
        Integer tableId = getSelectedTableId();
        if (tableId == null) { JOptionPane.showMessageDialog(this, "Select a table first.", "Error", JOptionPane.ERROR_MESSAGE); return; }
        Mesa m = tableService.getMesas().get(tableId);
        if (m == null) { JOptionPane.showMessageDialog(this, "Table not found.", "Error", JOptionPane.ERROR_MESSAGE); return; }

        var optRes = reservationService.findByTableId(tableId);
        if (optRes.isPresent()) {
            Reservation r = optRes.get();
            int res = JOptionPane.showConfirmDialog(this,
                    "Seat reservation " + r.getId() + " for " + r.getCustomerName() + " now?",
                    "Seat Reservation",
                    JOptionPane.YES_NO_OPTION);
            if (res == JOptionPane.YES_OPTION) {
                boolean ok = reservationService.seatReservation(r.getId());
                if (ok) {
                    JOptionPane.showMessageDialog(this, "Reservation seated; table " + m.getId() + " is now occupied.");
                    // Prompt for orders immediately after seating from floor map
                   promptAndAddOrders(m.getId());
                   refreshTable();
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to seat reservation.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } else {
            // No reservation assigned to this table — allow seating as a walk-in (mark occupied)
            if (m.getEstado() == org.models.TableStatus.OCUPADA) {
                JOptionPane.showMessageDialog(this, "Table is already occupied.", "Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            int res = JOptionPane.showConfirmDialog(this,
                    "No reservation assigned. Mark table " + m.getId() + " as occupied for a walk-in?",
                    "Seat Walk-in",
                    JOptionPane.YES_NO_OPTION);
            if (res == JOptionPane.YES_OPTION) {
                try {
                    boolean ok = tableService.occupyTable(m.getId());
                    if (ok) {
                        // Prompt for orders for the walk-in immediately
                        promptAndAddOrders(m.getId());
                        refreshTable();
                    } else {
                        JOptionPane.showMessageDialog(this, "Failed to mark table as occupied.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (org.exceptions.TableNotFoundException tnfe) {
                    JOptionPane.showMessageDialog(this, "Table not found: " + tnfe.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
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
                // cancel any orders for this table (mark cancelled and cancel linked production jobs)
                try {
                    tableService.cancelOrdersForTable(tableId);
                } catch (Exception ignored) {}
                JOptionPane.showMessageDialog(this, "Table " + tableId + " released and orders cancelled.");
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

    private void onAddOrder() {
        Integer tableId = getSelectedTableId();
        if (tableId == null) {
            // ask user to pick a table if none selected
            java.util.Map<Integer, Mesa> mesas = tableService.getMesas();
            if (mesas.isEmpty()) { JOptionPane.showMessageDialog(this, "No tables available.", "Info", JOptionPane.INFORMATION_MESSAGE); return; }
            JComboBox<String> combo = new JComboBox<>();
            java.util.Map<String, Integer> keyToId = new java.util.HashMap<>();
            for (Mesa mm : mesas.values()) {
                String key = mm.getId() + " - " + mm.getEstado();
                combo.addItem(key);
                keyToId.put(key, mm.getId());
            }
            int res = JOptionPane.showConfirmDialog(this, combo, "Select table to add order", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (res != JOptionPane.OK_OPTION) return;
            String chosen = (String) combo.getSelectedItem();
            if (chosen == null) return;
            tableId = keyToId.get(chosen);
        }
        promptAndAddOrders(tableId);
    }

    private void onClearOrders() {
        Integer tableId = getSelectedTableId();
        if (tableId == null) { JOptionPane.showMessageDialog(this, "Select a table first.", "Error", JOptionPane.ERROR_MESSAGE); return; }
        int res = JOptionPane.showConfirmDialog(this, "Clear all orders for table " + tableId + "?", "Confirm", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return;
        try {
            tableService.clearOrders(tableId);
            refreshOrdersForSelectedTable();
        } catch (TableNotFoundException tnfe) {
            JOptionPane.showMessageDialog(this, "Table not found: " + tnfe.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onGetBill() {
        Integer tableId = getSelectedTableId();
        if (tableId == null) { JOptionPane.showMessageDialog(this, "Select a table first.", "Error", JOptionPane.ERROR_MESSAGE); return; }
        try {
            java.util.List<OrderItem> orders = tableService.getOrders(tableId);

            // Determine customer name: prefer reservation's customer; if none and table is occupied, ask for it
            String customerName = null;
            var optRes = reservationService.findByTableId(tableId);
            if (optRes.isPresent()) {
                customerName = optRes.get().getCustomerName();
            } else {
                // no reservation assigned — if table is actually occupied, ask for customer name before billing
                Mesa m = tableService.getMesas().get(tableId);
                if (m != null && m.getEstado() == org.models.TableStatus.OCUPADA) {
                    String input = (String) JOptionPane.showInputDialog(this,
                            "Enter customer name for table " + tableId + ":",
                            "Customer required",
                            JOptionPane.PLAIN_MESSAGE,
                            null,
                            null,
                            "");
                    if (input == null) return; // user cancelled
                    input = input.trim();
                    if (input.isEmpty()) {
                        JOptionPane.showMessageDialog(this, "Customer name required to generate the bill.", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    customerName = input;
                }
            }

            Factura factura = new Factura(tableId, orders, menu);
            StringBuilder fullText = new StringBuilder();
            if (customerName != null && !customerName.isEmpty()) {
                fullText.append("Customer: ").append(customerName).append('\n');
            }
            // StringBuilder.append(Object) will call toString() internally
            fullText.append(factura);

            JTextArea ta = new JTextArea(fullText.toString());
            ta.setEditable(false);
            ta.setRows(Math.min(20, factura.getItems().size() + 6));
            ta.setColumns(50);
            int res = JOptionPane.showOptionDialog(this, new JScrollPane(ta), "Factura - Table " + tableId,
                    JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, null, new String[] {"Close","Settle & Clear"}, "Close");
            if (res == JOptionPane.NO_OPTION) {
                // Settle: clear orders and release table
                try {
                    // Clear orders
                    tableService.clearOrders(tableId);
                    // release the mesa and remove reservation assignment if any
                    try {
                        boolean ok = tableService.releaseTable(tableId);
                        if (ok) {
                            var opt = reservationService.findByTableId(tableId);
                            opt.ifPresent(r -> r.setTableId(null));
                        }
                    } catch (TableNotFoundException ignored) {}
                } catch (Exception ignored) {}
                refreshTable();
            }
        } catch (TableNotFoundException tnfe) {
            JOptionPane.showMessageDialog(this, "Table not found: " + tnfe.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Produce the bill (show factura) and immediately settle: clear orders, release table and remove reservation assignment
    private void onBillAndClose() {
        Integer tableId = getSelectedTableId();
        if (tableId == null) { JOptionPane.showMessageDialog(this, "Select a table first.", "Error", JOptionPane.ERROR_MESSAGE); return; }
        try {
            java.util.List<OrderItem> orders = tableService.getOrders(tableId);
            if (orders.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No orders for table " + tableId, "Orders", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // Determine customer name: prefer reservation's customer; if none and table is occupied, ask for it
            String customerName = null;
            var optRes = reservationService.findByTableId(tableId);
            if (optRes.isPresent()) {
                customerName = optRes.get().getCustomerName();
            } else {
                Mesa m = tableService.getMesas().get(tableId);
                if (m != null && m.getEstado() == org.models.TableStatus.OCUPADA) {
                    String input = (String) JOptionPane.showInputDialog(this,
                            "Enter customer name for table " + tableId + ":",
                            "Customer required",
                            JOptionPane.PLAIN_MESSAGE,
                            null,
                            null,
                            "");
                    if (input == null) return; // user cancelled
                    input = input.trim();
                    if (input.isEmpty()) {
                        JOptionPane.showMessageDialog(this, "Customer name required to generate the bill.", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    customerName = input;
                }
            }

            Factura factura = new Factura(tableId, orders, menu);
            StringBuilder fullText = new StringBuilder();
            if (customerName != null && !customerName.isEmpty()) fullText.append("Customer: ").append(customerName).append('\n');
            fullText.append(factura);

            JTextArea ta = new JTextArea(fullText.toString());
            ta.setEditable(false);
            ta.setRows(Math.min(20, factura.getItems().size() + 6));
            ta.setColumns(50);
            JOptionPane.showMessageDialog(this, new JScrollPane(ta), "Factura - Table " + tableId, JOptionPane.PLAIN_MESSAGE);

            // Settle: clear orders and release table and remove reservation assignment if any
            try {
                tableService.clearOrders(tableId);
            } catch (TableNotFoundException ignored) {}
            try {
                boolean ok = tableService.releaseTable(tableId);
                if (ok) {
                    var opt = reservationService.findByTableId(tableId);
                    opt.ifPresent(r -> r.setTableId(null));
                }
            } catch (TableNotFoundException ignored) {}

            refreshTable();
            JOptionPane.showMessageDialog(this, "Table " + tableId + " settled and closed.");

        } catch (TableNotFoundException tnfe) {
            JOptionPane.showMessageDialog(this, "Table not found: " + tnfe.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshOrdersForSelectedTable() {
        ordersModel.clear();
        Integer tableId = getSelectedTableId();
        if (tableId == null) return;
        try {
            java.util.List<OrderItem> list = new java.util.ArrayList<>(tableService.getOrders(tableId));
            list.sort((a,b) -> {
                Long ta = a.getEstimatedMinutes();
                Long tb = b.getEstimatedMinutes();
                if (ta == null && tb == null) return 0;
                if (ta == null) return 1;
                if (tb == null) return -1;
                return ta.compareTo(tb);
            });
            for (OrderItem oi : list) ordersModel.addElement(oi);
        } catch (TableNotFoundException tnfe) {
            // ignore
        }
    }

    private void showOrderDetails(int tableId) {
        try {
            java.util.List<OrderItem> orders = tableService.getOrders(tableId);
            if (orders.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No orders for table " + tableId, "Orders", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Orders for table ").append(tableId).append("\n\n");
            for (OrderItem oi : orders) sb.append(oi.toString()).append("\n");
            JTextArea ta = new JTextArea(sb.toString());
            ta.setEditable(false);
            ta.setRows(Math.min(orders.size()+2, 15));
            ta.setColumns(40);
            JOptionPane.showMessageDialog(this, new JScrollPane(ta), "Order Details", JOptionPane.PLAIN_MESSAGE);
        } catch (TableNotFoundException tnfe) {
            JOptionPane.showMessageDialog(this, "Table not found: " + tnfe.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
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
            JButton tb = new JButton("Table " + m.getId());
            // let GridLayout size buttons evenly; keep a comfortable margin and tooltip for capacity
            tb.setMargin(new Insets(6,6,6,6));
            tb.setToolTipText("Capacity: " + m.getCapacidad());
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
                 // refresh orders view for this table
                 refreshOrdersForSelectedTable();
                 // show unified actions dialog for this table (view/edit/add orders + status actions)
                 showTableActions(m);
             });
            floorPanel.add(tb);
            tableButtons.put(m.getId(), tb);
        }
        // adapt floorPanel to a square-ish grid based on number of tables
        int n = tableButtons.size();
        if (n > 0) {
            int cols = (int) Math.ceil(Math.sqrt(n));
            cols = Math.max(1, Math.min(cols, 6)); // limit columns for usability
            floorPanel.setLayout(new GridLayout(0, cols, 10, 10));
        } else {
            floorPanel.setLayout(new GridLayout(0, 1, 10, 10));
        }
        floorPanel.revalidate();
        floorPanel.repaint();

        // attempt to refresh orders for currently selected row
        refreshOrdersForSelectedTable();
        // update View Orders button state after refresh
        updateViewOrdersEnabled();
    }

    // --- Action wrappers to avoid unused-lambda warnings from static analysis ---
    private void onRefreshAction(ActionEvent e) { refreshTable(); }
    private void onAssignAction(ActionEvent e) { onAssign(); }
    private void onSeatAction(ActionEvent e) { onSeat(); }
    private void onReleaseAction(ActionEvent e) { onRelease(); }
    private void onAddOrderAction(ActionEvent e) {
        Integer tableId = getSelectedTableId();
        if (tableId == null) {
            // ask user to pick a table if none selected
            java.util.Map<Integer, Mesa> mesas = tableService.getMesas();
            if (mesas.isEmpty()) { JOptionPane.showMessageDialog(this, "No tables available.", "Info", JOptionPane.INFORMATION_MESSAGE); return; }
            JComboBox<String> combo = new JComboBox<>();
            java.util.Map<String, Integer> keyToId = new java.util.HashMap<>();
            for (Mesa mm : mesas.values()) {
                String key = mm.getId() + " - " + mm.getEstado();
                combo.addItem(key);
                keyToId.put(key, mm.getId());
            }
            int res = JOptionPane.showConfirmDialog(this, combo, "Select table to add order", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (res != JOptionPane.OK_OPTION) return;
            String chosen = (String) combo.getSelectedItem();
            if (chosen == null) return;
            tableId = keyToId.get(chosen);
        }
        promptAndAddOrders(tableId);
    }
    private void onClearOrdersAction(ActionEvent e) { onClearOrders(); }
    private void onGetBillAction(ActionEvent e) { onGetBill(); }
    private void onBillAndCloseAction(ActionEvent e) { onBillAndClose(); }
    // Wrapper for View Orders button
    private void onViewOrdersAction(ActionEvent e) { onViewOrders(); }

    private void onViewOrders() {
        Integer tableId = getSelectedTableId();
        // If no selection, ask user to pick an occupied table only
        if (tableId == null) {
            java.util.Map<Integer, Mesa> mesas = tableService.getMesas();
            java.util.List<Mesa> occupied = mesas.values().stream().filter(mm -> mm.getEstado() == org.models.TableStatus.OCUPADA).toList();
            if (occupied.isEmpty()) { JOptionPane.showMessageDialog(this, "No occupied tables available.", "Info", JOptionPane.INFORMATION_MESSAGE); return; }
            JComboBox<String> combo = new JComboBox<>(); java.util.Map<String,Integer> keyToId = new java.util.HashMap<>();
            for (Mesa mm : occupied) { String key = mm.getId() + " - " + mm.getEstado(); combo.addItem(key); keyToId.put(key, mm.getId()); }
            int res = JOptionPane.showConfirmDialog(this, combo, "Select occupied table to view orders", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (res != JOptionPane.OK_OPTION) return; String chosen = (String) combo.getSelectedItem(); if (chosen == null) return; tableId = keyToId.get(chosen);
        }
        // only open dialog if the table is occupied
        Mesa m = tableService.getMesas().get(tableId);
        if (m != null && m.getEstado() == org.models.TableStatus.OCUPADA) viewOrdersDialog(tableId);
        else JOptionPane.showMessageDialog(this, "View Orders is available only for occupied tables.", "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    // Update the enabled state of the View Orders button based on currently selected table
    private void updateViewOrdersEnabled() {
        Integer tableId = getSelectedTableId();
        if (tableId == null) { btnViewOrders.setEnabled(false); return; }
        Mesa m = tableService.getMesas().get(tableId);
        btnViewOrders.setEnabled(m != null && m.getEstado() == org.models.TableStatus.OCUPADA);
    }

    // Unified orders dialog: view list and allow add/edit/remove/bill from one place
    private void viewOrdersDialog(int tableId) {
        try {
            JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this), "Orders - Table " + tableId, Dialog.ModalityType.APPLICATION_MODAL);
            dlg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            dlg.setLayout(new BorderLayout(8,8));

            DefaultListModel<OrderItem> model = new DefaultListModel<>();
            java.util.List<OrderItem> list = new java.util.ArrayList<>(tableService.getOrders(tableId));
            for (OrderItem oi : list) model.addElement(oi);
            JList<OrderItem> jlist = new JList<>(model);
            jlist.setVisibleRowCount(12);
            dlg.add(new JScrollPane(jlist), BorderLayout.CENTER);

            JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton btnAdd = new JButton("Add");
            JButton btnEdit = new JButton("Edit");
            JButton btnRemove = new JButton("Remove");
            JButton btnBillClose = new JButton("Bill & Close");
            JButton btnClose = new JButton("Close");
            btns.add(btnAdd); btns.add(btnEdit); btns.add(btnRemove); btns.add(btnBillClose); btns.add(btnClose);
            dlg.add(btns, BorderLayout.SOUTH);

            btnAdd.addActionListener(ae -> { dlg.dispose(); promptAndAddOrders(tableId); refreshOrdersForSelectedTable(); viewOrdersDialog(tableId); });
            btnEdit.addActionListener(ae -> { dlg.dispose(); editOrdersDialog(tableId); refreshOrdersForSelectedTable(); viewOrdersDialog(tableId); });
            btnRemove.addActionListener(ae -> {
                int idx = jlist.getSelectedIndex();
                if (idx < 0) { JOptionPane.showMessageDialog(dlg, "Select an item to remove.", "Error", JOptionPane.ERROR_MESSAGE); return; }
                try {
                    boolean ok = tableService.removeOrderItem(tableId, idx);
                    if (ok) { model.remove(idx); refreshOrdersForSelectedTable(); }
                } catch (TableNotFoundException tnfe) { JOptionPane.showMessageDialog(dlg, "Table not found: " + tnfe.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); }
            });
            btnBillClose.addActionListener(ae -> { dlg.dispose(); onBillAndClose(); });
            btnClose.addActionListener(ae -> dlg.dispose());

            dlg.pack(); dlg.setLocationRelativeTo(this); dlg.setVisible(true);
        } catch (TableNotFoundException tnfe) {
            JOptionPane.showMessageDialog(this, "Table not found: " + tnfe.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Nicer dialog to add multiple order items to the given table
    private void promptAndAddOrders(int tableId) {
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this), "Add Orders - Table " + tableId, Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dlg.setLayout(new BorderLayout(10,10));

        // Left: menu selection (or manual entry if no menu)
        JPanel left = new JPanel(new BorderLayout(5,5));
        left.setBorder(BorderFactory.createTitledBorder("Menu Items"));
        DefaultListModel<String> menuModel = new DefaultListModel<>();
        java.util.List<org.models.Recipe> recipes = new java.util.ArrayList<>();
        if (menu != null) {
            for (org.models.Recipe r : menu.getAll().values()) { menuModel.addElement(r.getNombre()); recipes.add(r); }
        }
        JList<String> menuList = new JList<>(menuModel);
        menuList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        left.add(new JScrollPane(menuList), BorderLayout.CENTER);

        JPanel manual = new JPanel(new GridLayout(0,1,4,4));
        JTextField manualName = new JTextField();
        JSpinner manualQty = new JSpinner(new SpinnerNumberModel(1,1,100,1));
        manual.add(new JLabel("Manual name (if not using menu):")); manual.add(manualName);
        manual.add(new JLabel("Quantity:")); manual.add(manualQty);
        left.add(manual, BorderLayout.SOUTH);

        // Center: add controls
        JPanel center = new JPanel(new GridLayout(0,1,4,4));
        JButton btnAdd = new JButton("Add to preview");
        center.add(btnAdd);

        // Right: preview of items to be added
        DefaultListModel<OrderItem> previewModel = new DefaultListModel<>();
        JList<OrderItem> previewList = new JList<>(previewModel);
        previewList.setVisibleRowCount(8);
        JPanel right = new JPanel(new BorderLayout(5,5));
        right.setBorder(BorderFactory.createTitledBorder("Preview (will be added to table)"));
        right.add(new JScrollPane(previewList), BorderLayout.CENTER);
        JPanel previewButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnRemove = new JButton("Remove");
        previewButtons.add(btnRemove);
        right.add(previewButtons, BorderLayout.SOUTH);

        // Bottom: confirm/cancel
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnConfirm = new JButton("Confirm and Add");
        JButton btnCancel = new JButton("Cancel");
        bottom.add(btnCancel); bottom.add(btnConfirm);

        // Wire actions
        btnAdd.addActionListener(ae -> {
            String selected = menuList.getSelectedValue();
            int qty = (Integer) manualQty.getValue();
            if (selected != null && !selected.isEmpty()) {
                // find recipe id
                Integer rid = null;
                for (org.models.Recipe r : recipes) if (r.getNombre().equals(selected)) { rid = r.getId(); break; }
                previewModel.addElement(new OrderItem(selected, qty, rid));
            } else {
                String name = manualName.getText().trim();
                if (name.isEmpty()) { JOptionPane.showMessageDialog(dlg, "Enter a name or select a menu item.", "Error", JOptionPane.ERROR_MESSAGE); return; }
                previewModel.addElement(new OrderItem(name, qty, null));
            }
        });

        btnRemove.addActionListener(ae -> {
            int idx = previewList.getSelectedIndex();
            if (idx >= 0) previewModel.remove(idx);
        });

        btnCancel.addActionListener(ae -> dlg.dispose());

        btnConfirm.addActionListener(ae -> {
            if (previewModel.isEmpty()) { dlg.dispose(); return; }
            try {
                for (int i=0;i<previewModel.size();i++) {
                    OrderItem oi = previewModel.get(i);
                    tableService.addOrder(tableId, oi);
                }
                dlg.dispose();
                JOptionPane.showMessageDialog(this, "Added " + previewModel.size() + " order(s) to table " + tableId + ".");
                refreshOrdersForSelectedTable();
            } catch (TableNotFoundException tnfe) {
                JOptionPane.showMessageDialog(this, "Table not found: " + tnfe.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Layout main dialog
        JPanel mid = new JPanel(new BorderLayout(10,10));
        mid.add(left, BorderLayout.WEST);
        mid.add(center, BorderLayout.CENTER);
        mid.add(right, BorderLayout.EAST);
        dlg.add(mid, BorderLayout.CENTER);
        dlg.add(bottom, BorderLayout.SOUTH);
        dlg.pack();
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    // Open an edit dialog prepopulated with current orders; on confirm replace table's orders with preview
    private void editOrdersDialog(int tableId) {
        try {
            java.util.List<OrderItem> existing = new java.util.ArrayList<>(tableService.getOrders(tableId));

            JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this), "Edit Orders - Table " + tableId, Dialog.ModalityType.APPLICATION_MODAL);
            dlg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            dlg.setLayout(new BorderLayout(10,10));

            // reuse prompt UI parts: menu list and manual entry
            DefaultListModel<String> menuModel = new DefaultListModel<>();
            java.util.List<org.models.Recipe> recipes = new java.util.ArrayList<>();
            if (menu != null) for (org.models.Recipe r : menu.getAll().values()) { menuModel.addElement(r.getNombre()); recipes.add(r); }
            JList<String> menuList = new JList<>(menuModel);
            menuList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

            JPanel left = new JPanel(new BorderLayout(5,5));
            left.setBorder(BorderFactory.createTitledBorder("Menu Items"));
            left.add(new JScrollPane(menuList), BorderLayout.CENTER);
            JPanel manual = new JPanel(new GridLayout(0,1,4,4));
            JTextField manualName = new JTextField();
            JSpinner manualQty = new JSpinner(new SpinnerNumberModel(1,1,100,1));
            manual.add(new JLabel("Manual name (if not using menu):")); manual.add(manualName);
            manual.add(new JLabel("Quantity:")); manual.add(manualQty);
            left.add(manual, BorderLayout.SOUTH);

            JPanel center = new JPanel(new GridLayout(0,1,4,4));
            JButton btnAdd = new JButton("Add to preview");
            center.add(btnAdd);

            DefaultListModel<OrderItem> previewModel = new DefaultListModel<>();
            // clone existing orders into the preview so we don't reuse the same instances
            for (OrderItem oi : existing) previewModel.addElement(new OrderItem(oi.getName(), oi.getQuantity(), oi.getRecipeId()));
            JList<OrderItem> previewList = new JList<>(previewModel);
            JPanel right = new JPanel(new BorderLayout(5,5));
            right.setBorder(BorderFactory.createTitledBorder("Preview (will replace table orders)"));
            right.add(new JScrollPane(previewList), BorderLayout.CENTER);
            JPanel previewButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JButton btnRemove = new JButton("Remove"); previewButtons.add(btnRemove);
            right.add(previewButtons, BorderLayout.SOUTH);

            JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton btnConfirm = new JButton("Confirm Changes"); JButton btnCancel = new JButton("Cancel");
            bottom.add(btnCancel); bottom.add(btnConfirm);

            btnAdd.addActionListener(ae -> {
                String selected = menuList.getSelectedValue();
                int qty = (Integer) manualQty.getValue();
                if (selected != null && !selected.isEmpty()) {
                    Integer rid = null; for (org.models.Recipe r : recipes) if (r.getNombre().equals(selected)) { rid = r.getId(); break; }
                    previewModel.addElement(new OrderItem(selected, qty, rid));
                } else {
                    String name = manualName.getText().trim(); if (name.isEmpty()) { JOptionPane.showMessageDialog(dlg, "Enter a name or select a menu item.", "Error", JOptionPane.ERROR_MESSAGE); return; }
                    previewModel.addElement(new OrderItem(name, qty, null));
                }
            });
            btnRemove.addActionListener(ae -> { int idx = previewList.getSelectedIndex(); if (idx >= 0) previewModel.remove(idx); });
            btnCancel.addActionListener(ae -> dlg.dispose());

            btnConfirm.addActionListener(ae -> {
                try {
                    // mark existing orders CANCELLED and cancel their production jobs so kitchen updates
                    tableService.cancelOrdersForTable(tableId);
                    // then add the new preview items (these will create new production jobs as needed)
                    for (int i=0;i<previewModel.size();i++) {
                        OrderItem oi = previewModel.get(i);
                        // create a fresh OrderItem instance to avoid reusing any cancelled objects
                        tableService.addOrder(tableId, new OrderItem(oi.getName(), oi.getQuantity(), oi.getRecipeId()));
                    }
                    dlg.dispose();
                    JOptionPane.showMessageDialog(this, "Updated orders for table " + tableId);
                    refreshOrdersForSelectedTable();
                } catch (TableNotFoundException tnfe) {
                    JOptionPane.showMessageDialog(this, "Table not found: " + tnfe.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            });

            JPanel mid = new JPanel(new BorderLayout(10,10)); mid.add(left, BorderLayout.WEST); mid.add(center, BorderLayout.CENTER); mid.add(right, BorderLayout.EAST);
            dlg.add(mid, BorderLayout.CENTER); dlg.add(bottom, BorderLayout.SOUTH);
            dlg.pack(); dlg.setLocationRelativeTo(this); dlg.setVisible(true);

        } catch (TableNotFoundException tnfe) {
            JOptionPane.showMessageDialog(this, "Table not found: " + tnfe.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Action wrapper for Edit Orders button
    private void onEditOrdersAction(ActionEvent e) {
        Integer tableId = getSelectedTableId();
        if (tableId == null) {
            // ask user to pick a table
            java.util.Map<Integer, Mesa> mesas = tableService.getMesas();
            if (mesas.isEmpty()) { JOptionPane.showMessageDialog(this, "No tables available.", "Info", JOptionPane.INFORMATION_MESSAGE); return; }
            JComboBox<String> combo = new JComboBox<>(); java.util.Map<String,Integer> keyToId = new java.util.HashMap<>();
            for (Mesa mm : mesas.values()) { String key = mm.getId() + " - " + mm.getEstado(); combo.addItem(key); keyToId.put(key, mm.getId()); }
            int res = JOptionPane.showConfirmDialog(this, combo, "Select table to edit orders", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (res != JOptionPane.OK_OPTION) return; String chosen = (String) combo.getSelectedItem(); if (chosen == null) return; tableId = keyToId.get(chosen);
        }
        editOrdersDialog(tableId);
    }

    // Show a unified dialog with table actions: view orders, edit orders, add orders, and status specific actions
    private void showTableActions(Mesa m) {
        java.util.List<String> opts = new java.util.ArrayList<>();
        // include View Orders only for occupied tables
        if (m.getEstado() == org.models.TableStatus.OCUPADA) opts.add("View Orders");
        // status-specific
        switch (m.getEstado()) {
            case LIBRE -> {
                opts.add("Create Reservation");
                opts.add("Mark Occupied");
            }
            case RESERVADA -> {
                opts.add("Seat Reservation");
                opts.add("Mark Occupied");
            }
            case OCUPADA -> {
                opts.add("Release Table");
            }
            default -> {}
        }
        opts.add("Cancel");

        String[] options = opts.toArray(new String[0]);
        int choice = JOptionPane.showOptionDialog(this,
                "Table " + m.getId() + " - choose action:",
                "Table Actions",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                options[0]);

        if (choice < 0 || choice >= options.length) return;
        String sel = options[choice];
        switch (sel) {
            case "View Orders" -> viewOrdersDialog(m.getId());
            case "Create Reservation" -> createAndAssignReservation(m.getId(), m.getCapacidad());
            case "Mark Occupied" -> {
                try {
                    boolean ok = tableService.occupyTable(m.getId());
                    if (ok) { promptAndAddOrders(m.getId()); refreshTable(); }
                } catch (TableNotFoundException tnfe) { JOptionPane.showMessageDialog(this, "Table not found: " + tnfe.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); }
            }
            case "Seat Reservation" -> {
                var opt = reservationService.findByTableId(m.getId());
                if (opt.isPresent()) {
                    Reservation r = opt.get();
                    int res = JOptionPane.showConfirmDialog(this, "Seat reservation " + r.getId() + " for " + r.getCustomerName() + " now?", "Seat Reservation", JOptionPane.YES_NO_OPTION);
                    if (res == JOptionPane.YES_OPTION) {
                        boolean ok = reservationService.seatReservation(r.getId());
                        if (ok) { JOptionPane.showMessageDialog(this, "Reservation seated; table " + m.getId() + " is now occupied."); promptAndAddOrders(m.getId()); refreshTable(); }
                        else JOptionPane.showMessageDialog(this, "Failed to seat reservation.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "No reservation found for this table.", "Info", JOptionPane.INFORMATION_MESSAGE);
                }
            }
            case "Release Table" -> {
                int res = JOptionPane.showConfirmDialog(this, "Release table " + m.getId() + " now?", "Release Table", JOptionPane.YES_NO_OPTION);
                if (res == JOptionPane.YES_OPTION) onRelease();
            }
            default -> {}
        }
    }
}
