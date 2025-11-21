package org.gui;

import javax.swing.*;
import java.awt.*;

public class Styles {
    private Styles() {} // utility class

    public static final Font DEFAULT_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Color PRIMARY = new Color(0x2D89EF);
    private static final Color PANEL_BG = new Color(0xF5F7FA);
    private static final Color TAB_SELECTED = new Color(0xE8EEF8);

    public static void apply() {
        // Try to set Nimbus look and feel if available
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // Ignore LAF failures; fall back to default
        }

        // Global fonts
        UIManager.put("Label.font", DEFAULT_FONT);
        UIManager.put("Button.font", DEFAULT_FONT);
        UIManager.put("TextField.font", DEFAULT_FONT);
        UIManager.put("Table.font", DEFAULT_FONT);
        UIManager.put("TabbedPane.font", DEFAULT_FONT);
        UIManager.put("TableHeader.font", DEFAULT_FONT.deriveFont(Font.BOLD));

        // Colors
        UIManager.put("Panel.background", PANEL_BG);
        UIManager.put("TabbedPane.contentAreaColor", PANEL_BG);
        UIManager.put("TabbedPane.selected", TAB_SELECTED);
        UIManager.put("Table.background", Color.WHITE);
        UIManager.put("Table.selectionBackground", PRIMARY);
        UIManager.put("Table.selectionForeground", Color.WHITE);

        // Table tweaks
        UIManager.put("Table.rowHeight", 24);

        // Buttons
        UIManager.put("Button.background", PRIMARY);
        UIManager.put("Button.foreground", Color.WHITE);
        UIManager.put("Button.focus", PRIMARY.darker());

        // Text fields
        UIManager.put("TextField.background", Color.WHITE);

        // Other tweaks
        UIManager.put("ScrollPane.background", PANEL_BG);
        UIManager.put("ToolTip.background", new Color(0xFFFFFF));
        UIManager.put("ToolTip.foreground", Color.DARK_GRAY);
    }

    public static void styleComponent(Component c) {
        if (c == null) return;
        Color bg = UIManager.getColor("Panel.background");
        try {
            if (c instanceof JComponent jc) {
                jc.setOpaque(true);
            }
            if (bg != null) {
                c.setBackground(bg);
            }
            if (c instanceof JScrollPane sp) {
                if (sp.getViewport() != null) sp.getViewport().setBackground(bg);
            }
            if (c instanceof Container container) {
                for (Component child : container.getComponents()) {
                    styleComponent(child);
                }
            }
        } catch (Exception e) {
            // ignore styling errors
        }
    }
}
