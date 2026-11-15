package com.lru.ui;

import javax.swing.BorderFactory;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.Color;
import java.awt.Font;

/** Central place for colours, fonts and borders so the whole UI stays visually consistent. */
final class Theme {

    static final Color BG = new Color(243, 244, 248);
    static final Color CARD = Color.WHITE;
    static final Color BORDER = new Color(223, 226, 234);
    static final Color TEXT = new Color(31, 41, 55);
    static final Color MUTED = new Color(107, 114, 128);

    static final Color PRIMARY = new Color(79, 70, 229);
    static final Color SUCCESS = new Color(22, 163, 74);
    static final Color SUCCESS_SOFT = new Color(231, 247, 237);
    static final Color WARN = new Color(234, 88, 12);
    static final Color WARN_SOFT = new Color(255, 241, 230);
    static final Color DANGER = new Color(220, 38, 38);
    static final Color DANGER_SOFT = new Color(254, 233, 233);
    static final Color HIGHLIGHT = new Color(245, 158, 11);
    static final Color SLATE = new Color(71, 85, 105);
    static final Color TEAL = new Color(13, 148, 136);

    static final Font BASE = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    static final Font BOLD = new Font(Font.SANS_SERIF, Font.BOLD, 13);
    static final Font MONO = new Font(Font.MONOSPACED, Font.PLAIN, 12);

    private Theme() {
    }

    static Border card(int padding) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                BorderFactory.createEmptyBorder(padding, padding, padding, padding));
    }

    static Border titled(String title) {
        TitledBorder border = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(BORDER, 1, true), " " + title + " ");
        border.setTitleFont(BOLD);
        border.setTitleColor(MUTED);
        return border;
    }
}
