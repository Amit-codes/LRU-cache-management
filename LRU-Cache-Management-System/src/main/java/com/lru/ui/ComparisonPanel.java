package com.lru.ui;

import com.lru.exception.CacheException;
import com.lru.simulation.PolicyComparator;
import com.lru.simulation.PolicyComparator.Result;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

/** Replays one request sequence against LRU, FIFO and LFU and shows which policy gets the best hit ratio. */
public class ComparisonPanel extends JPanel {

    private final JTextField requestsField = new JTextField("1 2 3 1 2 4 1 2 5 1 2 3 1 2", 36);
    private final JSpinner capacitySpinner = new JSpinner(new SpinnerNumberModel(3, 1, 50, 1));
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"Policy", "Hits", "Misses", "Evictions", "Hit ratio"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final BarChart chart = new BarChart();
    private final JLabel summary = new JLabel(" ");

    public ComparisonPanel() {
        super(new BorderLayout(0, 12));
        setBackground(Theme.BG);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel controls = new JPanel(new BorderLayout());
        controls.setBackground(Theme.CARD);
        controls.setBorder(Theme.card(6));
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        row.setOpaque(false);
        row.add(bold("Request sequence (keys)"));
        row.add(requestsField);
        row.add(bold("Capacity"));
        row.add(capacitySpinner);
        StyledButton run = new StyledButton("Run comparison", Theme.PRIMARY);
        run.addActionListener(e -> runComparison());
        requestsField.addActionListener(e -> runComparison());
        row.add(run);
        controls.add(row, BorderLayout.CENTER);
        JLabel hint = new JLabel("Every request is a GET; on a miss the key is loaded into the cache. "
                + "LRU keeps recently touched keys, FIFO ignores reads, LFU keeps frequently used keys.");
        hint.setFont(Theme.BASE);
        hint.setForeground(Theme.MUTED);
        hint.setBorder(BorderFactory.createEmptyBorder(0, 8, 4, 0));
        controls.add(hint, BorderLayout.SOUTH);

        JTable table = new JTable(tableModel);
        table.setRowHeight(28);
        table.setFont(Theme.BASE.deriveFont(14f));
        table.getTableHeader().setFont(Theme.BOLD);
        table.setEnabled(false);
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(Theme.titled("Results"));
        tableScroll.setPreferredSize(new Dimension(100, 140));

        summary.setFont(Theme.BOLD.deriveFont(15f));
        summary.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        JPanel chartHolder = new JPanel(new BorderLayout());
        chartHolder.setBackground(Theme.CARD);
        chartHolder.setBorder(Theme.titled("Hit ratio by policy"));
        chartHolder.add(chart, BorderLayout.CENTER);

        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setOpaque(false);
        center.add(tableScroll, BorderLayout.NORTH);
        center.add(chartHolder, BorderLayout.CENTER);
        center.add(summary, BorderLayout.SOUTH);

        add(controls, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        runComparison();
    }

    private JLabel bold(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.BOLD);
        l.setForeground(Theme.MUTED);
        return l;
    }

    private void runComparison() {
        try {
            List<String> requests = PolicyComparator.parseRequests(requestsField.getText());
            List<Result> results = PolicyComparator.compare(requests, (Integer) capacitySpinner.getValue());
            tableModel.setRowCount(0);
            double best = 0;
            for (Result r : results) {
                tableModel.addRow(new Object[]{r.policy(), r.hits(), r.misses(), r.evictions(),
                        String.format("%.1f%%", r.hitRatio() * 100)});
                best = Math.max(best, r.hitRatio());
            }
            List<String> winners = new ArrayList<>();
            for (Result r : results) {
                if (Math.abs(r.hitRatio() - best) < 1e-9) {
                    winners.add(r.policy());
                }
            }
            summary.setForeground(Theme.SUCCESS);
            summary.setText("Best hit ratio: " + String.join(" / ", winners) + String.format("  (%.1f%%)", best * 100)
                    + "   over " + requests.size() + " requests");
            chart.setResults(results);
        } catch (CacheException ex) {
            summary.setForeground(Theme.DANGER);
            summary.setText("Error: " + ex.getMessage());
        }
    }

    /** Minimal custom bar chart (no charting library needed). */
    private static final class BarChart extends JPanel {
        private List<Result> results = List.of();

        BarChart() {
            setOpaque(false);
            setPreferredSize(new Dimension(400, 220));
        }

        void setResults(List<Result> results) {
            this.results = results;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            if (results.isEmpty()) {
                return;
            }
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            int base = h - 34;
            int top = 30;
            int slot = Math.max(120, w / results.size());
            int barW = Math.min(110, slot - 60);
            Color[] colors = {Theme.PRIMARY, Theme.SLATE, Theme.TEAL};

            g.setColor(Theme.BORDER);
            g.drawLine(24, base, w - 24, base);
            for (int i = 0; i < results.size(); i++) {
                Result r = results.get(i);
                int barH = (int) Math.round((base - top) * r.hitRatio());
                int x = (w - slot * results.size()) / 2 + i * slot + (slot - barW) / 2;
                g.setColor(colors[i % colors.length]);
                g.fillRoundRect(x, base - barH, barW, Math.max(barH, 2), 10, 10);

                g.setFont(Theme.BOLD.deriveFont(Font.BOLD, 14f));
                FontMetrics fm = g.getFontMetrics();
                String pct = String.format("%.1f%%", r.hitRatio() * 100);
                g.setColor(Theme.TEXT);
                g.drawString(pct, x + (barW - fm.stringWidth(pct)) / 2, base - barH - 8);
                g.drawString(r.policy(), x + (barW - fm.stringWidth(r.policy())) / 2, base + 22);
            }
            g.dispose();
        }
    }
}
