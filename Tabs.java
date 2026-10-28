package stocktrack;

import java.awt.*;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/** Base class for the extra tabs. Each tab draws itself in refresh(). */
abstract class Tab extends JPanel {
    protected final InventoryManager inv;
    protected final Runnable changed;
    protected final Consumer<String> say;

    Tab(InventoryManager inv, Runnable changed, Consumer<String> say) {
        super(new BorderLayout(0, 10));
        this.inv = inv; this.changed = changed; this.say = say;
        setBackground(Theme.BG);
        setBorder(BorderFactory.createEmptyBorder(14, 16, 12, 16));
    }

    abstract void refresh();

    protected void info(String m) { JOptionPane.showMessageDialog(this, m, "StockTrack", JOptionPane.INFORMATION_MESSAGE); }

    protected JPanel bar(String hint, JComponent... buttons) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.add(Theme.label(hint, Theme.MUTED, Theme.BASE), BorderLayout.WEST);
        JPanel r = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        r.setOpaque(false);
        for (JComponent b : buttons) r.add(b);
        p.add(r, BorderLayout.EAST);
        return p;
    }

    protected Theme.FlatButton button(String text, Color c, boolean outline, Runnable action) {
        Theme.FlatButton b = new Theme.FlatButton(text, c, outline ? c : Color.WHITE, outline);
        b.addActionListener(e -> action.run());
        return b;
    }

    protected JPanel section(String title, JTable t) {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);
        p.add(Theme.label(title, Theme.TEXT, Theme.bold(14f)), BorderLayout.NORTH);
        p.add(Theme.scroll(t), BorderLayout.CENTER);
        return p;
    }

    protected String money(double v) { return String.format("%,.2f", v); }
}

/** Supplier list and price comparison. */
class SupplierTab extends Tab {
    private final DefaultTableModel sm = Theme.model("Supplier", "Lead time (days)", "Products priced");
    private final DefaultTableModel cm = Theme.model("SKU", "Product", "Best supplier", "Price", "Lead (days)", "All offers");

    SupplierTab(InventoryManager inv, Runnable changed, Consumer<String> say, boolean owner) {
        super(inv, changed, say);
        JPanel c = new JPanel(new GridLayout(2, 1, 0, 12));
        c.setOpaque(false);
        c.add(section("Suppliers", Theme.table(sm)));
        c.add(section("Price comparison (cheapest first)", Theme.table(cm)));
        add(c, BorderLayout.CENTER);
        add(owner ? bar("Compare prices and lead times before you order.",
                button("Add supplier", Theme.PRIMARY, true, this::addSupplier),
                button("Set price", Theme.PRIMARY, false, this::setPrice))
                : bar("Compare prices and lead times before you order."), BorderLayout.NORTH);
    }

    @Override void refresh() {
        sm.setRowCount(0);
        for (Supplier s : inv.suppliers()) sm.addRow(new Object[]{s.name, s.leadDays, s.prices.size()});
        cm.setRowCount(0);
        record Offer(Supplier s, double price) { }
        for (Product p : InventoryManager.mergeSort(inv.all(), Comparator.comparing(Product::getName))) {
            List<Offer> offers = new ArrayList<>();
            for (Supplier s : inv.suppliers()) if (s.prices.containsKey(p.getSku())) offers.add(new Offer(s, s.prices.get(p.getSku())));
            offers = InventoryManager.mergeSort(offers, Comparator.comparingDouble(Offer::price).thenComparingInt(o -> o.s().leadDays));
            StringJoiner all = new StringJoiner(", ");
            for (Offer o : offers) all.add(o.s().name + " " + money(o.price()));
            Offer best = offers.isEmpty() ? null : offers.get(0);
            cm.addRow(new Object[]{p.getSku(), p.getName(), best == null ? "-" : best.s().name,
                    best == null ? "-" : money(best.price()), best == null ? "-" : best.s().leadDays, all.length() == 0 ? "No offers" : all.toString()});
        }
    }

    private void addSupplier() {
        JTextField name = new JTextField(), lead = new JTextField("3");
        JPanel f = new JPanel(new GridLayout(0, 2, 8, 8));
        f.add(new JLabel("Supplier name")); f.add(name);
        f.add(new JLabel("Lead time (days)")); f.add(lead);
        if (JOptionPane.showConfirmDialog(this, f, "Add supplier", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        try {
            if (inv.addSupplier(name.getText(), Integer.parseInt(lead.getText().trim()))) say.accept("Added supplier " + name.getText().trim() + ".");
            else info("Enter a new name and a lead time of 0 or more days.");
        } catch (NumberFormatException ex) { info("Lead time must be a whole number."); }
        changed.run();
    }

    private void setPrice() {
        if (inv.suppliers().isEmpty() || inv.all().isEmpty()) { info("Add a supplier and a product first."); return; }
        JComboBox<String> sup = new JComboBox<>();
        for (Supplier s : inv.suppliers()) sup.addItem(s.name);
        JComboBox<String> prod = new JComboBox<>();
        for (Product p : InventoryManager.mergeSort(inv.all(), Comparator.comparing(Product::getName))) prod.addItem(p.getSku() + " - " + p.getName());
        JTextField price = new JTextField();
        JPanel f = new JPanel(new GridLayout(0, 2, 8, 8));
        f.add(new JLabel("Supplier")); f.add(sup);
        f.add(new JLabel("Product")); f.add(prod);
        f.add(new JLabel("Unit price (P)")); f.add(price);
        if (JOptionPane.showConfirmDialog(this, f, "Set supplier price", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        try {
            String sku = ((String) prod.getSelectedItem()).split(" - ")[0];
            if (inv.setPrice((String) sup.getSelectedItem(), sku, Double.parseDouble(price.getText().trim()))) say.accept("Price saved.");
            else info("Price must be greater than 0.");
        } catch (NumberFormatException ex) { info("Please enter a number, like 12.50."); }
        changed.run();
    }
}

/** Orders page: products the system suggests ordering (top) and the purchase orders (bottom). */
class OrderTab extends Tab {
    private final DefaultTableModel sm = Theme.model("SKU", "Product", "In stock", "Reorder at", "To order", "Best supplier", "Price", "Total", "Status");
    private final JTable suggestTable = Theme.table(sm);
    private final DefaultTableModel m = Theme.model("ID", "Date", "Supplier", "Product", "Qty", "Unit price", "Total", "Status");
    private final JTable table = Theme.table(m);
    private List<InventoryManager.Suggestion> rows = new ArrayList<>();

    OrderTab(InventoryManager inv, Runnable changed, Consumer<String> say, boolean owner) {
        super(inv, changed, say);
        int[] w = {80, 150, 70, 90, 80, 170, 80, 90, 130};
        for (int i = 0; i < w.length; i++) suggestTable.getColumnModel().getColumn(i).setPreferredWidth(w[i]);
        JPanel both = new JPanel(new GridLayout(2, 1, 0, 14));
        both.setOpaque(false);
        both.add(block("Suggested to order", "These products are running low. The system picks the cheapest supplier and the order quantity (EOQ).",
                suggestTable, button("Order selected", Theme.PRIMARY, false, this::orderSelected),
                button("Order all", Theme.PRIMARY, true, this::orderAll)));
        Theme.FlatButton receive = button("Mark received", Theme.BLUE, false, this::receive);
        Theme.FlatButton create = button("New order", Theme.PRIMARY, true, this::newOrder);
        both.add(owner
                ? block("Purchase orders", "The owner approves an order, then staff marks it received when it arrives.", table,
                        create, button("Approve", Theme.PRIMARY, false, this::approve), receive,
                        button("Cancel order", Theme.DANGER, true, this::cancel))
                : block("Purchase orders", "Wait for the owner to approve an order, then mark it received when it arrives.", table, create, receive));
        add(both, BorderLayout.CENTER);
    }

    private JPanel block(String title, String hint, JTable t, JComponent... buttons) {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setOpaque(false);
        JLabel l1 = Theme.label(title, Theme.TEXT, Theme.bold(14f)), l2 = Theme.label(hint, Theme.MUTED, Theme.BASE.deriveFont(12f));
        l1.setAlignmentX(Component.LEFT_ALIGNMENT);
        l2.setAlignmentX(Component.LEFT_ALIGNMENT);
        text.add(l1);
        text.add(l2);
        head.add(text, BorderLayout.WEST);
        JPanel r = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        r.setOpaque(false);
        for (JComponent b : buttons) r.add(b);
        head.add(r, BorderLayout.EAST);
        p.add(head, BorderLayout.NORTH);
        p.add(Theme.scroll(t), BorderLayout.CENTER);
        return p;
    }

    @Override void refresh() {
        rows = inv.suggestions();
        sm.setRowCount(0);
        for (InventoryManager.Suggestion s : rows) {
            PurchaseOrder open = inv.openOrderFor(s.product().getSku());
            sm.addRow(new Object[]{s.product().getSku(), s.product().getName(), s.product().getQty(), s.reorderPoint(), s.qty(),
                    s.supplier() == null ? "No supplier yet" : s.supplier().name, money(s.unitPrice()), money(s.qty() * s.unitPrice()),
                    open != null ? "On order (#" + open.id + ")" : "Needs ordering"});
        }
        m.setRowCount(0);
        for (PurchaseOrder o : inv.orders()) {
            Product p = inv.find(o.sku);
            m.addRow(new Object[]{o.id, o.date, o.supplier, p == null ? o.sku : p.getName(), o.qty, money(o.unitPrice), money(o.total()), o.status});
        }
    }

    private void orderSelected() {
        int r = suggestTable.getSelectedRow();
        if (r < 0) { info("Please select a product in the top table first."); return; }
        InventoryManager.Suggestion s = rows.get(r);
        PurchaseOrder open = inv.openOrderFor(s.product().getSku());
        if (open != null) { info("This product already has order #" + open.id + " waiting."); return; }
        if (s.supplier() == null) { info("No supplier sells this product yet. Add a price in Suppliers > Set price."); return; }
        String in = JOptionPane.showInputDialog(this, "Quantity to order from " + s.supplier().name + ":", s.qty());
        if (in == null) return;
        try {
            PurchaseOrder o = inv.createOrder(s.product().getSku(), s.supplier(), Integer.parseInt(in.trim()));
            if (o == null) { info("Enter a number greater than 0."); return; }
            say.accept("Purchase order #" + o.id + " created. Waiting for approval.");
            changed.run();
        } catch (NumberFormatException ex) { info("Please enter a whole number."); }
    }

    private void orderAll() {
        List<InventoryManager.Suggestion> todo = new ArrayList<>();
        int noSupplier = 0;
        double total = 0;
        for (InventoryManager.Suggestion s : rows) {
            if (inv.openOrderFor(s.product().getSku()) != null) continue;
            if (s.supplier() == null) { noSupplier++; continue; }
            todo.add(s);
            total += s.qty() * s.unitPrice();
        }
        if (todo.isEmpty()) {
            info(noSupplier > 0 ? "No supplier sells the low products yet. Add prices in Suppliers > Set price." : "Nothing to order right now.");
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "Create " + todo.size() + " purchase order(s) with the suggested quantities?\nTotal: P" + money(total),
                "Order all", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        for (InventoryManager.Suggestion s : todo) inv.createOrder(s.product().getSku(), s.supplier(), s.qty());
        say.accept("Created " + todo.size() + " purchase order(s). Waiting for approval."
                + (noSupplier > 0 ? " " + noSupplier + " skipped (no supplier)." : ""));
        changed.run();
    }

    /** Creates an order for any product, not only the low ones. */
    private void newOrder() {
        if (inv.all().isEmpty()) { info("Add a product first."); return; }
        JComboBox<String> prod = new JComboBox<>();
        for (Product p : InventoryManager.mergeSort(inv.all(), Comparator.comparing(Product::getName)))
            prod.addItem(p.getSku() + " - " + p.getName());
        JComboBox<String> sup = new JComboBox<>();
        JLabel price = Theme.label(" ", Theme.MUTED, Theme.BASE);
        JTextField qty = new JTextField("10");
        List<Supplier> options = new ArrayList<>();
        String[] sku = {""};
        Runnable showPrice = () -> {
            int i = sup.getSelectedIndex();
            price.setText(i < 0 || i >= options.size() ? "No supplier sells this product yet" : "Unit price: P" + money(options.get(i).prices.get(sku[0])));
        };
        Runnable fill = () -> {
            sku[0] = ((String) prod.getSelectedItem()).split(" - ")[0];
            options.clear();
            sup.removeAllItems();
            List<Supplier> offers = new ArrayList<>();
            for (Supplier s : inv.suppliers()) if (s.prices.containsKey(sku[0])) offers.add(s);
            for (Supplier s : InventoryManager.mergeSort(offers, Comparator.comparingDouble((Supplier s) -> s.prices.get(sku[0])).thenComparingInt(s -> s.leadDays))) {
                options.add(s);
                sup.addItem(s.name + " (lead " + s.leadDays + " days)");
            }
            showPrice.run();
        };
        prod.addActionListener(e -> fill.run());
        sup.addActionListener(e -> showPrice.run());
        fill.run();
        JPanel f = new JPanel(new GridLayout(0, 2, 8, 8));
        f.add(new JLabel("Product")); f.add(prod);
        f.add(new JLabel("Supplier (cheapest first)")); f.add(sup);
        f.add(new JLabel("Quantity")); f.add(qty);
        f.add(new JLabel("")); f.add(price);
        if (JOptionPane.showConfirmDialog(this, f, "New purchase order", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        int i = sup.getSelectedIndex();
        if (i < 0 || i >= options.size()) { info("No supplier sells this product yet. Add a price in Suppliers > Set price."); return; }
        try {
            PurchaseOrder o = inv.createOrder(sku[0], options.get(i), Integer.parseInt(qty.getText().trim()));
            if (o == null) { info("Enter a quantity greater than 0."); return; }
            say.accept("Purchase order #" + o.id + " created. Waiting for approval.");
            changed.run();
        } catch (NumberFormatException ex) { info("Please enter a whole number for the quantity."); }
    }

    private Integer selectedId() {
        int r = table.getSelectedRow();
        if (r < 0) { info("Please select an order first."); return null; }
        return (Integer) m.getValueAt(r, 0);
    }

    private void approve() {
        Integer id = selectedId();
        if (id == null) return;
        if (inv.approve(id)) say.accept("Order #" + id + " approved.");
        else info("Only pending orders can be approved.");
        changed.run();
    }

    private void cancel() {
        Integer id = selectedId();
        if (id == null) return;
        if (JOptionPane.showConfirmDialog(this, "Cancel order #" + id + "?", "Cancel order", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        if (inv.cancel(id)) say.accept("Order #" + id + " cancelled.");
        else info("This order is already received or cancelled.");
        changed.run();
    }

    private void receive() {
        Integer id = selectedId();
        if (id == null) return;
        String in = JOptionPane.showInputDialog(this, "Expiry date of the delivered items (yyyy-mm-dd):", LocalDate.now().plusMonths(6).toString());
        if (in == null) return;
        try {
            if (inv.receive(id, LocalDate.parse(in.trim()))) say.accept("Order #" + id + " received. Stock updated.");
            else info("Only approved orders can be marked received.");
        } catch (RuntimeException ex) { info("Please use a date like 2027-01-31."); }
        changed.run();
    }
}

/** Best sellers, slow movers, ABC summary, and exact-name search. */
class ReportTab extends Tab {
    private final DefaultTableModel best = Theme.model("Rank", "Product", "Units sold");
    private final DefaultTableModel slow = Theme.model("Rank", "Product", "Units sold");
    private final DefaultTableModel fefo = Theme.model("Order", "Product", "Expires");
    private final JLabel summary = Theme.label(" ", Theme.TEXT, Theme.BASE);
    private final JLabel found = Theme.label(" ", Theme.MUTED, Theme.BASE);
    private final JTextField name = new JTextField(16);

    ReportTab(InventoryManager inv, Runnable changed, Consumer<String> say) {
        super(inv, changed, say);
        JPanel grid = new JPanel(new GridLayout(1, 3, 12, 0));
        grid.setOpaque(false);
        grid.add(section("Best sellers", narrowFirst(Theme.table(best))));
        grid.add(section("Slow movers", narrowFirst(Theme.table(slow))));
        grid.add(section("Sell first (FEFO queue)", narrowFirst(Theme.table(fefo))));
        add(grid, BorderLayout.CENTER);

        name.setBorder(Theme.fieldBorder());
        JPanel find = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        find.setOpaque(false);
        find.add(Theme.label("Find a product by exact name", Theme.MUTED, Theme.BASE));
        find.add(name);
        find.add(button("Find", Theme.PRIMARY, true, this::find));
        find.add(found);
        JPanel south = new JPanel(new GridLayout(2, 1, 0, 8));
        south.setOpaque(false);
        south.add(summary);
        south.add(find);
        add(south, BorderLayout.SOUTH);
    }

    private static JTable narrowFirst(JTable t) {
        t.getColumnModel().getColumn(0).setPreferredWidth(50);
        t.getColumnModel().getColumn(0).setMaxWidth(70);
        return t;
    }

    @Override void refresh() {
        MyHashMap<String, Integer> sold = inv.unitsSold();
        List<Product> ranked = inv.rankBySales();
        best.setRowCount(0);
        slow.setRowCount(0);
        for (int i = 0; i < Math.min(3, ranked.size()); i++)
            best.addRow(new Object[]{i + 1, ranked.get(i).getName(), sold.getOrDefault(ranked.get(i).getSku(), 0)});
        for (int i = 0; i < Math.min(3, ranked.size()); i++) {
            Product p = ranked.get(ranked.size() - 1 - i);
            slow.addRow(new Object[]{i + 1, p.getName(), sold.getOrDefault(p.getSku(), 0)});
        }
        fefo.setRowCount(0);
        MyQueue<Product> queue = inv.fefoQueue();          // Queue: earliest expiry comes out first
        for (int i = 1; i <= 5 && !queue.isEmpty(); i++) {
            Product p = queue.dequeue();
            fefo.addRow(new Object[]{i, p.getName(), p.getExpiry() == null ? "-" : p.getExpiry()});
        }
        int a = 0, b = 0, c = 0;
        double value = 0;
        for (char k : inv.abcAnalysis().values()) { if (k == 'A') a++; else if (k == 'B') b++; else c++; }
        for (Product p : inv.all()) value += p.getValue();
        summary.setText(String.format("Stock value: P%,.0f     ABC: %d items in A, %d in B, %d in C     Orders waiting: %d",
                value, a, b, c, inv.orders().stream().filter(o -> o.status == PurchaseOrder.Status.PENDING || o.status == PurchaseOrder.Status.APPROVED).count()));
    }

    private void find() {
        List<Product> sorted = InventoryManager.mergeSort(inv.all(), Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER));
        Product p = InventoryManager.binarySearchByName(sorted, name.getText().trim());
        found.setText(p == null ? "Not found." : p.getSku() + ": " + p.getQty() + " in stock, P" + money(p.getPrice()));
    }
}

/** Every stock-in and stock-out, newest first. */
class HistoryTab extends Tab {
    private final DefaultTableModel m = Theme.model("Time", "SKU", "Product", "Type", "Qty");

    HistoryTab(InventoryManager inv, Runnable changed, Consumer<String> say) {
        super(inv, changed, say);
        add(bar("Newest first. Use Undo on the Inventory tab to reverse the last one."), BorderLayout.NORTH);
        add(Theme.scroll(Theme.table(m)), BorderLayout.CENTER);
    }

    @Override void refresh() {
        m.setRowCount(0);
        for (Iterator<Transaction> it = inv.history().descendingIterator(); it.hasNext();) {
            Transaction t = it.next();
            Product p = inv.find(t.sku());
            m.addRow(new Object[]{t.time().withNano(0).toString().replace('T', ' '), t.sku(), p == null ? "(deleted)" : p.getName(),
                    t.discard() ? "Discarded (expired)" : t.change() > 0 ? "Stock in" : "Stock out", Math.abs(t.change())});
        }
    }
}

/** Simple bar chart (no extra library). */
class BarChart extends JPanel {
    private final String title;
    private List<String> labels = List.of();
    private List<Double> values = List.of();

    BarChart(String title) {
        this.title = title;
        setBackground(Theme.CARD);
        setBorder(BorderFactory.createLineBorder(Theme.LINE));
        setPreferredSize(new Dimension(100, 200));
    }

    void setData(List<String> labels, List<Double> values) { this.labels = labels; this.values = values; repaint(); }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight(), left = 14, right = 14, top = 38, bottom = 24;
        g2.setFont(Theme.bold(13f));
        g2.setColor(Theme.TEXT);
        g2.drawString(title, left, 22);
        if (values.isEmpty()) { g2.dispose(); return; }
        double max = 1;
        for (double v : values) max = Math.max(max, v);
        int n = values.size(), slot = (w - left - right) / n, bw = Math.max(4, slot - 8);
        g2.setFont(Theme.BASE.deriveFont(10f));
        FontMetrics fm = g2.getFontMetrics();
        for (int i = 0; i < n; i++) {
            int bh = (int) ((h - top - bottom) * values.get(i) / max), x = left + i * slot + (slot - bw) / 2, y = h - bottom - bh;
            g2.setColor(Theme.PRIMARY);
            g2.fillRoundRect(x, y, bw, Math.max(bh, 1), 4, 4);
            g2.setColor(Theme.MUTED);
            g2.drawString(labels.get(i), x + (bw - fm.stringWidth(labels.get(i))) / 2, h - 8);
            if (values.get(i) > 0) {
                String v = String.format("%,.0f", values.get(i));
                g2.setColor(Theme.TEXT);
                g2.drawString(v, x + (bw - fm.stringWidth(v)) / 2, y - 3);
            }
        }
        g2.dispose();
    }
}

/** Sales per day and per month, built from the stock-out history. */
class SalesTab extends Tab {
    private final DefaultTableModel dm = Theme.model("Date", "Units sold", "Sales (P)", "Sales made");
    private final DefaultTableModel mm = Theme.model("Month", "Units sold", "Sales (P)", "Sales made");
    private final BarChart chart = new BarChart("Sales per day, last 14 days (P)");
    private final JLabel summary = Theme.label(" ", Theme.TEXT, Theme.bold(13f));

    SalesTab(InventoryManager inv, Runnable changed, Consumer<String> say) {
        super(inv, changed, say);
        add(summary, BorderLayout.NORTH);
        JPanel left = new JPanel(new BorderLayout(0, 10));
        left.setOpaque(false);
        left.add(chart, BorderLayout.NORTH);
        left.add(section("Daily sales (last 30 days)", firstWide(Theme.table(dm))), BorderLayout.CENTER);
        JPanel grid = new JPanel(new GridLayout(1, 2, 12, 0));
        grid.setOpaque(false);
        grid.add(left);
        grid.add(section("Monthly sales (last 12 months)", firstWide(Theme.table(mm))));
        add(grid, BorderLayout.CENTER);
    }

    private static JTable firstWide(JTable t) {
        t.getColumnModel().getColumn(0).setPreferredWidth(170);
        for (int i = 1; i < t.getColumnCount(); i++) t.getColumnModel().getColumn(i).setPreferredWidth(80);
        return t;
    }

    @Override void refresh() {
        List<InventoryManager.SalesRow> days = inv.salesByDay(30), months = inv.salesByMonth(12);
        dm.setRowCount(0);
        double last30 = 0;
        for (InventoryManager.SalesRow r : days) { dm.addRow(new Object[]{r.period(), r.units(), money(r.amount()), r.count()}); last30 += r.amount(); }
        mm.setRowCount(0);
        for (InventoryManager.SalesRow r : months) mm.addRow(new Object[]{r.period(), r.units(), money(r.amount()), r.count()});
        List<String> labels = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        for (int i = 13; i >= 0; i--) { labels.add(days.get(i).shortLabel()); values.add(days.get(i).amount()); }
        chart.setData(labels, values);
        summary.setText("Today: P" + money(days.get(0).amount()) + "     This month: P" + money(months.get(0).amount())
                + "     Last 30 days: P" + money(last30));
    }
}
