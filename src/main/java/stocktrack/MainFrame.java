package stocktrack;

import java.awt.*;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.table.*;

/** Dashboard window. Layout and look are done; extend the TODO parts. */
public class MainFrame extends JFrame {
    private final InventoryManager inv;
    private final Auth.User user;
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"SKU", "Name", "Qty", "Price", "Expiry", "Status", "ABC"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private Theme.NavButton ordersBtn;
    private final java.util.List<Tab> tabList = new java.util.ArrayList<>();
    private final DefaultListModel<String> alertModel = new DefaultListModel<>();
    private final JTextField search = new JTextField(8);
    private final JLabel status = Theme.label("Ready. Select a product to stock in or out.", Theme.MUTED, Theme.BASE);
    private final JLabel lblProducts = big(Theme.TEXT), lblValue = big(Theme.TEXT),
            lblLow = big(Theme.DANGER), lblExpiring = big(Theme.WARN);

    public MainFrame(InventoryManager inv, Auth.User user) {
        super("StockTrack v" + Version.NAME + " - Inventory and Supply Chain");
        this.inv = inv;
        this.user = user;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1220, 680);
        setLocationRelativeTo(null);
        getContentPane().setBackground(Theme.BG);
        setLayout(new BorderLayout());
        add(header(), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setBackground(Theme.BG);
        body.setBorder(BorderFactory.createEmptyBorder(14, 16, 12, 16));

        JPanel top = new JPanel(new BorderLayout(0, 12));
        top.setOpaque(false);
        top.add(cards(), BorderLayout.NORTH);
        top.add(toolbar(), BorderLayout.SOUTH);
        body.add(top, BorderLayout.NORTH);

        setupTable();
        JScrollPane tablePane = new JScrollPane(table);
        tablePane.setBorder(BorderFactory.createLineBorder(Theme.LINE));
        tablePane.getViewport().setBackground(Theme.CARD);
        body.add(tablePane, BorderLayout.CENTER);

        JList<String> alertList = new JList<>(alertModel);
        alertList.setCellRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
                JLabel c = (JLabel) super.getListCellRendererComponent(l, v, i, false, false);
                String t = String.valueOf(v);
                c.setForeground(t.startsWith("Low") || t.startsWith("Expired") ? Theme.DANGER : t.startsWith("Expiring") ? Theme.WARN : Theme.MUTED);
                c.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
                return c;
            }
        });
        JScrollPane alertPane = new JScrollPane(alertList);
        alertPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Theme.LINE), "Alerts"));
        alertPane.setBackground(Theme.CARD);
        alertPane.setPreferredSize(new Dimension(100, 105));
        JPanel south = new JPanel(new BorderLayout(0, 6));
        south.setOpaque(false);
        south.add(alertPane, BorderLayout.CENTER);
        body.add(south, BorderLayout.SOUTH);
        boolean owner = user.role() == Auth.Role.OWNER;
        Runnable changed = this::refresh;
        java.util.function.Consumer<String> say = this::status;
        tabList.add(new OrderTab(inv, changed, say, owner));
        tabList.add(new SupplierTab(inv, changed, say, owner));
        tabList.add(new SalesTab(inv, changed, say));
        tabList.add(new ReportTab(inv, changed, say));
        tabList.add(new HistoryTab(inv, changed, say));
        String[] names = {"Inventory", "Orders", "Suppliers", "Sales", "Reports", "History"};
        CardLayout cards = new CardLayout();
        JPanel content = new JPanel(cards);
        content.setBackground(Theme.BG);
        content.add(body, names[0]);
        for (int i = 1; i < names.length; i++) content.add(tabList.get(i - 1), names[i]);
        add(sidebar(names, cards, content), BorderLayout.WEST);
        add(content, BorderLayout.CENTER);
        status.setBorder(BorderFactory.createEmptyBorder(6, 16, 8, 16));
        add(status, BorderLayout.SOUTH);

        search.setBorder(Theme.fieldBorder());
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { refreshInventory(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { refreshInventory(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { refreshInventory(); }
        });
        refresh();
    }

    // ---------- building the screen ----------
    private JLabel big(Color c) { return Theme.label("0", c, Theme.bold(26f)); }

    private JPanel sidebar(String[] names, CardLayout cards, JPanel content) {
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBackground(Theme.PRIMARY_DARK);
        side.setBorder(BorderFactory.createEmptyBorder(16, 10, 16, 10));
        side.setPreferredSize(new Dimension(190, 0));
        JLabel menu = Theme.label("MENU", new Color(0x9FE1CB), Theme.bold(11f));
        menu.setBorder(BorderFactory.createEmptyBorder(0, 18, 8, 0));
        menu.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(menu);
        ButtonGroup group = new ButtonGroup();
        for (String n : names) {
            Theme.NavButton b = new Theme.NavButton(n);
            b.addActionListener(e -> cards.show(content, n));
            group.add(b);
            if (n.equals("Orders")) ordersBtn = b;
            side.add(b);
            side.add(Box.createVerticalStrut(4));
            if (n.equals(names[0])) b.setSelected(true);
        }
        return side;
    }

    private JPanel header() {
        JPanel h = new JPanel(new BorderLayout());
        h.setBackground(Theme.PRIMARY);
        h.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        h.add(Theme.label("StockTrack", Color.WHITE, Theme.bold(20f)), BorderLayout.WEST);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);
        right.add(Theme.label("Hi, " + user.username() + " (" + user.roleLabel() + ")", Color.WHITE, Theme.BASE));
        Theme.FlatButton out = new Theme.FlatButton("Log out", Color.WHITE, Color.WHITE, true);
        out.addActionListener(e -> { dispose(); new LoginFrame(inv).setVisible(true); });
        right.add(out);
        h.add(right, BorderLayout.EAST);
        return h;
    }

    private JPanel cards() {
        JPanel p = new JPanel(new GridLayout(1, 4, 12, 0));
        p.setOpaque(false);
        p.add(card("Products", lblProducts, Theme.PRIMARY));
        p.add(card("Stock value", lblValue, Theme.BLUE));
        p.add(card("Low stock", lblLow, Theme.DANGER));
        p.add(card("Expiring in 30 days", lblExpiring, Theme.WARN));
        return p;
    }

    private JPanel card(String title, JLabel value, Color accent) {
        JPanel c = new JPanel(new BorderLayout(0, 2));
        c.setBackground(Theme.CARD);
        c.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.LINE),
                BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 4, 0, 0, accent),
                        BorderFactory.createEmptyBorder(10, 12, 10, 12))));
        c.add(Theme.label(title, Theme.MUTED, Theme.BASE), BorderLayout.NORTH);
        c.add(value, BorderLayout.CENTER);
        return c;
    }

    private JPanel toolbar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        left.add(Theme.label("Search", Theme.MUTED, Theme.BASE));
        left.add(search);
        bar.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(action("Stock in", Theme.PRIMARY, false, e -> move(true)));
        right.add(action("Stock out", Theme.BLUE, false, e -> move(false)));
        right.add(action("Undo", Theme.MUTED, true, e -> {
            status(inv.undo() ? "Last transaction undone." : "Nothing to undo.");
            refresh();
        }));
        if (user.role() == Auth.Role.OWNER) {
            right.add(action("Add product", Theme.PRIMARY, true, e -> addProduct()));
            right.add(action("Edit", Theme.PRIMARY, true, e -> editProduct()));
            right.add(action("Delete", Theme.DANGER, true, e -> deleteSelected()));
            right.add(action("Discard expired", Theme.WARN, true, e -> discardExpired()));
            right.add(action("Samples", Theme.MUTED, true, e -> sampleData()));
        }
        // TODO: add buttons for Suppliers, Reports, Save (team)
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private Theme.FlatButton action(String text, Color color, boolean outline, java.awt.event.ActionListener a) {
        Theme.FlatButton b = new Theme.FlatButton(text, color, outline ? color : Color.WHITE, outline);
        b.addActionListener(a);
        return b;
    }

    private void setupTable() {
        table.setRowHeight(30);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(new Color(0xCDEBDD));
        table.setSelectionForeground(Theme.TEXT);
        table.setFillsViewportHeight(true);
        int[] widths = {90, 190, 70, 90, 110, 100, 60};
        for (int i = 0; i < widths.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, s, f, r, c);
                l.setOpaque(true);
                l.setBackground(Theme.PRIMARY_LIGHT);
                l.setForeground(Theme.PRIMARY_DARK);
                l.setFont(Theme.bold(12f));
                l.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
                l.setHorizontalAlignment(c == 2 || c == 3 ? SwingConstants.RIGHT : SwingConstants.LEFT);
                return l;
            }
        });
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean f, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, sel, f, r, c);
                l.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                l.setHorizontalAlignment(c == 2 || c == 3 ? SwingConstants.RIGHT : SwingConstants.LEFT);
                if (!sel) l.setBackground(r % 2 == 0 ? Theme.CARD : new Color(0xF9FAFB));
                l.setForeground(Theme.TEXT);
                l.setFont(Theme.BASE);
                String st = String.valueOf(model.getValueAt(r, 5));
                if (c == 5) {
                    l.setForeground(st.equals("Low stock") || st.equals("Expired") ? Theme.DANGER : st.equals("Expiring") ? Theme.WARN : Theme.OK);
                    l.setFont(Theme.bold(12f));
                } else if (c == 2 && st.equals("Low stock")) {
                    l.setForeground(Theme.DANGER);
                    l.setFont(Theme.bold(13f));
                }
                return l;
            }
        });
    }

    // ---------- data to screen ----------
    private String statusOf(Product p) {
        if (p.getExpiry() != null && p.getExpiry().isBefore(LocalDate.now())) return "Expired";
        if (p.getQty() <= p.getReorderLevel()) return "Low stock";
        if (p.getExpiry() != null && !p.getExpiry().isAfter(LocalDate.now().plusDays(30))) return "Expiring";
        return "OK";
    }

    private void refresh() {
        refreshInventory();
        for (Tab t : tabList) t.refresh();
        if (ordersBtn != null) {
            int n = inv.needsOrdering().size();
            ordersBtn.setText(n > 0 ? "Orders (" + n + ")" : "Orders");
        }
        try { DataStore.save(inv); }
        catch (java.io.IOException ex) { status("Could not save to the database: " + ex.getMessage()); }
    }

    private void refreshInventory() {
        String q = search.getText().trim().toLowerCase();
        Map<String, Character> abc = inv.abcAnalysis();
        List<Product> sorted = InventoryManager.mergeSort(inv.all(),
                Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER));
        model.setRowCount(0);
        double total = 0;
        for (Product p : sorted) {
            total += p.getValue();
            if (!q.isEmpty() && !StringSearch.contains(p.getSku().toLowerCase(), q) && !StringSearch.contains(p.getName().toLowerCase(), q)) continue; // KMP search
            model.addRow(new Object[]{p.getSku(), p.getName(), p.getQty(), String.format("%.2f", p.getPrice()),
                    p.getExpiry() == null ? "-" : p.getExpiry(), statusOf(p), abc.get(p.getSku())});
        }
        List<Product> low = inv.lowStockAlerts();
        List<Product> exp = inv.expiringSoon(30);
        alertModel.clear();
        for (Product p : low) alertModel.addElement("Low stock: " + p.getName() + " has " + p.getQty() + " left (reorder at " + p.getReorderLevel() + ")");
        for (Product p : exp) alertModel.addElement((p.getExpiry().isBefore(LocalDate.now()) ? "Expired: " : "Expiring: ") + p.getName() + " on " + p.getExpiry());
        if (alertModel.isEmpty()) alertModel.addElement("No alerts. Everything looks good.");
        lblProducts.setText(String.valueOf(inv.all().size()));
        lblValue.setText(String.format("P%,.0f", total));
        lblLow.setText(String.valueOf(low.size()));
        lblExpiring.setText(String.valueOf(exp.size()));
    }

    // ---------- actions ----------
    private Product selected() {
        int r = table.getSelectedRow();
        if (r < 0) { msg("Please select a product in the table first."); return null; }
        return inv.find((String) model.getValueAt(r, 0));
    }

    private void status(String text) { status.setText(text); }

    private void move(boolean in) {
        Product p = selected();
        if (p == null) return;
        try {
            if (in) {
                JTextField q = new JTextField("10"), e = new JTextField(LocalDate.now().plusMonths(6).toString());
                JPanel f = new JPanel(new GridLayout(0, 2, 8, 8));
                f.add(new JLabel("Units received")); f.add(q);
                f.add(new JLabel("Expiry (yyyy-mm-dd)")); f.add(e);
                if (JOptionPane.showConfirmDialog(this, f, "Stock in: " + p.getName(), JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
                int n = Integer.parseInt(q.getText().trim());
                if (inv.stockIn(p.getSku(), n, LocalDate.parse(e.getText().trim())))
                    status("Added " + n + " to " + p.getName() + ". Now " + p.getQty() + " in stock.");
                else msg("Enter a number greater than 0.");
            } else {
                String s = JOptionPane.showInputDialog(this, "How many units of " + p.getName() + " were sold?\nCurrent stock: "
                        + p.getQty() + " (the earliest-expiring units go first)", "Stock out", JOptionPane.QUESTION_MESSAGE);
                if (s == null) return;
                int n = Integer.parseInt(s.trim());
                if (inv.stockOut(p.getSku(), n)) status("Removed " + n + " from " + p.getName() + ". Now " + p.getQty() + " in stock.");
                else msg("Enter a number from 1 to " + p.getQty() + ".");
            }
        } catch (RuntimeException ex) { msg("Please check your input: a whole number and a date like 2027-01-31."); }
        refresh();
    }

    private void deleteSelected() {
        Product p = selected();
        if (p != null && inv.hasOpenOrders(p.getSku())) {
            msg("This product has a pending or approved purchase order. Cancel or receive the order first.");
            return;
        }
        if (p != null && JOptionPane.showConfirmDialog(this, "Delete " + p.getName() + " (" + p.getSku() + ")?\nThis cannot be undone.",
                "Delete product", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
            inv.remove(p.getSku());
            status("Deleted " + p.getName() + ".");
            refresh();
        }
    }

    private void addProduct() {
        JTextField sku = new JTextField(), name = new JTextField(), qty = new JTextField("0"),
                price = new JTextField(), exp = new JTextField(LocalDate.now().plusMonths(6).toString()),
                level = new JTextField("10");
        JPanel f = new JPanel(new GridLayout(0, 2, 8, 8));
        f.add(new JLabel("SKU (example: SKU-006)")); f.add(sku);
        f.add(new JLabel("Product name")); f.add(name);
        f.add(new JLabel("Starting quantity")); f.add(qty);
        f.add(new JLabel("Price (P)")); f.add(price);
        f.add(new JLabel("Expiry (yyyy-mm-dd)")); f.add(exp);
        f.add(new JLabel("Reorder level")); f.add(level);
        if (JOptionPane.showConfirmDialog(this, f, "Add product", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        try {
            Product p = new Product(sku.getText().trim(), name.getText().trim(), Integer.parseInt(qty.getText().trim()),
                    Double.parseDouble(price.getText().trim()), LocalDate.parse(exp.getText().trim()),
                    Integer.parseInt(level.getText().trim()), 1);
            if (p.getSku().isEmpty() || p.getName().isEmpty()) msg("SKU and name are required.");
            else if (!inv.addProduct(p)) msg("That SKU already exists. Use a different one.");
            else status("Added " + p.getName() + ".");
        } catch (RuntimeException ex) { msg("Please check your input: numbers for quantity, price, and level, and a date like 2027-01-31."); }
        refresh();
    }

    private void editProduct() {
        Product p = selected();
        if (p == null) return;
        JTextField name = new JTextField(p.getName()), price = new JTextField(String.valueOf(p.getPrice())),
                level = new JTextField(String.valueOf(p.getReorderLevel())), demand = new JTextField(String.valueOf(p.getDailyDemand()));
        JPanel f = new JPanel(new GridLayout(0, 2, 8, 8));
        f.add(new JLabel("Product name")); f.add(name);
        f.add(new JLabel("Price (P)")); f.add(price);
        f.add(new JLabel("Reorder level")); f.add(level);
        f.add(new JLabel("Average units sold per day")); f.add(demand);
        if (JOptionPane.showConfirmDialog(this, f, "Edit " + p.getSku(), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        try {
            if (inv.editProduct(p.getSku(), name.getText(), Double.parseDouble(price.getText().trim()),
                    Integer.parseInt(level.getText().trim()), Double.parseDouble(demand.getText().trim()))) status("Updated " + name.getText().trim() + ".");
            else msg("Name is required, price must be above 0, and the other numbers cannot be negative.");
        } catch (NumberFormatException ex) { msg("Please enter numbers only for price, level, and units per day."); }
        refresh();
    }

    /** Loads 1,000 fake products (with sales history) to test many records, or removes them again. */
    private void sampleData() {
        String[] options = {"Load 1,000 products", "Remove samples", "Cancel"};
        int c = JOptionPane.showOptionDialog(this, "Add 1,000 fake products with sales history to see how the system handles lots of data,\nor remove them again. Sample products have SKUs that start with BULK-.",
                "Sample data", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        long start = System.nanoTime();
        if (c == 0) {
            int n = SampleData.loadBulk(inv, 1000);
            refresh();
            status("Loaded " + n + " sample products. Everything was updated and saved in " + (System.nanoTime() - start) / 1_000_000 + " ms.");
        } else if (c == 1) {
            int n = inv.removeSamples();
            refresh();
            status("Removed " + n + " sample products.");
        }
    }

    private void discardExpired() {
        int n = inv.discardExpired();
        if (n == 0) msg("No expired stock to discard.");
        else status("Discarded " + n + " expired units. They are not counted as sales.");
        refresh();
    }

    private void msg(String m) { JOptionPane.showMessageDialog(this, m, "StockTrack", JOptionPane.INFORMATION_MESSAGE); }
}
