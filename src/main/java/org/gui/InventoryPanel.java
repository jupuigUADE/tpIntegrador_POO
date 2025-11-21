package org.gui;

import org.models.DetalleIngrediente;
import org.models.StockGeneral;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.BiConsumer;

public class InventoryPanel extends JPanel {
    private final StockGeneral sistemaStock;
    private final DefaultTableModel tableModel;
    private final JTable table;
    // Optional listener to notify when a row is selected (ingredient id)
    private Consumer<Integer> selectionListener;
    // Optional listener to notify when a row action is requested (id, actionKey)
    private BiConsumer<Integer,String> rowActionListener;

    public InventoryPanel(StockGeneral sistemaStock) {
        super(new BorderLayout());
        this.sistemaStock = sistemaStock;

        String[] cols = {"ID", "Nombre", "Cantidad", "Unidad", "Punto Reorden"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);

        // highlight rows that need reorden
        table.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                try {
                    Double cantidad = Double.parseDouble(String.valueOf(table.getModel().getValueAt(row, 2)));
                    Integer punto = Integer.parseInt(String.valueOf(table.getModel().getValueAt(row, 4)));
                    if (cantidad <= punto) {
                        c.setBackground(new Color(255, 220, 220));
                    } else {
                        c.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
                    }
                } catch (Exception ex) {
                    c.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
                }
                return c;
            }
        });

        // Notify selection on mouse click
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Only treat double-click as selection autocomplete (avoids accidental fills)
                if (e.getClickCount() == 2) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0 && selectionListener != null) {
                        Object idObj = tableModel.getValueAt(row, 0);
                        if (idObj instanceof Integer) {
                            selectionListener.accept((Integer) idObj);
                        } else {
                            try { selectionListener.accept(Integer.parseInt(String.valueOf(idObj))); } catch (Exception ex) { /* ignore */ }
                        }
                    }
                }
            }
        });

        // Popup menu for right-click actions
        JPopupMenu popup = new JPopupMenu();
        JMenuItem miUseId = new JMenuItem("Use ID");
        JMenuItem miAutofill = new JMenuItem("Autofill name/unit");
        JMenuItem miEntrada1 = new JMenuItem("Entrada 1");
        JMenuItem miSalida1 = new JMenuItem("Salida 1");
        popup.add(miUseId);
        popup.add(miAutofill);
        popup.addSeparator();
        popup.add(miEntrada1);
        popup.add(miSalida1);

        miUseId.addActionListener(ae -> {
            int row = table.getSelectedRow();
            if (row >= 0 && rowActionListener != null) {
                Object idObj = tableModel.getValueAt(row, 0);
                try { rowActionListener.accept(Integer.parseInt(String.valueOf(idObj)), "useId"); } catch (Exception ex) { /* ignore */ }
            }
        });
        miAutofill.addActionListener(ae -> {
            int row = table.getSelectedRow();
            if (row >= 0 && rowActionListener != null) {
                Object idObj = tableModel.getValueAt(row, 0);
                try { rowActionListener.accept(Integer.parseInt(String.valueOf(idObj)), "autofill"); } catch (Exception ex) { /* ignore */ }
            }
        });
        miEntrada1.addActionListener(ae -> {
            int row = table.getSelectedRow();
            if (row >= 0 && rowActionListener != null) {
                Object idObj = tableModel.getValueAt(row, 0);
                try { rowActionListener.accept(Integer.parseInt(String.valueOf(idObj)), "entrada1"); } catch (Exception ex) { /* ignore */ }
            }
        });
        miSalida1.addActionListener(ae -> {
            int row = table.getSelectedRow();
            if (row >= 0 && rowActionListener != null) {
                Object idObj = tableModel.getValueAt(row, 0);
                try { rowActionListener.accept(Integer.parseInt(String.valueOf(idObj)), "salida1"); } catch (Exception ex) { /* ignore */ }
            }
        });

        add(new JScrollPane(table), BorderLayout.CENTER);
        refreshTable();
        // Attach the popup on mouse pressed (cross-platform)
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        table.setRowSelectionInterval(row, row);
                        popup.show(table, e.getX(), e.getY());
                    }
                }
            }
        });
    }

    public void setSelectionListener(Consumer<Integer> listener) {
        this.selectionListener = listener;
    }

    public void setRowActionListener(BiConsumer<Integer,String> listener) {
        this.rowActionListener = listener;
    }

    public void refreshTable() {
        tableModel.setRowCount(0);
        for (Map.Entry<Integer, DetalleIngrediente> e : sistemaStock.getInventario().entrySet()) {
            DetalleIngrediente s = e.getValue();
            Object[] row = new Object[] {
                s.getIngrediente().getId(),
                s.getIngrediente().getNombre(),
                s.getCantidadActual(),
                s.getIngrediente().getUnidadMedida(),
                s.getCantidadMinima()
            };
            tableModel.addRow(row);
        }
    }

    public void selectRowForId(int id) {
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            if (tableModel.getValueAt(row, 0).equals(id)) {
                table.setRowSelectionInterval(row, row);
                table.scrollRectToVisible(table.getCellRect(row, 0, true));
                break;
            }
        }
    }

    public JTable getTable() { return table; }
}
