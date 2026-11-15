package com.lru.ui;

import com.lru.model.CacheOperation;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import java.awt.Component;
import java.awt.Font;

/** Colour-codes history rows: evictions in orange, failed/miss operations in red, others neutral. */
final class OperationRenderer extends DefaultListCellRenderer {

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                  boolean selected, boolean focus) {
        super.getListCellRendererComponent(list, value, index, selected, focus);
        setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        setFont(Theme.MONO);
        if (value instanceof CacheOperation op) {
            setText(op.describe());
            if (!selected) {
                if (op.causedEviction()) {
                    setForeground(Theme.WARN);
                    setFont(Theme.MONO.deriveFont(Font.BOLD));
                } else if (!op.success()) {
                    setForeground(Theme.DANGER);
                } else {
                    setForeground(Theme.TEXT);
                }
            }
        }
        return this;
    }
}
