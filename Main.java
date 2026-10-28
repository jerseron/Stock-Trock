package stocktrack;

import java.time.LocalDate;
import java.util.*;

/** Console version (for testing logic). The main app is App.java (Swing UI). */
public class Main {
    public static void main(String[] args) {
        InventoryManager inv = new InventoryManager();
        SampleData.load(inv);
        Scanner in = new Scanner(System.in);
        while (true) {
            System.out.println("\n=== STOCKTRACK ===");
            System.out.println("1 List products   2 Add product   3 Stock in   4 Stock out   5 Undo");
            System.out.println("6 Low-stock alerts   7 Expiring soon   8 Sorted by name   9 Search name");
            System.out.println("10 ABC analysis   11 Transaction history   0 Exit");
            System.out.print("Choice: ");
            String c = in.nextLine().trim();
            try {
                switch (c) {
                    case "1" -> inv.all().forEach(System.out::println);
                    case "2" -> {
                        System.out.print("SKU: "); String sku = in.nextLine();
                        System.out.print("Name: "); String name = in.nextLine();
                        System.out.print("Qty: "); int q = Integer.parseInt(in.nextLine());
                        System.out.print("Price: "); double pr = Double.parseDouble(in.nextLine());
                        System.out.print("Expiry (yyyy-mm-dd): "); LocalDate ex = LocalDate.parse(in.nextLine());
                        System.out.print("Reorder level: "); int rl = Integer.parseInt(in.nextLine());
                        System.out.println(inv.addProduct(new Product(sku, name, q, pr, ex, rl, 1)) ? "Added." : "SKU already exists.");
                    }
                    case "3", "4" -> {
                        System.out.print("SKU: "); String sku = in.nextLine();
                        System.out.print("Qty: "); int q = Integer.parseInt(in.nextLine());
                        boolean ok = c.equals("3") ? inv.stockIn(sku, q) : inv.stockOut(sku, q);
                        System.out.println(ok ? "Done." : "Failed (check SKU / qty).");
                    }
                    case "5" -> System.out.println(inv.undo() ? "Undone." : "Nothing to undo.");
                    case "6" -> inv.lowStockAlerts().forEach(System.out::println);
                    case "7" -> inv.expiringSoon(30).forEach(System.out::println);
                    case "8" -> InventoryManager.mergeSort(inv.all(),
                            Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER)).forEach(System.out::println);
                    case "9" -> {
                        System.out.print("Name: "); String n = in.nextLine();
                        List<Product> sorted = InventoryManager.mergeSort(inv.all(),
                                Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER));
                        Product p = InventoryManager.binarySearchByName(sorted, n);
                        System.out.println(p == null ? "Not found." : p.toString());
                    }
                    case "10" -> inv.abcAnalysis().forEach((k, v) -> System.out.println(k + " -> " + v));
                    case "11" -> inv.history().forEach(System.out::println);
                    case "0" -> { return; }
                    default -> System.out.println("Invalid choice.");
                }
            } catch (RuntimeException e) {
                System.out.println("Invalid input: " + e.getMessage());
            }
        }
    }
}
