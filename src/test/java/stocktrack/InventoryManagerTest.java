package stocktrack;

import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.*;

class InventoryManagerTest {
    private InventoryManager inv;

    @BeforeEach
    void setUp() {
        inv = new InventoryManager();
        inv.addProduct(new Product("A", "Apple", 10, 5, LocalDate.now().plusDays(5), 5, 1));
        inv.addProduct(new Product("B", "Banana", 3, 2, LocalDate.now().plusDays(2), 5, 1));
    }

    @Test void stockOutCannotGoNegative() { assertFalse(inv.stockOut("A", 99)); }

    @Test void undoRestoresQuantity() {
        inv.stockOut("A", 4);
        assertEquals(6, inv.find("A").getQty());
        inv.undo();
        assertEquals(10, inv.find("A").getQty());
    }

    @Test void lowStockListsBananaOnly() {
        List<Product> low = inv.lowStockAlerts();
        assertEquals(1, low.size());
        assertEquals("B", low.get(0).getSku());
    }

    @Test void mergeSortAndBinarySearch() {
        List<Product> sorted = InventoryManager.mergeSort(inv.all(),
                Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER));
        assertEquals("Apple", sorted.get(0).getName());
        assertNotNull(InventoryManager.binarySearchByName(sorted, "banana"));
        assertNull(InventoryManager.binarySearchByName(sorted, "Mango"));
    }

    @Test void reorderFormulas() {
        assertEquals(25, InventoryManager.reorderPoint(4, 5, 5));
        assertEquals(64, InventoryManager.eoq(1000, 10, 5));
    }
}
