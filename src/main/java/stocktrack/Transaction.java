package stocktrack;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * One stock movement. change > 0 is stock-in, change < 0 is stock-out.
 * discard = true means expired stock thrown away (not a sale).
 * unitPrice is the selling price at the time of a sale (0 = unknown, older data).
 */
public record Transaction(String sku, int change, LocalDateTime time, LocalDate expiry, List<Batch> pieces,
                          boolean discard, double unitPrice) {
    public Transaction(String sku, int change) { this(sku, change, LocalDateTime.now(), null, List.of(), false, 0); }
    public Transaction(String sku, int change, LocalDate expiry, List<Batch> pieces) {
        this(sku, change, LocalDateTime.now(), expiry, pieces, false, 0);
    }
    public Transaction(String sku, int change, LocalDateTime time, LocalDate expiry, List<Batch> pieces, boolean discard) {
        this(sku, change, time, expiry, pieces, discard, 0);
    }

    @Override
    public String toString() {
        return time.withNano(0) + "  " + sku + "  " + (discard ? "DISCARD " : change > 0 ? "IN +" : "OUT ") + change;
    }
}
