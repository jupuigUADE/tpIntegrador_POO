package org.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import org.models.TableStatus;

public final class Icons {
    private Icons() {}

    private static ImageIcon circleIcon(Color color, String text, int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // background transparent
            g.setComposite(AlphaComposite.Src);
            g.setColor(new Color(0,0,0,0));
            g.fillRect(0,0,size,size);

            // draw circle
            g.setColor(color);
            g.fillOval(0,0,size-1,size-1);

            // draw border
            g.setColor(color.darker());
            g.setStroke(new BasicStroke(Math.max(1, size/20f)));
            g.drawOval(0,0,size-1,size-1);

            // draw text (centered)
            if (text != null && !text.isEmpty()) {
                g.setFont(new Font("Segoe UI", Font.BOLD, Math.max(10, size/2)));
                FontMetrics fm = g.getFontMetrics();
                int tw = fm.stringWidth(text);
                int th = fm.getAscent();
                g.setColor(Color.WHITE);
                g.drawString(text, (size - tw) / 2, (size + th) / 2 - 2);
            }
        } finally {
            g.dispose();
        }
        return new ImageIcon(img);
    }

    public static ImageIcon tableIcon(TableStatus status, int size) {
        if (status == null) status = TableStatus.LIBRE;
        return switch (status) {
            case LIBRE -> circleIcon(new Color(0x4CAF50), "L", size);
            case RESERVADA -> circleIcon(new Color(0xFFC107), "R", size);
            case OCUPADA -> circleIcon(new Color(0xF44336), "O", size);
        };
    }

    public static ImageIcon reserveIcon(int size) { return circleIcon(new Color(0xFFC107), "R", size); }
    public static ImageIcon seatIcon(int size) { return circleIcon(new Color(0x4CAF50), "S", size); }
    public static ImageIcon releaseIcon(int size) { return circleIcon(new Color(0x9E9E9E), "X", size); }
    public static ImageIcon refreshIcon(int size) { return circleIcon(new Color(0x2D89EF), "↻", size); }
    public static ImageIcon orderIcon(int size) { return circleIcon(new Color(0x795548), "☕", size); }
    public static ImageIcon billIcon(int size) { return circleIcon(new Color(0x607D8B), "$", size); }
}

