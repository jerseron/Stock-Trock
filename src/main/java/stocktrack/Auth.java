package stocktrack;

import java.util.Map;

/** Demo login. TODO: move accounts to saved data (Jaimer). */
public final class Auth {
    public enum Role { OWNER, STAFF }

    public record User(String username, Role role) {
        public String roleLabel() { return role == Role.OWNER ? "Owner" : "Staff"; }
    }

    private static final Map<String, String> PASSWORDS = Map.of("owner", "owner123", "staff", "staff123");
    private static final Map<String, Role> ROLES = Map.of("owner", Role.OWNER, "staff", Role.STAFF);

    private Auth() { }

    /** Returns the user, or null if the username or password is wrong. */
    public static User login(String username, String password) {
        String u = username == null ? "" : username.trim().toLowerCase();
        if (PASSWORDS.containsKey(u) && PASSWORDS.get(u).equals(password)) return new User(u, ROLES.get(u));
        return null;
    }
}
