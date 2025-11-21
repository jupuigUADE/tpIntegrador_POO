package org.gui;

import org.gui.KitchenPanel;
import org.gui.MenuPanel;
import org.gui.TablesPanel;
import org.models.*;
import org.models.Menu;

import javax.swing.*;
import java.awt.*;

public class MainGui extends JFrame {
    private final StockGeneral sistemaStock;
    private final org.gui.InventoryPanel inventoryPanel;

    public MainGui(StockGeneral sistemaStock, Menu menu, ReservationService reservationService, TableService tableService, Pedido productionService, User user) {
        super("Sistema de Gestión Gastronómica");
        this.sistemaStock = sistemaStock;

        // Apply global styles/look-and-feel before creating components
        Styles.apply();

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1000, 600);
        setLocationRelativeTo(null);

        // Inventory panel (table)
        inventoryPanel = new org.gui.InventoryPanel(sistemaStock);
        org.gui.ControlsPanel controls = new org.gui.ControlsPanel(sistemaStock, inventoryPanel, user != null && user.isAdmin());
        JPanel inventoryTab = new JPanel(new BorderLayout());
        inventoryTab.add(inventoryPanel, BorderLayout.CENTER);
        inventoryTab.add(controls, BorderLayout.SOUTH);

        // Menu panel
        MenuPanel menuPanel = new MenuPanel(menu, sistemaStock);

        // Reservations panel
        org.gui.ReservationsPanel reservationsPanel = new org.gui.ReservationsPanel(reservationService, tableService);

        // Tables panel
        TablesPanel tablesPanel = new TablesPanel(tableService, reservationService);

        // Production panel
        org.gui.ProductionPanel productionPanel = new org.gui.ProductionPanel(productionService, menu, sistemaStock);
        // Kitchen panel (compact view for staff)
        org.gui.KitchenPanel kitchenPanel = new KitchenPanel(productionService, sistemaStock);

        // Tabbed pane
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Inventario", inventoryTab);
        tabs.addTab("Menú", menuPanel);
        tabs.addTab("Mesas", tablesPanel);
        tabs.addTab("Reservas", reservationsPanel);
        tabs.addTab("Producción", productionPanel);
        tabs.addTab("Cocina", kitchenPanel);

        // Refresh relevant panels when switching tabs so updates appear immediately
        tabs.addChangeListener(ev -> {
            int idx = tabs.getSelectedIndex();
            if (idx < 0) return;
            String title = tabs.getTitleAt(idx);
            if ("Cocina".equals(title)) {
                kitchenPanel.refreshAll();
            } else if ("Producción".equals(title)) {
                productionPanel.refreshTable();
            } else if ("Inventario".equals(title)) {
                inventoryPanel.refreshTable();
            } else if ("Mesas".equals(title)) {
                tablesPanel.refreshTable();
            }
        });

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(tabs, BorderLayout.CENTER);

        // Style components recursively so backgrounds/fonts apply to nested panels
        Styles.styleComponent(this.getContentPane());
        // Small tweak: set tabbed pane background to tab content color
        Color tabBg = UIManager.getColor("TabbedPane.contentAreaColor");
        if (tabBg != null) tabs.setBackground(tabBg);
    }
}
