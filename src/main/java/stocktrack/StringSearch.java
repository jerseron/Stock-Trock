package stocktrack;

/** Knuth-Morris-Pratt (KMP) string matching. O(n + m): the text is scanned once, never backing up. */
public final class StringSearch {
    private StringSearch() { }

    /** failure[i] = length of the longest proper prefix of the pattern that is also a suffix of pattern[0..i]. */
    static int[] failure(String pattern) {
        int[] f = new int[pattern.length()];
        for (int i = 1, k = 0; i < pattern.length(); i++) {
            while (k > 0 && pattern.charAt(i) != pattern.charAt(k)) k = f[k - 1];
            if (pattern.charAt(i) == pattern.charAt(k)) k++;
            f[i] = k;
        }
        return f;
    }

    /** Position of the first match, or -1. */
    public static int indexOf(String text, String pattern) {
        if (pattern.isEmpty()) return 0;
        int[] f = failure(pattern);
        for (int i = 0, k = 0; i < text.length(); i++) {
            while (k > 0 && text.charAt(i) != pattern.charAt(k)) k = f[k - 1];
            if (text.charAt(i) == pattern.charAt(k)) k++;
            if (k == pattern.length()) return i - k + 1;
        }
        return -1;
    }

    public static boolean contains(String text, String pattern) { return indexOf(text, pattern) >= 0; }
}
