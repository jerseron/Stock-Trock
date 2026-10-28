package stocktrack;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.*;

/** Colors, fonts and small UI helpers so every screen looks the same. */
public final class Theme {
    public static final Color PRIMARY = new Color(0x0F6E56), PRIMARY_DARK = new Color(0x085041),
            PRIMARY_LIGHT = new Color(0xE1F5EE), BG = new Color(0xF4F6F5), CARD = Color.WHITE,
            TEXT = new Color(0x1F2937), MUTED = new Color(0x6B7280), DANGER = new Color(0xB3261E),
            WARN = new Color(0xB45309), OK = new Color(0x2E7D32), BLUE = new Color(0x2563EB),
            LINE = new Color(0xD9DEDC);
    public static final Font BASE = new Font("SansSerif", Font.PLAIN, 13);

    private Theme() { }

    public static Font bold(float size) { return BASE.deriveFont(Font.BOLD, size); }

    public static void install() {
        for (String k : new String[]{"Label.font", "TextField.font", "PasswordField.font", "Table.font",
                "List.font", "Button.font", "OptionPane.messageFont", "OptionPane.buttonFont"})
            UIManager.put(k, BASE);
        UIManager.put("OptionPane.background", CARD);
        UIManager.put("Panel.background", CARD);
    }

    public static Border fieldBorder() {
        return BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LINE),
                BorderFactory.createEmptyBorder(6, 8, 6, 8));
    }

    public static JLabel label(String text, Color color, Font font) {
        JLabel l = new JLabel(text);
        l.setForeground(color);
        l.setFont(font);
        return l;
    }

    public static DefaultTableModel model(String... cols) {
        return new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    /** Styled read-only table (green header, zebra rows). */
    public static JTable table(DefaultTableModel m) {
        JTable t = new JTable(m);
        t.setRowHeight(30);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setSelectionBackground(new Color(0xCDEBDD));
        t.setSelectionForeground(TEXT);
        t.setFillsViewportHeight(true);
        t.getTableHeader().setReorderingAllowed(false);
        t.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tb, Object v, boolean s, boolean f, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(tb, v, s, f, r, c);
                l.setOpaque(true);
                l.setBackground(PRIMARY_LIGHT);
                l.setForeground(PRIMARY_DARK);
                l.setFont(bold(12f));
                l.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
                return l;
            }
        });
        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tb, Object v, boolean sel, boolean f, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(tb, v, sel, f, r, c);
                l.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                if (!sel) l.setBackground(r % 2 == 0 ? CARD : new Color(0xF9FAFB));
                l.setForeground(TEXT);
                return l;
            }
        });
        return t;
    }

    public static JScrollPane scroll(JTable t) {
        JScrollPane s = new JScrollPane(t);
        s.setBorder(BorderFactory.createLineBorder(LINE));
        s.getViewport().setBackground(CARD);
        return s;
    }

    /** Button for the side menu. Highlights when selected. */
    public static class NavButton extends JToggleButton {
        public NavButton(String text) {
            super(text);
            setFont(bold(14f));
            setForeground(Color.WHITE);
            setHorizontalAlignment(SwingConstants.LEFT);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(11, 18, 11, 12));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (isSelected()) {
                g2.setColor(new Color(255, 255, 255, 45));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(new Color(0x5DCAA5));
                g2.fillRoundRect(0, 8, 4, getHeight() - 16, 4, 4);
            } else if (getModel().isRollover()) {
                g2.setColor(new Color(255, 255, 255, 22));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Flat rounded button that looks the same on every Java look and feel. */
    public static class FlatButton extends JButton {
        private final Color base;
        private final boolean outline;

        public FlatButton(String text, Color base, Color textColor, boolean outline) {
            super(text);
            this.base = base;
            this.outline = outline;
            setForeground(textColor);
            setFont(bold(13));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth() - 1, h = getHeight() - 1;
            boolean hot = getModel().isRollover() || getModel().isPressed();
            if (outline) {
                if (hot) { g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 35)); g2.fillRoundRect(0, 0, w, h, 8, 8); }
                g2.setColor(base);
                g2.drawRoundRect(0, 0, w, h, 8, 8);
            } else {
                g2.setColor(getModel().isPressed() ? base.darker().darker() : hot ? base.darker() : base);
                g2.fillRoundRect(0, 0, w + 1, h + 1, 8, 8);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
