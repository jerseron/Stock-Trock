package stocktrack;

import java.io.IOException;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/** Entry point of the Swing app. Run this one. */
public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Theme.install();
            String error = DataStore.checkConnection();
            if (error != null) {
                JOptionPane.showMessageDialog(null, "Cannot connect to the database.\n\n" + error
                        + "\n\nCheck stocktrack.properties and make sure the database is running.", "StockTrack", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
            InventoryManager inv = new InventoryManager();
            if (!DataStore.load(inv)) {      // first run: use demo data
                inv.clearAll();
                SampleData.load(inv);
                try { DataStore.save(inv); } catch (IOException ex) {
                    JOptionPane.showMessageDialog(null, "Could not create the tables.\n" + ex.getMessage(), "StockTrack", JOptionPane.ERROR_MESSAGE);
                    System.exit(1);
                }
            }
            new LoginFrame(inv).setVisible(true);
        });
    }
}
