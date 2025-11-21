package org.gui;

import org.models.*;
import org.models.Menu;
import org.models.StockGeneral;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class MenuPanel extends JPanel {
    private final Menu menu;
    private final StockGeneral stock;
    private final TableService tableService;
    private final JList<Recipe> lstRecipes;
    private final DefaultListModel<Recipe> listModel;
    private final JTextArea txtDetails;
    private final JSpinner spQuantity;
    private final JButton btnProduce;
    private final JButton btnAddToTable;

    public MenuPanel(Menu menu, StockGeneral stock, TableService tableService) {
        super(new BorderLayout());
        this.menu = menu;
        this.stock = stock;
        this.tableService = tableService;

        listModel = new DefaultListModel<>();
        for (Recipe r : menu.getAll().values()) listModel.addElement(r);
        lstRecipes = new JList<>(listModel);
        lstRecipes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        txtDetails = new JTextArea();
        txtDetails.setEditable(false);

        spQuantity = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
        btnProduce = new JButton("Produce");
        btnAddToTable = new JButton("Add to Table");

        JPanel right = new JPanel(new BorderLayout());
        right.add(new JScrollPane(txtDetails), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.add(new JLabel("Quantity:"));
        bottom.add(spQuantity);
        bottom.add(btnProduce);
        bottom.add(btnAddToTable);
        right.add(bottom, BorderLayout.SOUTH);

        add(new JScrollPane(lstRecipes), BorderLayout.WEST);
        add(right, BorderLayout.CENTER);

        lstRecipes.addListSelectionListener(e -> updateDetails());

        btnProduce.addActionListener(e -> produceSelected());
        btnAddToTable.addActionListener(e -> addSelectedToTable());

        if (!listModel.isEmpty()) lstRecipes.setSelectedIndex(0);
    }

    private void addSelectedToTable() {
        Recipe r = lstRecipes.getSelectedValue();
        if (r == null) return;
        // Choose table
        var mesas = tableService.getMesas();
        if (mesas.isEmpty()) { JOptionPane.showMessageDialog(this, "No tables available.", "Error", JOptionPane.ERROR_MESSAGE); return; }
        JComboBox<String> combo = new JComboBox<>();
        java.util.Map<String, Integer> keyToId = new java.util.HashMap<>();
        for (var e : mesas.entrySet()) {
            String key = "Table " + e.getKey() + " (" + e.getValue().getEstado() + ")";
            combo.addItem(key);
            keyToId.put(key, e.getKey());
        }
        JPanel p = new JPanel(new BorderLayout());
        JPanel top = new JPanel(new GridLayout(0,2));
        top.add(new JLabel("Table:")); top.add(combo);
        top.add(new JLabel("Quantity:")); top.add(new JSpinner(new SpinnerNumberModel(1,1,100,1)));
        p.add(top, BorderLayout.CENTER);
        int res = JOptionPane.showConfirmDialog(this, p, "Add to Table", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return;
        String chosen = (String) combo.getSelectedItem();
        if (chosen == null) return;
        int tableId = keyToId.get(chosen);
        // get quantity component
        JSpinner qtySpinner = (JSpinner) ((JPanel)p.getComponent(0)).getComponent(3);
        int qty = (Integer) qtySpinner.getValue();
        try {
            // include recipe id so production jobs can be created automatically
            tableService.addOrder(tableId, new OrderItem(r.getNombre(), qty, r.getId()));
            JOptionPane.showMessageDialog(this, "Added " + qty + " x " + r.getNombre() + " to table " + tableId);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to add order: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateDetails() {
        Recipe r = lstRecipes.getSelectedValue();
        if (r == null) {
            txtDetails.setText("");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(r.getNombre()).append(" (prep: ").append(r.getTiempoPreparacionMinutos()).append(" min)\n\n");
        sb.append("Ingredients:\n");
        for (RecipeIngredient ri : r.getIngredientes()) {
            sb.append("- ").append(ri.getIngrediente().getNombre()).append(": ").append(ri.getCantidad()).append(" ").append(ri.getIngrediente().getUnidadMedida()).append("\n");
        }
        boolean can = r.canProduce(stock, (Integer) spQuantity.getValue());
        sb.append("\nCan produce " + spQuantity.getValue() + ": " + (can ? "YES" : "NO"));
        txtDetails.setText(sb.toString());
    }

    private void produceSelected() {
        Recipe r = lstRecipes.getSelectedValue();
        if (r == null) return;
        int qty = (Integer) spQuantity.getValue();
        if (!r.canProduce(stock, qty)) {
            JOptionPane.showMessageDialog(this, "Insufficient stock to produce.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        boolean ok = r.produce(stock, qty);
        if (ok) {
            JOptionPane.showMessageDialog(this, "Produced " + qty + " x " + r.getNombre());
        } else {
            JOptionPane.showMessageDialog(this, "Failed to produce (concurrency or stock changed).", "Error", JOptionPane.ERROR_MESSAGE);
        }
        updateDetails();
    }
}
