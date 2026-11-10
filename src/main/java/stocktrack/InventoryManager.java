package stocktrack;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.time.LocalDateTime;
import java.util.*;

/** Core logic. Each section notes which data structure / algorithm it demonstrates. */
public class InventoryManager {
    private static final double ORDER_COST = 50;      // assumed cost of placing one order (P)
    private static final double HOLDING_RATE = 0.20;  // assumed yearly holding cost = 20% of price

    // HASH TABLE: O(1) average lookup by SKU
    private final MyHashMap<String, Product> products = new MyHashMap<>();
    // LINKED LIST: transaction history, O(1) add at end
    private final MyLinkedList<Transaction> log = new MyLinkedList<>();
    // STACK: undo, last transaction is reversed first
    private final MyStack<Transaction> undoStack = new MyStack<>();
    private final List<Supplier> suppliers = new ArrayList<>();
    private final List<PurchaseOrder> orders = new ArrayList<>();

    public record Suggestion(Product product, int reorderPoint, int qty, Supplier supplier, double unitPrice) { }

    // ---------- Products ----------
    public boolean addProduct(Product p) { return products.putIfAbsent(p.getSku(), p) == null; }
    public Product find(String sku) { return products.get(sku); }
    /** Cannot delete a product that still has a pending or approved purchase order. */
    public boolean remove(String sku) { return !hasOpenOrders(sku) && products.remove(sku) != null; }

    public boolean hasOpenOrders(String sku) { return openOrderFor(sku) != null; }

    /** The pending or approved order for a product, or null. */
    public PurchaseOrder openOrderFor(String sku) {
        for (PurchaseOrder o : orders)
            if (o.sku.equals(sku) && (o.status == PurchaseOrder.Status.PENDING || o.status == PurchaseOrder.Status.APPROVED)) return o;
        return null;
    }
    public List<Product> all() { return products.values(); }
    public MyLinkedList<Transaction> history() { return log; }
    public List<Supplier> suppliers() { return suppliers; }
    public List<PurchaseOrder> orders() { return orders; }
    public void loadTransaction(Transaction t) { log.addLast(t); }
    public void clearUndo() { undoStack.clear(); }
    public void clearAll() { products.clear(); log.clear(); undoStack.clear(); suppliers.clear(); orders.clear(); }

    // ---------- Stock movements ----------
    public boolean stockIn(String sku, int qty) {
        Product p = products.get(sku);
        return p != null && stockIn(sku, qty, p.getExpiry());
    }

    public boolean stockIn(String sku, int qty, LocalDate expiry) {
        Product p = products.get(sku);
        if (p == null || qty <= 0) return false;
        p.addBatch(expiry, qty);
        record(new Transaction(sku, qty, expiry, List.of()));
        return true;
    }

    public boolean stockOut(String sku, int qty) {
        Product p = products.get(sku);
        if (p == null || qty <= 0 || p.getQty() < qty) return false;
        record(new Transaction(sku, -qty, LocalDateTime.now(), null, p.consumeFefo(qty), false, p.getPrice()));
        return true;
    }

    private void record(Transaction t) { log.addLast(t); undoStack.push(t); }

    public boolean undo() {
        if (undoStack.isEmpty()) return false;
        Transaction t = undoStack.peek();
        Product p = products.get(t.sku());
        if (p != null) {
            if (t.change() > 0) {
                if (p.getQty() < t.change()) return false; // already sold, cannot take back
                p.removeFromBatch(t.expiry(), t.change());
            } else {
                p.restore(t.pieces());
            }
        }
        undoStack.pop();
        log.removeLastOccurrence(t);
        return true;
    }

    // ---------- Edit and expired stock ----------
    public boolean editProduct(String sku, String name, double price, int reorderLevel, double dailyDemand) {
        Product p = products.get(sku);
        if (p == null || name == null || name.isBlank() || price <= 0 || reorderLevel < 0 || dailyDemand < 0) return false;
        p.setName(name.trim());
        p.setPrice(price);
        p.setReorderLevel(reorderLevel);
        p.setDailyDemand(dailyDemand);
        return true;
    }

    /** Throws away all units that expired before today. Returns how many units were discarded. */
    public int discardExpired() {
        int total = 0;
        for (Product p : products.values()) {
            List<Batch> gone = p.removeExpired(LocalDate.now());
            int q = 0;
            for (Batch b : gone) q += b.qty;
            if (q > 0) {
                record(new Transaction(p.getSku(), -q, java.time.LocalDateTime.now(), null, gone, true));
                total += q;
            }
        }
        return total;
    }

    // ---------- Alerts ----------
    // MIN-HEAP (our own MyMinHeap): most urgent item comes out first, O(log n)
    public List<Product> lowStockAlerts() {
        MyMinHeap<Product> heap = new MyMinHeap<>(
                Comparator.comparingInt(p -> p.getQty() - p.getReorderLevel()));
        for (Product p : products.values()) if (p.getQty() <= p.getReorderLevel()) heap.add(p);
        List<Product> out = new ArrayList<>();
        while (!heap.isEmpty()) out.add(heap.poll());
        return out;
    }

    public List<Product> expiringSoon(int days) {
        MyMinHeap<Product> heap = new MyMinHeap<>(Comparator.comparing(Product::getExpiry));
        LocalDate limit = LocalDate.now().plusDays(days);
        for (Product p : products.values())
            if (p.getExpiry() != null && !p.getExpiry().isAfter(limit)) heap.add(p);
        List<Product> out = new ArrayList<>();
        while (!heap.isEmpty()) out.add(heap.poll());
        return out;
    }

    // QUEUE (FEFO): products with the earliest expiry are sold first
    public MyQueue<Product> fefoQueue() {
        List<Product> list = mergeSort(all(),
                Comparator.comparing(Product::getExpiry, Comparator.nullsLast(Comparator.naturalOrder())));
        MyQueue<Product> queue = new MyQueue<>();
        for (Product p : list) queue.enqueue(p);
        return queue;
    }

    // ---------- Sorting and searching ----------
    // MERGE SORT: stable, O(n log n) time, O(n) space
    public static <T> List<T> mergeSort(List<T> list, Comparator<T> cmp) {
        if (list.size() <= 1) return new ArrayList<>(list);
        int mid = list.size() / 2;
        List<T> left = mergeSort(list.subList(0, mid), cmp);
        List<T> right = mergeSort(list.subList(mid, list.size()), cmp);
        List<T> merged = new ArrayList<>(list.size());
        int i = 0, j = 0;
        while (i < left.size() && j < right.size())
            merged.add(cmp.compare(left.get(i), right.get(j)) <= 0 ? left.get(i++) : right.get(j++));
        while (i < left.size()) merged.add(left.get(i++));
        while (j < right.size()) merged.add(right.get(j++));
        return merged;
    }

    // BINARY SEARCH: list must already be sorted by name, O(log n)
    public static Product binarySearchByName(List<Product> sortedByName, String name) {
        int lo = 0, hi = sortedByName.size() - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            int c = sortedByName.get(mid).getName().compareToIgnoreCase(name);
            if (c == 0) return sortedByName.get(mid);
            if (c < 0) lo = mid + 1; else hi = mid - 1;
        }
        return null;
    }

    // ---------- Reorder formulas ----------
    /** Reorder point = demand during lead time + safety stock. */
    public static int reorderPoint(double dailyDemand, int leadTimeDays, int safetyStock) {
        return (int) Math.ceil(dailyDemand * leadTimeDays) + safetyStock;
    }

    /** Economic Order Quantity = sqrt(2 * D * S / H). */
    public static int eoq(double annualDemand, double orderCost, double holdingCostPerUnit) {
        return (int) Math.ceil(Math.sqrt(2 * annualDemand * orderCost / holdingCostPerUnit));
    }

    /** Products at or below their reorder point, most urgent first, with the best supplier. */
    public List<Suggestion> suggestions() {
        List<Suggestion> out = new ArrayList<>();
        for (Product p : products.values()) {
            Supplier s = bestSupplier(p.getSku());
            int rop = reorderPoint(p.getDailyDemand(), s == null ? 3 : s.leadDays, p.getReorderLevel() / 2);
            if (p.getQty() > rop) continue;
            int q = Math.max(1, eoq(p.getDailyDemand() * 365, ORDER_COST, Math.max(0.01, HOLDING_RATE * p.getPrice())));
            out.add(new Suggestion(p, rop, q, s, s == null ? 0 : s.prices.get(p.getSku())));
        }
        return mergeSort(out, Comparator.comparingInt(x -> x.product().getQty() - x.reorderPoint()));
    }

    /** Suggestions for products that do not have an order waiting yet. */
    public List<Suggestion> needsOrdering() {
        List<Suggestion> out = new ArrayList<>();
        for (Suggestion s : suggestions()) if (openOrderFor(s.product().getSku()) == null) out.add(s);
        return out;
    }

    // ---------- ABC analysis ----------
    /** A = top 80% of stock value, B = next 15%, C = the rest. */
    public Map<String, Character> abcAnalysis() {
        record Valued(Product p, double value) { }
        List<Valued> items = new ArrayList<>();
        double total = 0;
        for (Product p : products.values()) { double v = p.getValue(); items.add(new Valued(p, v)); total += v; }
        List<Valued> sorted = mergeSort(items, Comparator.comparingDouble(Valued::value).reversed());
        Map<String, Character> result = new LinkedHashMap<>();
        double running = 0;
        for (Valued x : sorted) {
            running += x.value();
            double share = total == 0 ? 1 : running / total;
            result.put(x.p().getSku(), share <= 0.80 ? 'A' : share <= 0.95 ? 'B' : 'C');
        }
        return result;
    }

    // ---------- Sales reports ----------
    public MyHashMap<String, Integer> unitsSold() {
        MyHashMap<String, Integer> sold = new MyHashMap<>();
        for (Transaction t : log)
            if (t.change() < 0 && !t.discard()) sold.put(t.sku(), sold.getOrDefault(t.sku(), 0) - t.change());
        return sold;
    }

    /** Products sorted from best seller to slowest mover. */
    public List<Product> rankBySales() {
        MyHashMap<String, Integer> sold = unitsSold();
        return mergeSort(all(), Comparator.comparingInt((Product p) -> sold.getOrDefault(p.getSku(), 0)).reversed()
                .thenComparing(Product::getName));
    }

    // ---------- Daily and monthly sales ----------
    public record SalesRow(String period, String shortLabel, int units, double amount, int count) { }

    private double saleAmount(Transaction t) {
        double price = t.unitPrice();
        if (price <= 0) { Product p = products.get(t.sku()); price = p == null ? 0 : p.getPrice(); } // older data
        return -t.change() * price;
    }

    /** Sales (stock out, not discards) for each of the last n days, newest first. One pass over the history. */
    public List<SalesRow> salesByDay(int n) {
        LocalDate today = LocalDate.now();
        TreeMap<LocalDate, double[]> days = new TreeMap<>(); // [units, amount, count]
        for (int i = 0; i < n; i++) days.put(today.minusDays(i), new double[3]);
        for (Transaction t : log) {
            if (t.change() >= 0 || t.discard()) continue;
            double[] a = days.get(t.time().toLocalDate());
            if (a != null) { a[0] += -t.change(); a[1] += saleAmount(t); a[2]++; }
        }
        List<SalesRow> rows = new ArrayList<>();
        for (Map.Entry<LocalDate, double[]> e : days.descendingMap().entrySet())
            rows.add(new SalesRow(e.getKey() + " " + e.getKey().getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                    String.valueOf(e.getKey().getDayOfMonth()), (int) e.getValue()[0], e.getValue()[1], (int) e.getValue()[2]));
        return rows;
    }

    /** Sales for each of the last n months (including this month), newest first. */
    public List<SalesRow> salesByMonth(int n) {
        YearMonth now = YearMonth.now();
        TreeMap<YearMonth, double[]> months = new TreeMap<>();
        for (int i = 0; i < n; i++) months.put(now.minusMonths(i), new double[3]);
        for (Transaction t : log) {
            if (t.change() >= 0 || t.discard()) continue;
            double[] a = months.get(YearMonth.from(t.time()));
            if (a != null) { a[0] += -t.change(); a[1] += saleAmount(t); a[2]++; }
        }
        List<SalesRow> rows = new ArrayList<>();
        for (Map.Entry<YearMonth, double[]> e : months.descendingMap().entrySet())
            rows.add(new SalesRow(e.getKey().getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + e.getKey().getYear(),
                    e.getKey().getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH), (int) e.getValue()[0], e.getValue()[1], (int) e.getValue()[2]));
        return rows;
    }

    // ---------- Sample (bulk) data ----------
    /** Removes the fake BULK- products, their history, orders and supplier prices. Returns how many products were removed. */
    public int removeSamples() {
        orders.removeIf(o -> o.sku.startsWith("BULK-"));
        log.removeIf(t -> t.sku().startsWith("BULK-"));
        undoStack.clear();
        for (Supplier s : suppliers) s.prices.keySet().removeIf(k -> k.startsWith("BULK-"));
        int n = 0;
        for (Product p : products.values()) if (p.getSku().startsWith("BULK-") && products.remove(p.getSku()) != null) n++;
        return n;
    }

    // ---------- Suppliers ----------
    public boolean addSupplier(String name, int leadDays) {
        if (name == null || name.isBlank() || leadDays < 0) return false;
        for (Supplier s : suppliers) if (s.name.equalsIgnoreCase(name.trim())) return false;
        suppliers.add(new Supplier(name.trim(), leadDays));
        return true;
    }

    public boolean setPrice(String supplierName, String sku, double price) {
        if (price <= 0 || !products.containsKey(sku)) return false;
        for (Supplier s : suppliers) if (s.name.equals(supplierName)) { s.prices.put(sku, price); return true; }
        return false;
    }

    /** Cheapest supplier for a product; a shorter lead time wins a tie. */
    public Supplier bestSupplier(String sku) {
        Supplier best = null;
        for (Supplier s : suppliers) {
            Double price = s.prices.get(sku);
            if (price == null) continue;
            if (best == null || price < best.prices.get(sku)
                    || (price.equals(best.prices.get(sku)) && s.leadDays < best.leadDays)) best = s;
        }
        return best;
    }

    // ---------- Purchase orders ----------
    public PurchaseOrder createOrder(String sku, Supplier s, int qty) {
        if (s == null || qty <= 0 || !s.prices.containsKey(sku)) return null;
        int id = orders.stream().mapToInt(o -> o.id).max().orElse(0) + 1;
        PurchaseOrder o = new PurchaseOrder(id, LocalDate.now(), s.name, sku, qty, s.prices.get(sku));
        orders.add(o);
        return o;
    }

    /** Cancels an order that has not been received yet. */
    public boolean cancel(int id) {
        PurchaseOrder o = order(id);
        if (o == null || o.status == PurchaseOrder.Status.RECEIVED || o.status == PurchaseOrder.Status.CANCELLED) return false;
        o.status = PurchaseOrder.Status.CANCELLED;
        return true;
    }

    private PurchaseOrder order(int id) {
        for (PurchaseOrder o : orders) if (o.id == id) return o;
        return null;
    }

    public boolean approve(int id) {
        PurchaseOrder o = order(id);
        if (o == null || o.status != PurchaseOrder.Status.PENDING) return false;
        o.status = PurchaseOrder.Status.APPROVED;
        return true;
    }

    /** Marks an approved order as received and adds the units to stock. */
    public boolean receive(int id, LocalDate expiry) {
        PurchaseOrder o = order(id);
        if (o == null || o.status != PurchaseOrder.Status.APPROVED || !stockIn(o.sku, o.qty, expiry)) return false;
        o.status = PurchaseOrder.Status.RECEIVED;
        return true;
    }
}
