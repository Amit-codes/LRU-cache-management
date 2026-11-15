package com.lru.ui;

import com.lru.cache.CacheManager;
import com.lru.exception.CacheException;
import com.lru.model.CacheCommand;
import com.lru.model.CacheOperation;
import com.lru.simulation.CommandParser;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;

/**
 * Step-by-step simulation. The user writes (or loads) a script of cache commands, then steps through it
 * or plays it automatically while the visualizer animates every change. Runs on its own cache instance,
 * so it never disturbs the live dashboard.
 */
public class SimulationPanel extends JPanel {

    private static final String SAMPLE = """
            # Capacity 3. When PUT(4, 400) arrives, key 2 is the least recently used and is evicted.
            PUT(1, 100)
            PUT(2, 200)
            PUT(3, 300)
            GET(1)
            PUT(4, 400)
            """;

    private final JTextArea script = new JTextArea(SAMPLE, 12, 24);
    private final JSpinner capacitySpinner = new JSpinner(new SpinnerNumberModel(3, 1, 50, 1));
    private final CacheVisualizer visualizer = new CacheVisualizer();
    private final DefaultListModel<CacheOperation> logModel = new DefaultListModel<>();
    private final JList<CacheOperation> logList = new JList<>(logModel);
    private final JLabel explanation = new JLabel(" ");
    private final StyledButton playButton = new StyledButton("Play", Theme.SUCCESS);
    private final Timer autoPlay = new Timer(1400, e -> {
        if (!step()) {
            stopPlay();
        }
    });

    private CacheManager manager;
    private List<CacheCommand> program;
    private int pc;
    private boolean dirty = true;

    public SimulationPanel() {
        super(new BorderLayout(0, 12));
        setBackground(Theme.BG);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        add(buildToolbar(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);

        script.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { dirty = true; }
            public void removeUpdate(DocumentEvent e) { dirty = true; }
            public void changedUpdate(DocumentEvent e) { dirty = true; }
        });
        capacitySpinner.addChangeListener(e -> reset());
        autoPlay.setInitialDelay(200);
        reset();
    }

    private JPanel buildToolbar() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        row.setBackground(Theme.CARD);
        row.setBorder(Theme.card(6));

        JLabel cap = new JLabel("Capacity");
        cap.setFont(Theme.BOLD);
        cap.setForeground(Theme.MUTED);
        row.add(cap);
        row.add(capacitySpinner);

        StyledButton step = new StyledButton("Step", Theme.PRIMARY);
        step.addActionListener(e -> {
            stopPlay();
            step();
        });
        playButton.addActionListener(e -> togglePlay());
        StyledButton resetButton = new StyledButton("Reset", Theme.SLATE);
        resetButton.addActionListener(e -> reset());
        StyledButton sample = new StyledButton("Load sample (capacity 3)", Theme.TEAL);
        sample.addActionListener(e -> {
            script.setText(SAMPLE);
            capacitySpinner.setValue(3);
            reset();
        });
        row.add(step);
        row.add(playButton);
        row.add(resetButton);
        row.add(sample);
        return row;
    }

    private JSplitPane buildBody() {
        script.setFont(Theme.MONO.deriveFont(14f));
        script.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        script.setLineWrap(true);
        script.setWrapStyleWord(true);
        JScrollPane scriptScroll = new JScrollPane(script);
        scriptScroll.setBorder(Theme.titled("Script (one command per line)"));
        scriptScroll.setPreferredSize(new Dimension(330, 100));

        explanation.setFont(Theme.BASE.deriveFont(15f));
        explanation.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        explanation.setOpaque(true);
        explanation.setBackground(Theme.CARD);

        JScrollPane vizScroll = new JScrollPane(visualizer,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        vizScroll.setBorder(Theme.titled("Cache after each step   (MRU \u2192 LRU)"));

        logList.setCellRenderer(new OperationRenderer());
        JScrollPane logScroll = new JScrollPane(logList);
        logScroll.setBorder(Theme.titled("Executed steps"));
        logScroll.setPreferredSize(new Dimension(100, 170));

        JPanel right = new JPanel(new BorderLayout(0, 10));
        right.setOpaque(false);
        right.add(explanation, BorderLayout.NORTH);
        right.add(vizScroll, BorderLayout.CENTER);
        right.add(logScroll, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scriptScroll, right);
        split.setOpaque(false);
        split.setBorder(null);
        split.setDividerSize(8);
        split.setResizeWeight(0.0);
        return split;
    }

    // ------------------------------------------------------------------ behaviour

    /** Parses the script and starts a fresh cache. Returns false (and shows why) if the script is invalid. */
    private boolean prepare() {
        try {
            List<CacheCommand> parsed = CommandParser.parseScript(script.getText());
            if (parsed.isEmpty()) {
                throw new CacheException("The script has no commands");
            }
            int capacity = (Integer) capacitySpinner.getValue();
            program = parsed;
            manager = new CacheManager(capacity);
            pc = 0;
            dirty = false;
            logModel.clear();
            visualizer.reset(capacity);
            return true;
        } catch (CacheException ex) {
            program = null;
            showMessage("Script error - " + ex.getMessage(), Theme.DANGER);
            return false;
        }
    }

    private void reset() {
        stopPlay();
        if (prepare()) {
            showMessage("Ready: " + program.size() + " commands loaded. Press Step or Play.", Theme.MUTED);
        }
    }

    /** Executes the next command. Returns true while more commands remain. */
    private boolean step() {
        if (program == null || dirty) {
            if (!prepare()) {
                return false;
            }
        }
        if (pc >= program.size()) {
            showMessage("Simulation finished. Press Reset to run it again.", Theme.MUTED);
            return false;
        }
        CacheCommand command = program.get(pc++);
        try {
            CacheOperation op = manager.execute(command);
            visualizer.showOperation(manager.snapshot(), manager.capacity(), op);
            logModel.addElement(op);
            logList.ensureIndexIsVisible(logModel.size() - 1);
            Color color = op.causedEviction() ? Theme.WARN : op.success() ? Theme.SUCCESS : Theme.DANGER;
            showMessage("Step " + pc + " of " + program.size() + ":  " + op.signature() + "  ->  " + op.message(), color);
        } catch (CacheException ex) {
            showMessage("Step " + pc + " failed: " + ex.getMessage(), Theme.DANGER);
            return false;
        }
        return pc < program.size();
    }

    private void togglePlay() {
        if (autoPlay.isRunning()) {
            stopPlay();
            return;
        }
        if (program == null || dirty || pc >= program.size()) {
            if (!prepare()) {
                return;
            }
        }
        playButton.setText("Pause");
        autoPlay.start();
    }

    private void stopPlay() {
        autoPlay.stop();
        playButton.setText("Play");
    }

    private void showMessage(String text, Color color) {
        explanation.setForeground(color);
        explanation.setText("<html>" + text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;") + "</html>");
    }
}
