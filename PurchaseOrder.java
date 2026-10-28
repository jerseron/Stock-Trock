package stocktrack;

import java.time.LocalDate;

public class PurchaseOrder {
    public enum Status { PENDING, APPROVED, RECEIVED, CANCELLED }

    public final int id;
    public final LocalDate date;
    public final String supplier, sku;
    public final int qty;
    public final double unitPrice;
    public Status status = Status.PENDING;

    public PurchaseOrder(int id, LocalDate date, String supplier, String sku, int qty, double unitPrice) {
        this.id = id; this.date = date; this.supplier = supplier; this.sku = sku; this.qty = qty; this.unitPrice = unitPrice;
    }

    public double total() { return qty * unitPrice; }
}
