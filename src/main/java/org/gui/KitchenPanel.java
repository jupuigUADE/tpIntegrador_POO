package org.gui;

import org.models.Pedido;
import org.models.ProductionJob;
import org.models.StockGeneral;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class KitchenPanel extends JPanel {
    private final Pedido productionService;
    private final StockGeneral stock;

    private final DefaultTableModel jobsModel;
    private final JTable jobsTable;
    private final DefaultListModel<String> lowStockModel;
    private final JList<String> lowStockList;
    private final JButton btnRefresh;
    private final JButton btnStart;
    private final JButton btnFinish;

    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public KitchenPanel(Pedido productionService, StockGeneral stock) {
        super(new BorderLayout());
        this.productionService = productionService;
        this.stock = stock;

        // Jobs table
        String[] cols = {"ID", "Recipe", "Qty", "Status", "Created", "Started"};
        jobsModel = new DefaultTableModel(cols, 0) { @Override public boolean isCellEditable(int r,int c){return false;} };
        jobsTable = new JTable(jobsModel);

        // Low-stock list
        lowStockModel = new DefaultListModel<>();
        lowStockList = new JList<>(lowStockModel);

        // Controls
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnRefresh = new JButton("Refresh");
        btnStart = new JButton("Start Selected");
        btnFinish = new JButton("Finish Selected");
        top.add(btnRefresh); top.add(btnStart); top.add(btnFinish);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setTopComponent(new JScrollPane(jobsTable));

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(new JLabel("Low stock"), BorderLayout.NORTH);
        bottom.add(new JScrollPane(lowStockList), BorderLayout.CENTER);
        split.setBottomComponent(bottom);
        split.setResizeWeight(0.7);

        add(top, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);

        btnRefresh.addActionListener(e -> refreshAll());
        btnStart.addActionListener(e -> startSelected());
        btnFinish.addActionListener(e -> finishSelected());

        refreshAll();

        // Subscribe to productionService updates so the kitchen view updates live
        productionService.addJobListener(job -> SwingUtilities.invokeLater(this::refreshAll));
    }

    private Integer getSelectedJobId() {
        int row = jobsTable.getSelectedRow();
        if (row < 0) return null;
        Object idObj = jobsModel.getValueAt(row, 0);
        try { return Integer.parseInt(String.valueOf(idObj)); } catch (Exception ex) { return null; }
    }

    private void startSelected() {
        Integer id = getSelectedJobId();
        if (id == null) {
            JOptionPane.showMessageDialog(this, "Select a job first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        boolean ok = productionService.startJob(id);
        if (ok) JOptionPane.showMessageDialog(this, "Job started."); else JOptionPane.showMessageDialog(this, "Failed to start job.", "Error", JOptionPane.ERROR_MESSAGE);
        refreshAll();
    }

    private void finishSelected() {
        Integer id = getSelectedJobId();
        if (id == null) {
            JOptionPane.showMessageDialog(this, "Select a job first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        boolean ok = productionService.finishJob(id, true);
        if (ok) JOptionPane.showMessageDialog(this, "Job finished."); else JOptionPane.showMessageDialog(this, "Failed to finish job.", "Error", JOptionPane.ERROR_MESSAGE);
        refreshAll();
    }

    public void refreshAll() {
        refreshJobs();
        refreshLowStock();
    }

    private void refreshJobs() {
        jobsModel.setRowCount(0);
        List<ProductionJob> jobs = productionService.listJobs();
        for (ProductionJob j : jobs) {
            Object[] row = new Object[]{
                    j.getId(), j.getRecipe().getNombre(), j.getQuantity(), j.getStatus(),
                    j.getCreatedAt()!=null?j.getCreatedAt().format(dtf):"-",
                    j.getStartedAt()!=null?j.getStartedAt().format(dtf):"-"
            };
            jobsModel.addRow(row);
        }
    }

    private void refreshLowStock() {
        lowStockModel.clear();
        stock.getInventario().forEach((id, s) -> {
            if (s.getCantidadActual() <= s.getCantidadMinima()) {
                lowStockModel.addElement(s.getIngrediente().getNombre() + " - " + s.getCantidadActual() + " " + s.getIngrediente().getUnidadMedida());
            }
        });
    }
}
