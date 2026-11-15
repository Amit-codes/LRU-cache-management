package com.lru.ui;

import com.lru.cache.CacheManager;
import com.lru.exception.CacheException;
import com.lru.model.CacheOperation;
import com.lru.model.CacheStatistics;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.util.function.Supplier;

/**
 * Main window. The first tab is the live cache (statistics, controls, visualization, history);
 * the other tabs host the simulation and algorithm-comparison tools.
 */
public class Dashboard extends JFrame {

    private final CacheManager manager;
    private final CacheVisualizer visualizer = new CacheVisualizer();

    private final JTextField keyField = new JTextField(9);
    private final JTextField valueField = new JTextField(9);
    private final JTextField capacityField = new JTextField(4);
    private final JLabel status = new JLabel(" ");
    private final DefaultListModel<CacheOperation> historyModel = new DefaultListModel<>();

    private final StatCard capacityCard = new StatCard("Capacity", Theme.TEXT);
    private final StatCard sizeCard = new StatCard("Current size", Theme.TEXT);
    private final StatCard utilizationCard = new StatCard("Utilization", Theme.PRIMARY);
    private final StatCard hitsCard = new StatCard("Cache hits", Theme.SUCCESS);
    private final StatCard missesCard = new StatCard("Cache misses", Theme.WARN);
    private final StatCard ratioCard = new StatCard("Hit ratio", Theme.PRIMARY);
    private final StatCard evictionsCard = new StatCard("Evictions", Theme.DANGER);

    public Dashboard(int initialCapacity) {
        super("CacheFlow- LRU Cache Management System");
        this.manager = new CacheManager(initialCapacity);
        this.manager.addListener(this::onOperation);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1050, 720));
        setSize(1220, 800);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(buildHeader(), BorderLayout.NORTH);
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(Theme.BOLD);
        tabs.addTab("Cache Dashboard", buildLivePanel());
        tabs.addTab("Simulation", new SimulationPanel());
        tabs.addTab("Algorithm Comparison", new ComparisonPanel());
        add(tabs, BorderLayout.CENTER);

        capacityField.setText(String.valueOf(initialCapacity));
        visualizer.showState(manager.snapshot(), manager.capacity());
        refreshStats();
        setStatus("Ready. Enter a key and value, then press PUT.", Theme.MUTED);
    }

    // ------------------------------------------------------------------ layout

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.PRIMARY);
        header.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));
        JLabel title = new JLabel("CacheFlow- LRU Cache Management System");
        title.setForeground(Color.WHITE);
        title.setFont(Theme.BOLD.deriveFont(Font.BOLD, 20f));
        JLabel subtitle = new JLabel("HashMap + Doubly Linked List  |  O(1) get / put / remove");
        subtitle.setForeground(new Color(224, 231, 255));
        subtitle.setFont(Theme.BASE);
        header.add(title, BorderLayout.NORTH);
        header.add(subtitle, BorderLayout.SOUTH);
        return header;
    }

    private JPanel buildLivePanel() {
        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(Theme.BG);
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel stats = new JPanel(new GridLayout(1, 7, 10, 0));
        stats.setOpaque(false);
        for (StatCard card : new StatCard[]{capacityCard, sizeCard, utilizationCard, hitsCard, missesCard, ratioCard, evictionsCard}) {
            stats.add(card);
        }

        JScrollPane vizScroll = new JScrollPane(visualizer,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        vizScroll.setBorder(Theme.titled("Cache contents   (MRU \u2192 LRU)"));

        JList<CacheOperation> historyList = new JList<>(historyModel);
        historyList.setCellRenderer(new OperationRenderer());
        historyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane historyScroll = new JScrollPane(historyList);
        historyScroll.setBorder(Theme.titled("Operation history   (latest first)"));
        historyScroll.setPreferredSize(new Dimension(100, 190));

        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);
        center.add(buildControls(), BorderLayout.NORTH);
        center.add(vizScroll, BorderLayout.CENTER);
        center.add(historyScroll, BorderLayout.SOUTH);

        root.add(stats, BorderLayout.NORTH);
        root.add(center, BorderLayout.CENTER);
        return root;
    }

    private JPanel buildControls() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        row.setOpaque(false);

        row.add(label("Key"));
        row.add(keyField);
        row.add(label("Value"));
        row.add(valueField);
        row.add(button("PUT", Theme.PRIMARY, () -> manager.put(keyField.getText(), valueField.getText())));
        row.add(button("GET", Theme.SUCCESS, () -> manager.get(keyField.getText())));
        row.add(button("PEEK", Theme.TEAL, () -> manager.peek(keyField.getText())));
        row.add(button("REMOVE", Theme.WARN, () -> manager.remove(keyField.getText())));
        row.add(button("CLEAR", Theme.DANGER, manager::clear));
        row.add(label("   Capacity"));
        row.add(capacityField);
        row.add(button("RESIZE", Theme.SLATE, () -> manager.resize(CacheManager.parseCapacity(capacityField.getText()))));

        StyledButton resetStats = new StyledButton("Reset stats", Theme.MUTED);
        resetStats.addActionListener(e -> {
            manager.resetStatistics();
            refreshStats();
            setStatus("Statistics reset.", Theme.MUTED);
        });
        row.add(resetStats);

        valueField.addActionListener(e -> run(() -> manager.put(keyField.getText(), valueField.getText())));
        keyField.addActionListener(e -> run(() -> manager.get(keyField.getText())));

        status.setFont(Theme.BOLD);
        status.setBorder(BorderFactory.createEmptyBorder(0, 8, 2, 0));

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Theme.CARD);
        panel.setBorder(Theme.card(8));
        panel.add(row, BorderLayout.CENTER);
        panel.add(status, BorderLayout.SOUTH);
        return panel;
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.BOLD);
        l.setForeground(Theme.MUTED);
        return l;
    }

    private StyledButton button(String text, Color color, Supplier<CacheOperation> action) {
        StyledButton b = new StyledButton(text, color);
        b.addActionListener(e -> run(action));
        return b;
    }

    // ------------------------------------------------------------------ behaviour

    /** Runs an operation; validation problems are shown in the status line instead of crashing the UI. */
    private void run(Supplier<CacheOperation> action) {
        try {
            CacheOperation op = action.get();
            setStatus(op.message(), op.causedEviction() ? Theme.WARN : op.success() ? Theme.SUCCESS : Theme.DANGER);
        } catch (CacheException ex) {
            setStatus("Error: " + ex.getMessage(), Theme.DANGER);
            Toolkit.getDefaultToolkit().beep();
        }
    }

    private void onOperation(CacheOperation op) {
        visualizer.showOperation(manager.snapshot(), manager.capacity(), op);
        refreshStats();
        historyModel.clear();
        historyModel.addAll(manager.history().subList(0, Math.min(60, manager.history().size())));
    }

    private void refreshStats() {
        CacheStatistics s = manager.statistics();
        capacityCard.setValue(String.valueOf(manager.capacity()));
        sizeCard.setValue(String.valueOf(manager.size()));
        utilizationCard.setValue(String.format("%.0f%%", manager.utilization() * 100));
        hitsCard.setValue(String.valueOf(s.getHits()));
        missesCard.setValue(String.valueOf(s.getMisses()));
        ratioCard.setValue(String.format("%.1f%%", s.getHitRatio() * 100));
        evictionsCard.setValue(String.valueOf(s.getEvictions()));
    }

    private void setStatus(String text, Color color) {
        status.setForeground(color);
        status.setText(text);
    }

    // ------------------------------------------------------------------ small components

    /** One KPI tile on the dashboard. */
    private static final class StatCard extends JPanel {
        private final JLabel value = new JLabel("-", JLabel.LEFT);

        StatCard(String title, Color accent) {
            super(new BorderLayout(0, 4));
            setBackground(Theme.CARD);
            setBorder(Theme.card(12));
            JLabel t = new JLabel(title.toUpperCase());
            t.setFont(Theme.BOLD.deriveFont(10.5f));
            t.setForeground(Theme.MUTED);
            value.setFont(Theme.BOLD.deriveFont(26f));
            value.setForeground(accent);
            add(t, BorderLayout.NORTH);
            add(value, BorderLayout.CENTER);
        }

        void setValue(String text) {
            value.setText(text);
        }
    }
}
