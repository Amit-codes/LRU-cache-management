package com.lru.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Flat rounded button with hover and pressed states (plain Swing buttons look dated). */
final class StyledButton extends JButton {

    private final Color base;

    StyledButton(String text, Color base) {
        super(text);
        this.base = base;
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(false);
        setForeground(Color.WHITE);
        setFont(Theme.BOLD);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color fill;
        if (!isEnabled()) {
            fill = new Color(180, 184, 194);
        } else if (getModel().isPressed()) {
            fill = base.darker();
        } else if (getModel().isRollover()) {
            fill = new Color(Math.min(255, base.getRed() + 25), Math.min(255, base.getGreen() + 25), Math.min(255, base.getBlue() + 25));
        } else {
            fill = base;
        }
        g.setColor(fill);
        g.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
        g.dispose();
        super.paintComponent(g0);
    }
}
