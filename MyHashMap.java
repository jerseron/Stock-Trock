package stocktrack;

import java.util.ArrayList;
import java.util.List;

/**
 * Our own hash table (separate chaining). Average O(1) put / get / remove.
 * The table doubles in size when it is more than 75% full, so chains stay short.
 */
public class MyHashMap<K, V> {
    private static class Node<K, V> {
        final K key;
        V value;
        Node<K, V> next;
        Node(K key, V value, Node<K, V> next) { this.key = key; this.value = value; this.next = next; }
    }

    private Node<K, V>[] table;
    private int size;

    @SuppressWarnings("unchecked")
    public MyHashMap() { table = (Node<K, V>[]) new Node[16]; }

    private static int index(Object key, int length) {
        int h = key.hashCode();
        h ^= (h >>> 16);                 // spread the bits
        return (h & 0x7fffffff) % length;
    }

    public V get(K key) {
        for (Node<K, V> n = table[index(key, table.length)]; n != null; n = n.next)
            if (n.key.equals(key)) return n.value;
        return null;
    }

    public V getOrDefault(K key, V fallback) { V v = get(key); return v == null ? fallback : v; }

    public boolean containsKey(K key) { return get(key) != null; }

    /** Adds or replaces. Returns the old value, or null. */
    public V put(K key, V value) {
        int i = index(key, table.length);
        for (Node<K, V> n = table[i]; n != null; n = n.next)
            if (n.key.equals(key)) { V old = n.value; n.value = value; return old; }
        table[i] = new Node<>(key, value, table[i]);
        if (++size > table.length * 3 / 4) resize();
        return null;
    }

    /** Adds only if the key is new. Returns the value already there, or null if it was added. */
    public V putIfAbsent(K key, V value) {
        V existing = get(key);
        if (existing != null) return existing;
        put(key, value);
        return null;
    }

    public V remove(K key) {
        int i = index(key, table.length);
        Node<K, V> prev = null;
        for (Node<K, V> n = table[i]; n != null; prev = n, n = n.next) {
            if (n.key.equals(key)) {
                if (prev == null) table[i] = n.next; else prev.next = n.next;
                size--;
                return n.value;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private void resize() {
        Node<K, V>[] old = table;
        table = (Node<K, V>[]) new Node[old.length * 2];
        for (Node<K, V> head : old)
            for (Node<K, V> n = head; n != null; ) {
                Node<K, V> next = n.next;
                int i = index(n.key, table.length);
                n.next = table[i];
                table[i] = n;
                n = next;
            }
    }

    public int size() { return size; }

    @SuppressWarnings("unchecked")
    public void clear() { table = (Node<K, V>[]) new Node[16]; size = 0; }

    public List<V> values() {
        List<V> out = new ArrayList<>(size);
        for (Node<K, V> head : table) for (Node<K, V> n = head; n != null; n = n.next) out.add(n.value);
        return out;
    }
}
