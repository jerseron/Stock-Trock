package stocktrack;

import java.time.LocalDate;

/** A group of units of one product that share an expiry date. */
public class Batch {
    public final LocalDate expiry;
    public int qty;

    public Batch(LocalDate expiry, int qty) { this.expiry = expiry; this.qty = qty; }
}
