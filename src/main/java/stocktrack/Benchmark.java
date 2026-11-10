package stocktrack;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/** Measures how the main operations scale. Run it from NetBeans (right-click > Run File) and paste the table in your report. */
public class Benchmark {
    public static void main(String[] args) {
        run(1_000, false); // warm-up so the first row is fair
        System.out.printf("%-10s %-14s %-14s %-14s %-14s %-12s%n", "Products", "Hash lookups", "Merge sort", "Binary search", "Low-stock", "ABC");
        System.out.printf("%-10s %-14s %-14s %-14s %-14s %-12s%n", "(n)", "100,000 (ms)", "(ms)", "10,000 (ms)", "heap (ms)", "(ms)");
        for (int n : new int[]{1_000, 10_000, 100_000}) run(n, true);
    }

    private static void run(int n, boolean print) {
        Random r = new Random(42);
        InventoryManager inv = new InventoryManager();
        String[] skus = new String[n];
        for (int i = 0; i < n; i++) {
            skus[i] = String.format("SKU-%06d", i);
            inv.addProduct(new Product(skus[i], "Item " + r.nextInt(1_000_000), r.nextInt(200), 1 + r.nextInt(500),
                    LocalDate.now().plusDays(1 + r.nextInt(400)), 20, 5));
        }
        double hash = time(() -> { for (int i = 0; i < 100_000; i++) inv.find(skus[r.nextInt(n)]); });
        List<Product> all = inv.all();
        AtomicReference<List<Product>> sorted = new AtomicReference<>();
        double sort = time(() -> sorted.set(InventoryManager.mergeSort(all, Comparator.comparing(Product::getName))));
        double bin = time(() -> { for (int i = 0; i < 10_000; i++) InventoryManager.binarySearchByName(sorted.get(), sorted.get().get(r.nextInt(n)).getName()); });
        double heap = time(inv::lowStockAlerts);
        double abc = time(inv::abcAnalysis);
        if (print) System.out.printf("%-10d %-14.1f %-14.1f %-14.1f %-14.1f %-12.1f%n", n, hash, sort, bin, heap, abc);
    }

    private static double time(Runnable task) {
        long start = System.nanoTime();
        task.run();
        return (System.nanoTime() - start) / 1_000_000.0;
    }
}
