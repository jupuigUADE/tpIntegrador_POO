// Update ControlsPanel: add name/unit labels, quick Entrada/Salida buttons, handle rowActionListener and flash UI on autocomplete
// ...existing code...
package org.gui;

import org.models.DetalleIngrediente;
import org.models.StockGeneral;
import org.models.TipoMovimiento;
import org.exceptions.InvalidQuantityException;
import org.exceptions.StockNotFoundException;
import org.exceptions.InsufficientStockException;
import org.exceptions.MovementNotSupportedException;

import javax.swing.*;
import java.awt.*;

public class ControlsPanel extends JPanel {
    private final StockGeneral sistemaStock;
    private final InventoryPanel inventoryPanel;
    private final boolean isAdmin;

    private final JTextField txtId;
    private final JTextField txtCantidad;
    private final JComboBox<TipoMovimiento> cmbTipo;
    private final JButton btnRegistrar;
    private final JButton btnEntradaQuick;
    private final JButton btnSalidaQuick;
    private final JLabel lblName;
    private final JLabel lblUnit;

    public ControlsPanel(StockGeneral sistemaStock, InventoryPanel inventoryPanel, boolean isAdmin) {
        super(new GridBagLayout());
        this.sistemaStock = sistemaStock;
        this.inventoryPanel = inventoryPanel;
        this.isAdmin = isAdmin;

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4,4,4,4);
        c.fill = GridBagConstraints.HORIZONTAL;

        txtId = new JTextField(6);
        txtCantidad = new JTextField(6);
        cmbTipo = new JComboBox<>(TipoMovimiento.values());
        btnRegistrar = new JButton("Registrar Movimiento");
        btnEntradaQuick = new JButton("Entrada 1");
        btnSalidaQuick = new JButton("Salida 1");
        lblName = new JLabel("Nombre: -");
        lblUnit = new JLabel("Unidad: -");

        c.gridx = 0; c.gridy = 0; add(new JLabel("Ingrediente ID:"), c);
        c.gridx = 1; add(txtId, c);
        c.gridx = 2; add(new JLabel("Cantidad:"), c);
        c.gridx = 3; add(txtCantidad, c);
        c.gridx = 0; c.gridy = 1; add(new JLabel("Tipo:"), c);
        c.gridx = 1; add(cmbTipo, c);
        c.gridx = 2; add(lblName, c);
        c.gridx = 3; add(lblUnit, c);
        c.gridx = 0; c.gridy = 2; c.gridwidth = 2; add(btnRegistrar, c);
        c.gridwidth = 1; c.gridx = 2; add(btnEntradaQuick, c);
        c.gridx = 3; add(btnSalidaQuick, c);

        // Wire inventory selection to autocomplete the ID field
        inventoryPanel.setSelectionListener(id -> {
            if (id != null) {
                SwingUtilities.invokeLater(() -> {
                    txtId.setText(String.valueOf(id));
                    txtId.requestFocusInWindow();
                    txtId.selectAll();
                    // autofill name/unit and default quantity
                    DetalleIngrediente s = sistemaStock.obtenerStockPorId(id);
                    if (s != null) {
                        lblName.setText("Nombre: " + s.getIngrediente().getNombre());
                        lblUnit.setText("Unidad: " + s.getIngrediente().getUnidadMedida());
                        txtCantidad.setText("1");
                    }
                    flashComponent(txtId);
                });
            }
        });

        // If user is not admin, disable modification controls
        if (!isAdmin) {
            txtId.setEnabled(false);
            txtCantidad.setEnabled(false);
            cmbTipo.setEnabled(false);
            btnRegistrar.setEnabled(false);
            btnEntradaQuick.setEnabled(false);
            btnSalidaQuick.setEnabled(false);
        }

        // Handle popup/context menu actions from the inventory panel
        inventoryPanel.setRowActionListener((id, actionKey) -> {
            if (id == null || actionKey == null) return;
            SwingUtilities.invokeLater(() -> {
                switch (actionKey) {
                    case "useId":
                        txtId.setText(String.valueOf(id));
                        txtId.requestFocusInWindow();
                        txtId.selectAll();
                        flashComponent(txtId);
                        break;
                    case "autofill":
                        DetalleIngrediente s = sistemaStock.obtenerStockPorId(id);
                        if (s != null) {
                            txtId.setText(String.valueOf(id));
                            lblName.setText("Nombre: " + s.getIngrediente().getNombre());
                            lblUnit.setText("Unidad: " + s.getIngrediente().getUnidadMedida());
                            txtCantidad.setText("1");
                            flashComponent(lblName);
                            flashComponent(lblUnit);
                        }
                        break;
                    case "entrada1":
                        txtId.setText(String.valueOf(id));
                        txtCantidad.setText("1");
                        cmbTipo.setSelectedItem(TipoMovimiento.ENTRADA);
                        onRegistrar();
                        break;
                    case "salida1":
                        txtId.setText(String.valueOf(id));
                        txtCantidad.setText("1");
                        cmbTipo.setSelectedItem(TipoMovimiento.SALIDA);
                        onRegistrar();
                        break;
                }
            });
        });

        // Quick buttons behavior
        btnEntradaQuick.addActionListener(e -> {
            if (!isAdmin) { JOptionPane.showMessageDialog(this, "Only admins can modify stock.", "Permission denied", JOptionPane.ERROR_MESSAGE); return; }
            try {
                Integer.parseInt(txtId.getText().trim());
                txtCantidad.setText("1");
                cmbTipo.setSelectedItem(TipoMovimiento.ENTRADA);
                onRegistrar();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Set a valid ID first.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnSalidaQuick.addActionListener(e -> {
            if (!isAdmin) { JOptionPane.showMessageDialog(this, "Only admins can modify stock.", "Permission denied", JOptionPane.ERROR_MESSAGE); return; }
            try {
                Integer.parseInt(txtId.getText().trim());
                txtCantidad.setText("1");
                cmbTipo.setSelectedItem(TipoMovimiento.SALIDA);
                onRegistrar();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Set a valid ID first.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnRegistrar.addActionListener(e -> onRegistrar());
    }

    private void onRegistrar() {
        if (!isAdmin) { JOptionPane.showMessageDialog(this, "Only admins can modify stock.", "Permission denied", JOptionPane.ERROR_MESSAGE); return; }
        try {
            int id = Integer.parseInt(txtId.getText().trim());
            double cantidad = Double.parseDouble(txtCantidad.getText().trim());
            TipoMovimiento tipo = (TipoMovimiento) cmbTipo.getSelectedItem();

            // Basic validation
            if (cantidad <= 0) {
                JOptionPane.showMessageDialog(this, "La cantidad debe ser mayor que 0.", "Error de entrada", JOptionPane.ERROR_MESSAGE);
                return;
            }

            DetalleIngrediente stockActual = sistemaStock.obtenerStockPorId(id);
            if (stockActual == null) {
                JOptionPane.showMessageDialog(this, "Ingrediente con ID " + id + " no encontrado.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (tipo == TipoMovimiento.SALIDA && stockActual.getCantidadActual() < cantidad) {
                JOptionPane.showMessageDialog(this, "Stock insuficiente. Disponible: " + stockActual.getCantidadActual() + " " + stockActual.getIngrediente().getUnidadMedida(), "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                Double nuevo = sistemaStock.registrarMovimiento(id, cantidad, tipo);
                if (nuevo != null) {
                    inventoryPanel.refreshTable();
                    inventoryPanel.getTable().revalidate();
                    inventoryPanel.getTable().repaint();
                    inventoryPanel.selectRowForId(id);
                    String unidad = stockActual.getIngrediente().getUnidadMedida().name();
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Movimiento registrado. Nuevo stock: " + nuevo + " " + unidad));
                } else {
                    JOptionPane.showMessageDialog(this, "No se pudo registrar el movimiento." , "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (InvalidQuantityException iq) {
                JOptionPane.showMessageDialog(this, "Cantidad inválida: " + iq.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            } catch (StockNotFoundException snf) {
                JOptionPane.showMessageDialog(this, "Ingrediente no encontrado: " + snf.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            } catch (InsufficientStockException ise) {
                JOptionPane.showMessageDialog(this, "Stock insuficiente: " + ise.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            } catch (MovementNotSupportedException mns) {
                JOptionPane.showMessageDialog(this, "Tipo de movimiento no soportado: " + mns.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "ID y Cantidad deben ser numéricos.", "Error de entrada", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Briefly flash a component background to give visual feedback
    private void flashComponent(JComponent comp) {
        Color original = comp.getBackground();
        Color flash = new Color(255, 255, 150);
        comp.setBackground(flash);
        Timer t = new Timer(300, e -> comp.setBackground(original));
        t.setRepeats(false);
        t.start();
    }
}
