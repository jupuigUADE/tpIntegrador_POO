package org.gui;

import org.models.*;
import org.models.Menu;
import org.models.Pedido    ;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ProductionPanel extends JPanel {
    private final Pedido productionService;
    private final Menu menu;
    private final StockGeneral stock;

    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JComboBox<Recipe> cmbRecipes;
    private final JSpinner spQty;
    private final JButton btnCreate;
    private final JButton btnStart;
    private final JButton btnFinish;

    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public ProductionPanel(Pedido productionService, Menu menu, StockGeneral stock) {
        super(new BorderLayout());
        this.productionService = productionService;
        this.menu = menu;
        this.stock = stock;

        // include ETA (minutes) column
        String[] cols = {"ID", "Recipe", "Qty", "Status", "ETA (min)", "Created", "Started", "Finished"};
        tableModel = new DefaultTableModel(cols, 0) { @Override public boolean isCellEditable(int r,int c){return false;} };
        table = new JTable(tableModel);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        cmbRecipes = new JComboBox<>();
        for (Recipe r : menu.getAll().values()) cmbRecipes.addItem(r);
        spQty = new JSpinner(new SpinnerNumberModel(1,1,100,1));
        btnCreate = new JButton("Create Job");
        btnStart = new JButton("Start Job");
        btnFinish = new JButton("Finish Job");

        top.add(new JLabel("Recipe:")); top.add(cmbRecipes);
        top.add(new JLabel("Qty:")); top.add(spQty);
        top.add(btnCreate); top.add(btnStart); top.add(btnFinish);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        btnCreate.addActionListener(e -> onCreate());
        btnStart.addActionListener(e -> onStart());
        btnFinish.addActionListener(e -> onFinish());

        // Refresh table automatically when production jobs change
        productionService.addJobListener(job -> javax.swing.SwingUtilities.invokeLater(this::refreshTable));

        refreshTable();
    }

    private Integer getSelectedJobId() {
        int row = table.getSelectedRow();
        if (row < 0) return null;
        Object idObj = tableModel.getValueAt(row,0);
        try { return Integer.parseInt(String.valueOf(idObj)); } catch (Exception ex) { return null; }
    }

    private void onCreate() {
        Recipe r = (Recipe) cmbRecipes.getSelectedItem();
        if (r == null) return;
        int qty = (Integer) spQty.getValue();
        productionService.createJob(r, qty);
        refreshTable();
        JOptionPane.showMessageDialog(this, "Job created for " + r.getNombre() + " x" + qty);
    }

    private void onStart() {
        Integer id = getSelectedJobId();
        if (id == null) { JOptionPane.showMessageDialog(this, "Select job.", "Error", JOptionPane.ERROR_MESSAGE); return; }
        boolean ok = productionService.startJob(id);
        if (ok) JOptionPane.showMessageDialog(this, "Job started."); else JOptionPane.showMessageDialog(this, "Failed to start job (insufficient stock or invalid state).", "Error", JOptionPane.ERROR_MESSAGE);
        refreshTable();
    }

    private void onFinish() {
        Integer id = getSelectedJobId();
        if (id == null) { JOptionPane.showMessageDialog(this, "Select job.", "Error", JOptionPane.ERROR_MESSAGE); return; }
        boolean ok = productionService.finishJob(id, true);
        if (ok) JOptionPane.showMessageDialog(this, "Job finished."); else JOptionPane.showMessageDialog(this, "Failed to finish job.", "Error", JOptionPane.ERROR_MESSAGE);
        refreshTable();
    }

    public void refreshTable() {
        tableModel.setRowCount(0);
        List<ProductionJob> jobs = productionService.listJobs();
        for (ProductionJob j : jobs) {
            // compute ETA in minutes for this job
            long etaMinutes = productionService.estimateRemainingMinutesForJob(j.getId());
            String etaText = etaMinutes < 0 ? "-" : String.valueOf(etaMinutes);
            Object[] row = new Object[]{
                j.getId(), j.getRecipe().getNombre(), j.getQuantity(), j.getStatus(), etaText,
                j.getCreatedAt()!=null?j.getCreatedAt().format(dtf):"-",
                j.getStartedAt()!=null?j.getStartedAt().format(dtf):"-",
                j.getFinishedAt()!=null?j.getFinishedAt().format(dtf):"-"
            };
            tableModel.addRow(row);
        }
    }
}
