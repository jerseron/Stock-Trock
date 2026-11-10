package stocktrack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Saves and loads everything in a MariaDB database through JDBC.
 * By default it connects to the "stocktrack" database on this computer as user root with an empty password
 * (the XAMPP default). To use other settings, copy stocktrack.properties.example to stocktrack.properties.
 * The app keeps its data in memory for speed (the data structures), and this class copies it to the tables
 * after every change and reads it back at start-up.
 */
public final class DataStore {
    private static final Path CONFIG = Path.of("stocktrack.properties");
    private static final String DEFAULT_URL = "jdbc:mariadb://localhost:3306/stocktrack";
    private static final String[] TABLES = {"products", "batches", "transactions", "suppliers", "supplier_prices", "orders"};

    /** Where the data lives. */
    public record Settings(String url, String user, String password) {
        public String describe() { return url; }

        private int dbStart() {
            int slash = url.indexOf('/', url.indexOf("://") + 3);
            return slash < 0 ? url.length() : slash + 1;
        }

        /** The server part of the URL, without the database name. */
        String serverUrl() { return url.substring(0, dbStart()); }

        String databaseName() {
            String rest = url.substring(dbStart());
            int q = rest.indexOf('?');
            return q < 0 ? rest : rest.substring(0, q);
        }

        /** Same server and login, different database name (used by the tests so real data is never touched). */
        public Settings withDatabase(String name) {
            String rest = url.substring(dbStart());
            int q = rest.indexOf('?');
            return new Settings(serverUrl() + name + (q < 0 ? "" : rest.substring(q)), user, password);
        }
    }

    private DataStore() { }

    public static Settings settings() {
        Properties p = new Properties();
        if (Files.exists(CONFIG)) {
            try (InputStream in = Files.newInputStream(CONFIG)) { p.load(in); } catch (IOException ignored) { }
        }
        return new Settings(p.getProperty("db.url", DEFAULT_URL).trim(), p.getProperty("db.user", "root").trim(), p.getProperty("db.password", ""));
    }

    private static Connection open(Settings s) throws SQLException {
        return DriverManager.getConnection(s.url(), s.user(), s.password());
    }

    /** Returns null if the database can be reached (it is created if it does not exist yet), otherwise an error message. */
    public static String checkConnection() { return checkConnection(settings()); }

    public static String checkConnection(Settings s) {
        try (Connection c = open(s)) { return null; }
        catch (SQLException first) {
            if (!createDatabaseIfMissing(s)) return first.getMessage();
            try (Connection c = open(s)) { return null; }
            catch (SQLException again) { return again.getMessage(); }
        }
    }

    private static boolean createDatabaseIfMissing(Settings s) {
        String db = s.databaseName();
        if (!db.matches("[A-Za-z0-9_]+")) return false;
        try (Connection c = DriverManager.getConnection(s.serverUrl(), s.user(), s.password()); Statement st = c.createStatement()) {
            st.execute("CREATE DATABASE IF NOT EXISTS `" + db + "` CHARACTER SET utf8mb4");
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    private static String[] schema() {
        return new String[]{
            "CREATE TABLE IF NOT EXISTS products(sku VARCHAR(50) PRIMARY KEY, name VARCHAR(200) NOT NULL, price DOUBLE NOT NULL, reorder_level INT NOT NULL, daily_demand DOUBLE NOT NULL)",
            "CREATE TABLE IF NOT EXISTS batches(sku VARCHAR(50) NOT NULL, expiry VARCHAR(10), qty INT NOT NULL)",
            "CREATE TABLE IF NOT EXISTS transactions(id INT AUTO_INCREMENT PRIMARY KEY, sku VARCHAR(50) NOT NULL, qty_change INT NOT NULL, moved_at VARCHAR(40) NOT NULL, expiry VARCHAR(10), discard INT NOT NULL, unit_price DOUBLE NOT NULL DEFAULT 0)",
            "CREATE TABLE IF NOT EXISTS suppliers(id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100) NOT NULL UNIQUE, lead_days INT NOT NULL)",
            "CREATE TABLE IF NOT EXISTS supplier_prices(supplier VARCHAR(100) NOT NULL, sku VARCHAR(50) NOT NULL, price DOUBLE NOT NULL, PRIMARY KEY(supplier, sku))",
            "CREATE TABLE IF NOT EXISTS orders(id INT PRIMARY KEY, order_date VARCHAR(10) NOT NULL, supplier VARCHAR(100) NOT NULL, sku VARCHAR(50) NOT NULL, qty INT NOT NULL, unit_price DOUBLE NOT NULL, status VARCHAR(20) NOT NULL)"
        };
    }

    public static void save(InventoryManager inv) throws IOException { save(inv, settings()); }

    /** Writes the whole inventory to the database in one transaction (all or nothing). */
    public static void save(InventoryManager inv, Settings s) throws IOException {
        try (Connection c = open(s)) {
            try (Statement st = c.createStatement()) { for (String sql : schema()) st.execute(sql); }
            migrate(c);
            c.setAutoCommit(false);
            try (Statement st = c.createStatement()) { for (String t : TABLES) st.execute("DELETE FROM " + t); }
            try (PreparedStatement p = c.prepareStatement("INSERT INTO products VALUES(?,?,?,?,?)");
                 PreparedStatement b = c.prepareStatement("INSERT INTO batches VALUES(?,?,?)")) {
                for (Product x : inv.all()) {
                    p.setString(1, x.getSku()); p.setString(2, x.getName()); p.setDouble(3, x.getPrice());
                    p.setInt(4, x.getReorderLevel()); p.setDouble(5, x.getDailyDemand());
                    p.addBatch();
                    for (Batch y : x.getBatches()) {
                        b.setString(1, x.getSku()); b.setString(2, y.expiry == null ? null : y.expiry.toString()); b.setInt(3, y.qty);
                        b.addBatch();
                    }
                }
                p.executeBatch();
                b.executeBatch();
            }
            try (PreparedStatement t = c.prepareStatement("INSERT INTO transactions(sku,qty_change,moved_at,expiry,discard,unit_price) VALUES(?,?,?,?,?,?)")) {
                for (Transaction x : inv.history()) {
                    t.setString(1, x.sku()); t.setInt(2, x.change()); t.setString(3, x.time().toString());
                    t.setString(4, x.expiry() == null ? null : x.expiry().toString()); t.setInt(5, x.discard() ? 1 : 0); t.setDouble(6, x.unitPrice());
                    t.addBatch();
                }
                t.executeBatch();
            }
            try (PreparedStatement sp = c.prepareStatement("INSERT INTO suppliers(name,lead_days) VALUES(?,?)");
                 PreparedStatement pr = c.prepareStatement("INSERT INTO supplier_prices VALUES(?,?,?)")) {
                for (Supplier x : inv.suppliers()) {
                    sp.setString(1, x.name); sp.setInt(2, x.leadDays); sp.addBatch();
                    for (Map.Entry<String, Double> e : x.prices.entrySet()) {
                        pr.setString(1, x.name); pr.setString(2, e.getKey()); pr.setDouble(3, e.getValue()); pr.addBatch();
                    }
                }
                sp.executeBatch();
                pr.executeBatch();
            }
            try (PreparedStatement o = c.prepareStatement("INSERT INTO orders VALUES(?,?,?,?,?,?,?)")) {
                for (PurchaseOrder x : inv.orders()) {
                    o.setInt(1, x.id); o.setString(2, x.date.toString()); o.setString(3, x.supplier); o.setString(4, x.sku);
                    o.setInt(5, x.qty); o.setDouble(6, x.unitPrice); o.setString(7, x.status.name());
                    o.addBatch();
                }
                o.executeBatch();
            }
            c.commit();
        } catch (SQLException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    public static boolean load(InventoryManager inv) { return load(inv, settings()); }

    /** Returns false (and leaves the inventory empty) if the database has no StockTrack tables yet or is damaged. */
    public static boolean load(InventoryManager inv, Settings s) {
        try (Connection c = open(s); Statement st = c.createStatement()) {
            migrate(c);
            inv.clearAll();
            try (ResultSet r = st.executeQuery("SELECT * FROM products")) {
                while (r.next()) inv.addProduct(new Product(r.getString("sku"), r.getString("name"), 0, r.getDouble("price"), null,
                        r.getInt("reorder_level"), r.getDouble("daily_demand")));
            }
            try (ResultSet r = st.executeQuery("SELECT * FROM batches")) {
                while (r.next()) inv.find(r.getString("sku")).addBatch(date(r.getString("expiry")), r.getInt("qty"));
            }
            try (ResultSet r = st.executeQuery("SELECT * FROM transactions ORDER BY id")) {
                while (r.next()) inv.loadTransaction(new Transaction(r.getString("sku"), r.getInt("qty_change"),
                        LocalDateTime.parse(r.getString("moved_at")), date(r.getString("expiry")), List.of(), r.getInt("discard") == 1,
                        r.getDouble("unit_price")));
            }
            try (ResultSet r = st.executeQuery("SELECT * FROM suppliers ORDER BY id")) {
                while (r.next()) inv.addSupplier(r.getString("name"), r.getInt("lead_days"));
            }
            try (ResultSet r = st.executeQuery("SELECT * FROM supplier_prices")) {
                while (r.next()) inv.setPrice(r.getString("supplier"), r.getString("sku"), r.getDouble("price"));
            }
            try (ResultSet r = st.executeQuery("SELECT * FROM orders ORDER BY id")) {
                while (r.next()) {
                    PurchaseOrder o = new PurchaseOrder(r.getInt("id"), LocalDate.parse(r.getString("order_date")), r.getString("supplier"),
                            r.getString("sku"), r.getInt("qty"), r.getDouble("unit_price"));
                    o.status = PurchaseOrder.Status.valueOf(r.getString("status"));
                    inv.orders().add(o);
                }
            }
            return true;
        } catch (Exception e) {
            inv.clearAll();
            return false;
        }
    }

    /** Databases made by older versions get the new column (errors are ignored when it already exists). */
    private static void migrate(Connection c) {
        try (Statement st = c.createStatement()) {
            st.execute("ALTER TABLE transactions ADD COLUMN IF NOT EXISTS unit_price DOUBLE NOT NULL DEFAULT 0");
        } catch (SQLException ignored) { }
    }

    private static LocalDate date(String s) { return s == null || s.isEmpty() ? null : LocalDate.parse(s); }
}
