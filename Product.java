package stocktrack;

import java.time.LocalDate;
import java.util.*;

public class Product {
    private static final Comparator<Batch> BY_EXPIRY =
            Comparator.comparing((Batch b) -> b.expiry, Comparator.nullsLast(Comparator.naturalOrder()));

    private final String sku;
    private String name;
    private double price;
    private int reorderLevel;
    private double dailyDemand; // average units sold per day
    // QUEUE of batches, earliest expiry at the front (FEFO: first expired, first out)
    private final MyQueue<Batch> batches = new MyQueue<>();

    public Product(String sku, String name, int qty, double price,
                   LocalDate expiry, int reorderLevel, double dailyDemand) {
        this.sku = sku; this.name = name; this.price = price;
        this.reorderLevel = reorderLevel; this.dailyDemand = dailyDemand;
        if (qty > 0) batches.enqueue(new Batch(expiry, qty));
    }

    public String getSku() { return sku; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public int getReorderLevel() { return reorderLevel; }
    public double getDailyDemand() { return dailyDemand; }
    public void setName(String name) { this.name = name; }
    public void setPrice(double price) { this.price = price; }
    public void setReorderLevel(int reorderLevel) { this.reorderLevel = reorderLevel; }
    public void setDailyDemand(double dailyDemand) { this.dailyDemand = dailyDemand; }
    public List<Batch> getBatches() { return Collections.unmodifiableList(batches.toList()); }

    public int getQty() { int s = 0; for (Batch b : batches) s += b.qty; return s; }
    public double getValue() { return price * getQty(); }
    /** Earliest expiry among the batches in stock, or null. */
    public LocalDate getExpiry() { Batch head = batches.peek(); return head == null ? null : head.expiry; }

    private void rebuild(List<Batch> list) {
        batches.clear();
        for (Batch b : list) batches.enqueue(b);
    }

    public void addBatch(LocalDate expiry, int qty) {
        for (Batch b : batches) if (Objects.equals(b.expiry, expiry)) { b.qty += qty; return; }
        List<Batch> all = batches.toList();
        all.add(new Batch(expiry, qty));
        rebuild(InventoryManager.mergeSort(all, BY_EXPIRY));   // keep the queue in expiry order
    }

    /** FEFO: takes units from the batch at the front (earliest expiry) first. Returns what was taken. */
    public List<Batch> consumeFefo(int qty) {
        List<Batch> used = new ArrayList<>();
        while (qty > 0 && !batches.isEmpty()) {
            Batch head = batches.peek();
            int take = Math.min(qty, head.qty);
            used.add(new Batch(head.expiry, take));
            head.qty -= take; qty -= take;
            if (head.qty == 0) batches.dequeue();    // batch used up, remove it from the queue
        }
        return used;
    }

    /** Removes units from one batch first (used by undo), then from the earliest batches if it ran short. */
    public void removeFromBatch(LocalDate expiry, int qty) {
        List<Batch> all = batches.toList();
        for (Iterator<Batch> it = all.iterator(); it.hasNext() && qty > 0; ) {
            Batch b = it.next();
            if (!Objects.equals(b.expiry, expiry)) continue;
            int take = Math.min(qty, b.qty);
            b.qty -= take; qty -= take;
            if (b.qty == 0) it.remove();
        }
        rebuild(all);
        if (qty > 0) consumeFefo(qty);
    }

    /** Removes every batch that expired before today. Returns what was removed. */
    public List<Batch> removeExpired(LocalDate today) {
        List<Batch> keep = new ArrayList<>(), removed = new ArrayList<>();
        for (Batch b : batches) {
            if (b.expiry != null && b.expiry.isBefore(today)) removed.add(new Batch(b.expiry, b.qty)); else keep.add(b);
        }
        rebuild(keep);
        return removed;
    }

    public void restore(List<Batch> pieces) { for (Batch b : pieces) addBatch(b.expiry, b.qty); }

    @Override
    public String toString() {
        return String.format("%-8s %-18s qty=%-4d P%-8.2f exp=%s", sku, name, getQty(), price, getExpiry());
    }
}
