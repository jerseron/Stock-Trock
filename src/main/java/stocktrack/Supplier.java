package stocktrack;

import java.util.LinkedHashMap;
import java.util.Map;

public class Supplier {
    public final String name;
    public int leadDays;
    public final Map<String, Double> prices = new LinkedHashMap<>(); // SKU -> unit price

    public Supplier(String name, int leadDays) { this.name = name; this.leadDays = leadDays; }
}
