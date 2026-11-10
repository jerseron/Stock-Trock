package stocktrack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Our own queue (first in, first out) on linked nodes. enqueue and dequeue are O(1). Used for FEFO. */
public class MyQueue<T> implements Iterable<T> {
    private static class Node<T> {
        final T value;
        Node<T> next;
        Node(T value) { this.value = value; }
    }

    private Node<T> head, tail;
    private int size;

    public void enqueue(T value) {
        Node<T> n = new Node<>(value);
        if (tail == null) head = tail = n; else { tail.next = n; tail = n; }
        size++;
    }

    /** Removes and returns the item at the front, or null if empty. */
    public T dequeue() {
        if (head == null) return null;
        T v = head.value;
        head = head.next;
        if (head == null) tail = null;
        size--;
        return v;
    }

    public T peek() { return head == null ? null : head.value; }
    public boolean isEmpty() { return size == 0; }
    public int size() { return size; }
    public void clear() { head = tail = null; size = 0; }

    public List<T> toList() {
        List<T> out = new ArrayList<>(size);
        for (T v : this) out.add(v);
        return out;
    }

    @Override public Iterator<T> iterator() {
        return new Iterator<>() {
            private Node<T> cur = head;
            public boolean hasNext() { return cur != null; }
            public T next() { T v = cur.value; cur = cur.next; return v; }
        };
    }
}
