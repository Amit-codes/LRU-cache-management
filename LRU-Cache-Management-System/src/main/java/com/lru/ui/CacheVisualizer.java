package com.lru.ui;

import com.lru.cache.CacheEntry;
import com.lru.model.CacheOperation;
import com.lru.model.OperationType;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Draws the cache as a row of cards ordered MRU (left) to LRU (right).
 * <ul>
 *   <li>Each card eases toward its target slot, so reordering is animated.</li>
 *   <li>The accessed card is lifted, jumps to the front and glows gold.</li>
 *   <li>Evicted cards turn red and sink away; removed cards float up and fade.</li>
 *   <li>Empty slots are drawn dashed so the capacity is always visible.</li>
 * </ul>
 */
public class CacheVisualizer extends JPanel {

    private static final int CARD_W = 112;
    private static final int CARD_H = 92;
    private static final int GAP = 30;
    private static final int PAD = 28;
    private static final int TOP = 50;
    private static final int MAX_SLOTS = 40;

    private static final class Card {
        String key;
        String value;
        double x, y, tx, ty;
        float alpha;
        float targetAlpha = 1f;
        boolean evicted;
    }

    private final List<Card> cards = new ArrayList<>();
    private final List<Card> ghosts = new ArrayList<>();
    private int capacity = 1;
    private String highlightKey;
    private float highlight;
    private String notice;
    private Color noticeColor = Theme.DANGER;
    private int noticeTicks;
    private final Timer timer = new Timer(16, e -> tick());

    public CacheVisualizer() {
        setBackground(Theme.CARD);
        setOpaque(true);
    }

    // ------------------------------------------------------------------ public API

    /** Shows a state without animation hints (used for initial display). */
    public void showState(List<CacheEntry<String, String>> entries, int capacity) {
        update(entries, capacity, null, List.of(), null, Theme.DANGER);
    }

    /** Shows the result of an operation: highlights the accessed key and animates evictions. */
    public void showOperation(List<CacheEntry<String, String>> entries, int capacity, CacheOperation op) {
        boolean touched = op.success()
                && (op.type() == OperationType.PUT || op.type() == OperationType.GET || op.type() == OperationType.PEEK);
        String note = null;
        Color color = Theme.DANGER;
        if (op.causedEviction()) {
            StringBuilder sb = new StringBuilder("Evicted key ");
            for (int i = 0; i < op.evicted().size(); i++) {
                sb.append(i > 0 ? ", " : "").append(op.evicted().get(i).key());
            }
            note = sb.append(" (least recently used)").toString();
        } else if (op.type() == OperationType.GET && !op.success()) {
            note = "Cache miss for key " + op.key();
            color = Theme.SLATE;
        }
        update(entries, capacity, touched ? op.key() : null, op.evicted(), note, color);
    }

    /** Clears everything instantly (used when a simulation restarts). */
    public void reset(int capacity) {
        cards.clear();
        ghosts.clear();
        this.capacity = capacity;
        highlightKey = null;
        highlight = 0;
        notice = null;
        noticeTicks = 0;
        revalidate();
        repaint();
    }

    // ------------------------------------------------------------------ state transitions

    private void update(List<CacheEntry<String, String>> entries, int cap, String hlKey,
                        List<CacheEntry<String, String>> evicted, String note, Color noteColor) {
        this.capacity = cap;
        Map<String, Card> old = new HashMap<>();
        for (Card c : cards) {
            old.put(c.key, c);
        }
        Set<String> evictedKeys = new HashSet<>();
        for (CacheEntry<String, String> e : evicted) {
            evictedKeys.add(e.key());
        }

        List<Card> next = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            CacheEntry<String, String> e = entries.get(i);
            Card c = old.remove(e.key());
            boolean isNew = c == null;
            if (isNew) {                       // new cards drop in from above the front slot
                c = new Card();
                c.key = e.key();
                c.x = slotX(0) - 30;
                c.y = TOP - 40;
                c.alpha = 0f;
            } else if (e.key().equals(hlKey)) {  // an accessed card lifts, then travels to the front
                c.y = TOP - 28;
            }
            c.value = e.value();
            c.tx = slotX(i);
            c.ty = TOP;
            c.targetAlpha = 1f;
            next.add(c);
        }

        for (Card gone : old.values()) {       // cards that left: evicted sink, removed float up
            gone.evicted = evictedKeys.contains(gone.key);
            gone.tx = gone.x;
            gone.ty = gone.evicted ? TOP + 70 : TOP - 44;
            gone.targetAlpha = 0f;
            ghosts.add(gone);
        }

        cards.clear();
        cards.addAll(next);
        highlightKey = hlKey;
        highlight = hlKey == null ? 0f : 1f;
        if (note != null) {
            notice = note;
            noticeColor = noteColor;
            noticeTicks = 220;
        }
        revalidate();
        repaint();
        timer.start();
    }

    private void tick() {
        boolean active = false;
        for (Card c : cards) {
            active |= ease(c);
        }
        for (Iterator<Card> it = ghosts.iterator(); it.hasNext(); ) {
            Card g = it.next();
            ease(g);
            if (g.alpha <= 0.001f) {
                it.remove();
            } else {
                active = true;
            }
        }
        if (highlight > 0f) {
            highlight = Math.max(0f, highlight - 0.012f);
            active = true;
        }
        if (noticeTicks > 0) {
            noticeTicks--;
            active = true;
            if (noticeTicks == 0) {
                notice = null;
            }
        }
        repaint();
        if (!active) {
            timer.stop();
        }
    }

    private boolean ease(Card c) {
        double dx = c.tx - c.x;
        double dy = c.ty - c.y;
        float da = c.targetAlpha - c.alpha;
        c.x += dx * 0.2;
        c.y += dy * 0.2;
        c.alpha += da * 0.2f;
        if (Math.abs(dx) < 0.4) c.x = c.tx;
        if (Math.abs(dy) < 0.4) c.y = c.ty;
        if (Math.abs(da) < 0.02f) c.alpha = c.targetAlpha;
        return c.x != c.tx || c.y != c.ty || c.alpha != c.targetAlpha;
    }

    // ------------------------------------------------------------------ layout

    private static int slotX(int index) {
        return PAD + index * (CARD_W + GAP);
    }

    private int slotCount() {
        return Math.max(1, Math.max(Math.min(capacity, MAX_SLOTS), cards.size()));
    }

    @Override
    public Dimension getPreferredSize() {
        int slots = slotCount();
        return new Dimension(PAD * 2 + slots * CARD_W + (slots - 1) * GAP, TOP + CARD_H + 78);
    }

    // ------------------------------------------------------------------ painting

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        drawSlots(g);
        drawLinks(g);
        if (cards.isEmpty() && ghosts.isEmpty()) {
            g.setFont(Theme.BASE.deriveFont(Font.ITALIC, 14f));
            g.setColor(Theme.MUTED);
            g.drawString("Cache is empty - PUT something to begin", slotX(0) + 6, TOP + CARD_H / 2 + 5);
        }
        for (Card c : ghosts) {
            drawCard(g, c, -1, 0);
        }
        Card lifted = null;
        int liftedIndex = -1;
        for (int i = 0; i < cards.size(); i++) {
            Card c = cards.get(i);
            if (c.key.equals(highlightKey) && highlight > 0f) {
                lifted = c;
                liftedIndex = i;
            } else {
                drawCard(g, c, i, cards.size());
            }
        }
        if (lifted != null) {
            drawCard(g, lifted, liftedIndex, cards.size());   // draw last so it sits on top
        }
        drawNotice(g);
        g.dispose();
    }

    private void drawSlots(Graphics2D g) {
        g.setColor(new Color(200, 205, 215));
        g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{6f, 5f}, 0f));
        for (int i = 0; i < slotCount(); i++) {
            g.draw(new RoundRectangle2D.Double(slotX(i), TOP, CARD_W, CARD_H, 18, 18));
        }
        g.setStroke(new BasicStroke(1f));
    }

    /** Doubly-linked-list arrows between neighbouring cards (both directions). */
    private void drawLinks(Graphics2D g) {
        g.setColor(new Color(160, 168, 184));
        g.setStroke(new BasicStroke(1.5f));
        int midY = TOP + CARD_H / 2;
        for (int i = 0; i + 1 < cards.size(); i++) {
            int x1 = (int) (cards.get(i).x + CARD_W) + 3;
            int x2 = (int) cards.get(i + 1).x - 3;
            if (x2 - x1 < 12) {
                continue;
            }
            g.drawLine(x1, midY, x2, midY);
            g.drawLine(x1, midY, x1 + 5, midY - 4);
            g.drawLine(x1, midY, x1 + 5, midY + 4);
            g.drawLine(x2, midY, x2 - 5, midY - 4);
            g.drawLine(x2, midY, x2 - 5, midY + 4);
        }
        g.setStroke(new BasicStroke(1f));
    }

    private void drawCard(Graphics2D g, Card c, int index, int count) {
        boolean ghost = index < 0;
        boolean isMru = index == 0;
        boolean isLru = index == count - 1 && count > 0;
        int x = (int) Math.round(c.x);
        int y = (int) Math.round(c.y);

        java.awt.Composite saved = g.getComposite();
        g.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, Math.min(1f, c.alpha))));

        g.setColor(new Color(0, 0, 0, 26));
        g.fill(new RoundRectangle2D.Double(x + 2, y + 5, CARD_W, CARD_H, 18, 18));

        Color fill = Color.WHITE;
        Color border = Theme.BORDER;
        if (ghost) {
            fill = c.evicted ? Theme.DANGER_SOFT : new Color(238, 240, 244);
            border = c.evicted ? Theme.DANGER : Theme.MUTED;
        } else if (isMru) {
            fill = Theme.SUCCESS_SOFT;
            border = Theme.SUCCESS;
        } else if (isLru) {
            fill = Theme.WARN_SOFT;
            border = Theme.WARN;
        }
        RoundRectangle2D.Double shape = new RoundRectangle2D.Double(x, y, CARD_W, CARD_H, 18, 18);
        g.setColor(fill);
        g.fill(shape);

        if (!ghost && c.key.equals(highlightKey) && highlight > 0f) {   // gold glow that fades out
            g.setColor(new Color(Theme.HIGHLIGHT.getRed(), Theme.HIGHLIGHT.getGreen(), Theme.HIGHLIGHT.getBlue(),
                    (int) (150 * highlight)));
            g.setStroke(new BasicStroke(4f + 6f * highlight));
            g.draw(shape);
        }
        g.setColor(border);
        g.setStroke(new BasicStroke(2f));
        g.draw(shape);
        g.setStroke(new BasicStroke(1f));

        g.setFont(Theme.BOLD.deriveFont(10f));
        g.setColor(ghost && c.evicted ? Theme.DANGER : Theme.MUTED);
        g.drawString(ghost && c.evicted ? "EVICTED" : "KEY", x + 12, y + 20);

        g.setFont(Theme.BOLD.deriveFont(24f));
        g.setColor(Theme.TEXT);
        g.drawString(fit(String.valueOf(c.key), g.getFontMetrics(), CARD_W - 24), x + 12, y + 48);

        g.setColor(new Color(0, 0, 0, 25));
        g.drawLine(x + 12, y + 60, x + CARD_W - 12, y + 60);

        g.setFont(Theme.BASE.deriveFont(14f));
        g.setColor(Theme.SLATE);
        g.drawString(fit(String.valueOf(c.value), g.getFontMetrics(), CARD_W - 24), x + 12, y + 80);

        if (!ghost && c.alpha > 0.5f) {
            int badgeY = (int) Math.round(c.y) + CARD_H + 10;
            if (isMru && isLru) {
                badge(g, "MRU", x + 4, badgeY, Theme.SUCCESS);
                badge(g, "LRU", x + 4 + 52, badgeY, Theme.WARN);
            } else if (isMru) {
                badge(g, "MRU", x + (CARD_W - 46) / 2, badgeY, Theme.SUCCESS);
            } else if (isLru) {
                badge(g, "LRU - next out", x + (CARD_W - 100) / 2, badgeY, Theme.WARN);
            }
        }
        g.setComposite(saved);
    }

    private void badge(Graphics2D g, String text, int x, int y, Color color) {
        g.setFont(Theme.BOLD.deriveFont(11f));
        int w = g.getFontMetrics().stringWidth(text) + 16;
        g.setColor(color);
        g.fillRoundRect(x, y, w, 20, 20, 20);
        g.setColor(Color.WHITE);
        g.drawString(text, x + 8, y + 14);
    }

    private void drawNotice(Graphics2D g) {
        if (notice == null) {
            return;
        }
        g.setFont(Theme.BOLD.deriveFont(13f));
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(notice) + 28;
        Rectangle view = getVisibleRect();
        int x = view.x + view.width - w - 14;
        int y = 10;
        float fade = Math.min(1f, noticeTicks / 40f);
        g.setComposite(AlphaComposite.SrcOver.derive(fade));
        g.setColor(noticeColor);
        g.fillRoundRect(x, y, w, 28, 14, 14);
        g.setColor(Color.WHITE);
        g.drawString(notice, x + 14, y + 19);
        g.setComposite(AlphaComposite.SrcOver);
    }

    private static String fit(String text, FontMetrics fm, int maxWidth) {
        if (fm.stringWidth(text) <= maxWidth) {
            return text;
        }
        String s = text;
        while (s.length() > 1 && fm.stringWidth(s + "...") > maxWidth) {
            s = s.substring(0, s.length() - 1);
        }
        return s + "...";
    }
}
