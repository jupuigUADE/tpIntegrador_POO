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
    private final JList<Recipe> lstRecipes;
    private final DefaultListModel<Recipe> listModel;
    private final JTextArea txtDetails;
    private final JSpinner spQuantity;
    private final JButton btnProduce;

    public MenuPanel(Menu menu, StockGeneral stock) {
        super(new BorderLayout());
        this.menu = menu;
        this.stock = stock;

        listModel = new DefaultListModel<>();
        for (Recipe r : menu.getAll().values()) listModel.addElement(r);
        lstRecipes = new JList<>(listModel);
        lstRecipes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        txtDetails = new JTextArea();
        txtDetails.setEditable(false);

        spQuantity = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
        btnProduce = new JButton("Produce");

        JPanel right = new JPanel(new BorderLayout());
        right.add(new JScrollPane(txtDetails), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.add(new JLabel("Quantity:"));
        bottom.add(spQuantity);
        bottom.add(btnProduce);
        right.add(bottom, BorderLayout.SOUTH);

        add(new JScrollPane(lstRecipes), BorderLayout.WEST);
        add(right, BorderLayout.CENTER);

        lstRecipes.addListSelectionListener(e -> updateDetails());

        btnProduce.addActionListener(e -> produceSelected());

        if (!listModel.isEmpty()) lstRecipes.setSelectedIndex(0);
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

