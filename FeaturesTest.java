package stocktrack;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import org.junit.jupiter.api.*;

class FeaturesTest {
    private InventoryManager inv;

    @BeforeEach
    void setUp() { inv = new InventoryManager(); SampleData.load(inv); }

    @Test void fefoTakesEarliestBatchFirst() {
        LocalDate t = LocalDate.now();
        inv.stockIn("SKU-004", 10, t.plusDays(5));
        inv.stockOut("SKU-004", 12);
        assertEquals(58, inv.find("SKU-004").getQty());
        assertEquals(t.plusDays(400), inv.find("SKU-004").getExpiry());
    }

    @Test void undoRestoresBatches() {
        LocalDate t = LocalDate.now();
        inv.stockIn("SKU-004", 10, t.plusDays(5));
        inv.stockOut("SKU-004", 12);
        assertTrue(inv.undo());
        assertEquals(70, inv.find("SKU-004").getQty());
        assertEquals(t.plusDays(5), inv.find("SKU-004").getExpiry());
    }

    @Test void suggestionsPickCheapestSupplier() {
        assertEquals(1, inv.suggestions().size());
        assertEquals("Prime Distributors", inv.suggestions().get(0).supplier().name);
        assertEquals("Farm and Dairy Co.", inv.bestSupplier("SKU-003").name);
    }

    @Test void orderFlowAddsStockOnlyAfterApproval() {
        PurchaseOrder o = inv.createOrder("SKU-002", inv.bestSupplier("SKU-002"), 300);
        assertFalse(inv.receive(o.id, LocalDate.now().plusDays(100)));
        assertTrue(inv.approve(o.id));
        assertTrue(inv.receive(o.id, LocalDate.now().plusDays(100)));
        assertEquals(315, inv.find("SKU-002").getQty());
    }

    @Test void bestSellerIsNoodles() { assertEquals("SKU-001", inv.rankBySales().get(0).getSku()); }

    @Test void saveAndLoadRoundTrip() throws Exception {
        Path f = Files.createTempFile("stocktrack", ".db");
        DataStore.save(inv, f);
        InventoryManager copy = new InventoryManager();
        assertTrue(DataStore.load(copy, f));
        assertEquals(5, copy.all().size());
        assertEquals(3, copy.suppliers().size());
        assertEquals(inv.history().size(), copy.history().size());
        assertEquals(15, copy.find("SKU-002").getQty());
    }

    @Test void editProductChangesDetailsAndValidates() {
        assertTrue(inv.editProduct("SKU-001", "Noodles Cup", 15, 40, 9));
        assertEquals("Noodles Cup", inv.find("SKU-001").getName());
        assertEquals(40, inv.find("SKU-001").getReorderLevel());
        assertFalse(inv.editProduct("SKU-001", " ", 15, 40, 9));
        assertFalse(inv.editProduct("SKU-001", "X", 0, 40, 9));
    }

    @Test void discardExpiredIsNotASale() {
        inv.stockIn("SKU-004", 5, LocalDate.now().minusDays(3));
        int before = inv.find("SKU-004").getQty();
        assertEquals(5, inv.discardExpired());
        assertEquals(before - 5, inv.find("SKU-004").getQty());
        assertEquals(0, inv.unitsSold().getOrDefault("SKU-004", 0));
        assertEquals(0, inv.discardExpired());
    }

    @Test void cancelOrderOnlyBeforeReceived() {
        PurchaseOrder o = inv.createOrder("SKU-002", inv.bestSupplier("SKU-002"), 10);
        assertTrue(inv.cancel(o.id));
        assertFalse(inv.cancel(o.id));
        assertFalse(inv.approve(o.id));
    }

    @Test void fefoQueueHasEarliestExpiryFirst() {
        MyQueue<Product> q = inv.fefoQueue();
        assertEquals("SKU-003", q.dequeue().getSku()); // fresh milk expires first
        assertEquals(4, q.size());
    }

    @Test void canOrderAnyProductAndCannotDeleteWhileOrderOpen() {
        PurchaseOrder o = inv.createOrder("SKU-005", inv.bestSupplier("SKU-005"), 20); // rice is not low on stock
        assertNotNull(o);
        assertTrue(inv.hasOpenOrders("SKU-005"));
        assertFalse(inv.remove("SKU-005"));
        inv.cancel(o.id);
        assertTrue(inv.remove("SKU-005"));
    }

    @Test void salesByDayAndMonthMatchTheHistory() {
        double amount = 0;
        int units = 0;
        for (InventoryManager.SalesRow r : inv.salesByMonth(12)) { amount += r.amount(); units += r.units(); }
        assertEquals(85, units);
        assertEquals(2785.0, amount, 0.001);
        long monthsWithSales = inv.salesByMonth(12).stream().filter(r -> r.units() > 0).count();
        assertTrue(monthsWithSales >= 2);
        inv.stockOut("SKU-004", 2); // today the demo data sold 5 rice (P55 each), plus these 2 sardines (P28 each)
        assertEquals(7, inv.salesByDay(30).get(0).units());
        assertEquals(331.0, inv.salesByDay(30).get(0).amount(), 0.001);
    }

    @Test void openOrderRemovesProductFromNeedsOrdering() {
        assertEquals(1, inv.needsOrdering().size());
        PurchaseOrder o = inv.createOrder("SKU-002", inv.bestSupplier("SKU-002"), 300);
        assertEquals(0, inv.needsOrdering().size());
        assertEquals(1, inv.suggestions().size()); // still low on stock, but already on order
        assertEquals(o.id, inv.openOrderFor("SKU-002").id);
        inv.cancel(o.id);
        assertEquals(1, inv.needsOrdering().size());
    }
}
