package stocktrack;

import java.awt.*;
import javax.swing.*;

/** First screen. Opens the dashboard after a correct login. */
public class LoginFrame extends JFrame {
    private final InventoryManager inv;
    private final JTextField username = new JTextField();
    private final JPasswordField password = new JPasswordField();
    private final JLabel error = Theme.label(" ", Theme.DANGER, Theme.BASE);

    public LoginFrame(InventoryManager inv) {
        super("StockTrack v" + Version.NAME + " - Login");
        this.inv = inv;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(440, 580);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(Theme.BG);

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Theme.CARD);
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.LINE),
                BorderFactory.createEmptyBorder(28, 28, 28, 28)));
        card.setPreferredSize(new Dimension(340, 460));

        add(card, Theme.label("StockTrack", Theme.PRIMARY, Theme.bold(28f)), 0);
        add(card, Theme.label("Inventory and supply chain", Theme.MUTED, Theme.BASE), 2);
        add(card, Theme.label("Username", Theme.TEXT, Theme.bold(13f)), 24);
        username.setBorder(Theme.fieldBorder());
        add(card, username, 6);
        add(card, Theme.label("Password", Theme.TEXT, Theme.bold(13f)), 12);
        password.setBorder(Theme.fieldBorder());
        add(card, password, 6);
        add(card, error, 4);

        Theme.FlatButton login = new Theme.FlatButton("Log in", Theme.PRIMARY, Color.WHITE, false);
        login.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        login.addActionListener(e -> doLogin());
        add(card, login, 10);
        getRootPane().setDefaultButton(login);

        add(card, Theme.label("<html>Demo accounts:<br>owner / owner123 (full access)<br>staff / staff123 (no add or delete)</html>",
                Theme.MUTED, Theme.BASE.deriveFont(12f)), 0);

        add(card, Theme.label("<html>Version " + Version.NAME + "<br>Database: " + DataStore.settings().describe().replace("<", "&lt;") + "</html>", Theme.MUTED, Theme.BASE.deriveFont(11f)), 16);

        root.add(card);
        setContentPane(root);
    }

    private void add(JPanel card, JComponent c, int gapAfterPrevious) {
        if (card.getComponentCount() > 0) card.add(Box.createVerticalStrut(gapAfterPrevious == 0 ? 6 : gapAfterPrevious));
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (c instanceof JTextField) c.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        card.add(c);
    }

    private void doLogin() {
        Auth.User user = Auth.login(username.getText(), new String(password.getPassword()));
        if (user == null) {
            error.setText("Wrong username or password. Try again.");
            password.setText("");
            password.requestFocusInWindow();
            return;
        }
        dispose();
        new MainFrame(inv, user).setVisible(true);
    }
}
