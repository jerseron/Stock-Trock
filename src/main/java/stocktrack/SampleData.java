package stocktrack;

import java.time.LocalDate;

/** Demo data used the first time the app runs (before any saved file exists). */
public class SampleData {
    /** Adds n fake products (SKU BULK-0001 ...) with batches, two sales each, and supplier prices. Returns how many were added. */
    public static int loadBulk(InventoryManager inv, int n) {
        String[] items = {"Rice", "Noodles", "Sardines", "Milk", "Coffee", "Sugar", "Soap", "Shampoo", "Biscuits", "Juice", "Tuna", "Cooking Oil", "Eggs", "Bread", "Water"};
        String[] brands = {"Golden", "Fresh", "Happy", "Premium", "Daily", "Classic", "Super", "Mega"};
        java.util.Random r = new java.util.Random(7);
        java.time.LocalDateTime now = java.time.LocalDateTime.now().withNano(0);
        int added = 0;
        for (int i = 1; i <= n; i++) {
            String sku = String.format("BULK-%04d", i);
            double price = 5 + r.nextInt(296);
            Product p = new Product(sku, brands[r.nextInt(brands.length)] + " " + items[r.nextInt(items.length)] + " " + i,
                    r.nextInt(250), price, LocalDate.now().plusDays(5 + r.nextInt(400)), 10 + r.nextInt(31), 1 + r.nextInt(15));
            if (!inv.addProduct(p)) continue;
            added++;
            for (int k = 0; k < 2; k++)
                inv.loadTransaction(new Transaction(sku, -(1 + r.nextInt(5)), now.minusDays(r.nextInt(60)).withHour(9 + r.nextInt(8)).withMinute(r.nextInt(60)),
                        null, java.util.List.of(), false, price));
            for (Supplier s : inv.suppliers()) inv.setPrice(s.name, sku, Math.round(price * (0.70 + r.nextInt(16) / 100.0) * 100) / 100.0);
        }
        return added;
    }

    public static void load(InventoryManager inv) {
        LocalDate t = LocalDate.now();
        inv.addProduct(new Product("SKU-001", "Instant Noodles", 120, 12.0, t.plusDays(90), 30, 8));
        inv.addProduct(new Product("SKU-002", "Bottled Water", 15, 20.0, t.plusDays(300), 25, 10));
        inv.addProduct(new Product("SKU-003", "Fresh Milk", 40, 65.0, t.plusDays(7), 20, 6));
        inv.addProduct(new Product("SKU-004", "Canned Sardines", 60, 28.0, t.plusDays(400), 20, 4));
        inv.addProduct(new Product("SKU-005", "Rice 1kg", 200, 55.0, t.plusDays(200), 50, 12));

        // Past sales (already taken out of the stock above) so the Sales page has something to show.
        // {sku index, days ago, units}
        int[][] sales = {{1, 40, 5}, {1, 33, 5}, {1, 21, 5}, {1, 14, 5}, {1, 6, 5}, {1, 2, 5},
                {2, 38, 4}, {2, 29, 4}, {2, 18, 4}, {2, 9, 4}, {2, 1, 4},
                {3, 25, 3}, {3, 12, 4}, {3, 3, 3},
                {5, 36, 5}, {5, 27, 5}, {5, 20, 5}, {5, 8, 5}, {5, 0, 5}};
        java.time.LocalDateTime now = java.time.LocalDateTime.now().withNano(0);
        for (int[] x : sales) {
            Product p = inv.find("SKU-00" + x[0]);
            inv.loadTransaction(new Transaction(p.getSku(), -x[2], now.minusDays(x[1]).withHour(9 + x[1] % 8).withMinute(0).withSecond(0),
                    null, java.util.List.of(), false, p.getPrice()));
        }

        inv.addSupplier("Metro Wholesale", 2);
        inv.addSupplier("Prime Distributors", 4);
        inv.addSupplier("Farm and Dairy Co.", 1);
        String[] skus = {"SKU-001", "SKU-002", "SKU-003", "SKU-004", "SKU-005"};
        double[] metro = {10.5, 17, 60, 25, 50}, prime = {10, 16, 62, 24, 52};
        for (int i = 0; i < skus.length; i++) {
            inv.setPrice("Metro Wholesale", skus[i], metro[i]);
            inv.setPrice("Prime Distributors", skus[i], prime[i]);
        }
        inv.setPrice("Farm and Dairy Co.", "SKU-003", 58);
        inv.setPrice("Farm and Dairy Co.", "SKU-005", 51);
    }
}
