package com.lru;

import com.lru.cache.CacheManager;
import com.lru.exception.CacheException;
import com.lru.ui.Dashboard;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Application entry point.
 * Usage: {@code java -jar lru-cache-management-system-1.0.0.jar [initialCapacity]}
 */
public final class Main {

    private static final int DEFAULT_CAPACITY = 5;

    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ignored) {
                // fall back to the default look and feel
            }
            int capacity = args.length > 0 ? parseOrDefault(args[0]) : askForCapacity();
            new Dashboard(capacity).setVisible(true);
        });
    }

    private static int parseOrDefault(String text) {
        try {
            return CacheManager.parseCapacity(text);
        } catch (CacheException e) {
            System.err.println(e.getMessage() + " - using default capacity " + DEFAULT_CAPACITY);
            return DEFAULT_CAPACITY;
        }
    }

    /** Keeps asking until the user enters a valid capacity; cancelling falls back to the default. */
    private static int askForCapacity() {
        String input = String.valueOf(DEFAULT_CAPACITY);
        while (true) {
            Object answer = JOptionPane.showInputDialog(null, "Initial cache capacity (positive whole number):",
                    "LRU Cache Management System", JOptionPane.QUESTION_MESSAGE, null, null, input);
            if (answer == null) {
                return DEFAULT_CAPACITY;
            }
            input = answer.toString();
            try {
                return CacheManager.parseCapacity(input);
            } catch (CacheException e) {
                JOptionPane.showMessageDialog(null, e.getMessage(), "Invalid capacity", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
