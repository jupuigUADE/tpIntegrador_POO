package org.gui;

import org.models.*;
import org.models.Menu;

import javax.swing.*;
import java.awt.*;

public class MainGui extends JFrame {
    private final StockGeneral sistemaStock;
    private final Menu menu;
    private final ReservationService reservationService;
    private final TableService tableService;
    private final Pedido productionService;
    private User currentUser;
    private final org.gui.InventoryPanel inventoryPanel;

    public MainGui(StockGeneral sistemaStock, Menu menu, ReservationService reservationService, TableService tableService, Pedido productionService, User user) {
        super("Sistema de Gestión Gastronómica");
        this.sistemaStock = sistemaStock;
        this.menu = menu;
        this.reservationService = reservationService;
        this.tableService = tableService;
        this.productionService = productionService;
        this.currentUser = user;

        // Apply global styles/look-and-feel before creating components
        Styles.apply();

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1000, 600);
        setLocationRelativeTo(null);

        // Use BorderLayout for the content pane before adding components
        getContentPane().setLayout(new BorderLayout());

        // Top bar with current user and logout
        JPanel topBar = new JPanel(new BorderLayout());
        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JLabel lblUser = new JLabel("User: " + (user != null ? user.getUsername() : "(none)"));
        JButton btnLogout = new JButton("Logout");
        userPanel.add(lblUser);
        userPanel.add(btnLogout);
        topBar.add(userPanel, BorderLayout.EAST);
        getContentPane().add(topBar, BorderLayout.NORTH);

        // Inventory panel (table)
        inventoryPanel = new org.gui.InventoryPanel(sistemaStock);
        // Listen for stock changes so inventory view updates when auto-reorder or other services modify stock
        sistemaStock.addStockListener(stockItem -> SwingUtilities.invokeLater(() -> {
            inventoryPanel.refreshTable();
            inventoryPanel.getTable().revalidate();
            inventoryPanel.getTable().repaint();
        }));
        org.gui.ControlsPanel controls = new org.gui.ControlsPanel(sistemaStock, inventoryPanel, user != null && user.isAdmin());
        JPanel inventoryTab = new JPanel(new BorderLayout());
        inventoryTab.add(inventoryPanel, BorderLayout.CENTER);
        inventoryTab.add(controls, BorderLayout.SOUTH);

        // Menu panel
        MenuPanel menuPanel = new MenuPanel(menu, sistemaStock, tableService);

        // Reservations panel
        org.gui.ReservationsPanel reservationsPanel = new org.gui.ReservationsPanel(reservationService, tableService);

        // Tables panel
        TablesPanel tablesPanel = new TablesPanel(tableService, reservationService, menu);

        // Production panel
        org.gui.ProductionPanel productionPanel = new org.gui.ProductionPanel(productionService, menu, sistemaStock);
        // Kitchen panel (compact view for staff)
        org.gui.KitchenPanel kitchenPanel = new KitchenPanel(productionService, sistemaStock);

        // Tabbed pane
        JTabbedPane tabs = new JTabbedPane();

        // Decide visible tabs based on user role (simple heuristic using username and admin flag)
        String uname = user != null ? user.getUsername().toLowerCase() : "";
        boolean isAdmin = user != null && user.isAdmin();
        boolean isWaiter = !isAdmin && uname.contains("waiter");
        boolean isChef = !isAdmin && uname.contains("chef");

        if (isAdmin) {
            tabs.addTab("Inventario", inventoryTab);
            tabs.addTab("Menú", menuPanel);
            tabs.addTab("Mesas", tablesPanel);
            tabs.addTab("Reservas", reservationsPanel);
            tabs.addTab("Producción", productionPanel);
            tabs.addTab("Cocina", kitchenPanel);
        } else if (isWaiter) {
            // Waiter: hide Cocina, Inventario, Menú
            tabs.addTab("Mesas", tablesPanel);
            tabs.addTab("Reservas", reservationsPanel);
            tabs.addTab("Producción", productionPanel);
        } else if (isChef) {
            // Chef: only Cocina and Menú
            tabs.addTab("Menú", menuPanel);
            tabs.addTab("Cocina", kitchenPanel);
        } else {
            // Default fallback: minimal access (Mesas and Reservas)
            tabs.addTab("Mesas", tablesPanel);
            tabs.addTab("Reservas", reservationsPanel);
        }

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

        getContentPane().add(tabs, BorderLayout.CENTER);

        // Logout button behavior: prompt login again and recreate MainGui or exit
        btnLogout.addActionListener(ev -> {
            // Show login dialog (use this frame as owner)
            User newUser = LoginDialog.showLogin(this);
            if (newUser == null) {
                // user cancelled -> exit app
                System.out.println("No user logged in after logout. Exiting.");
                System.exit(0);
            }

            // Open a new MainGui for the newly logged user and dispose this one
            SwingUtilities.invokeLater(() -> {
                MainGui newGui = new MainGui(this.sistemaStock, this.menu, this.reservationService, this.tableService, this.productionService, newUser);
                newGui.setVisible(true);
                this.dispose();
            });
        });

        // Style components recursively so backgrounds/fonts apply to nested panels
        Styles.styleComponent(this.getContentPane());
        // Small tweak: set tabbed pane background to tab content color
        Color tabBg = UIManager.getColor("TabbedPane.contentAreaColor");
        if (tabBg != null) tabs.setBackground(tabBg);
    }
}
