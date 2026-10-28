package stocktrack;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.Predicate;

/** Our own doubly linked list. Adding at the end is O(1), which suits the transaction history. */
public class MyLinkedList<T> implements Iterable<T> {
    private static class Node<T> {
        final T value;
        Node<T> prev, next;
        Node(T value) { this.value = value; }
    }

    private Node<T> head, tail;
    private int size;

    public void addLast(T value) {
        Node<T> n = new Node<>(value);
        if (tail == null) head = tail = n;
        else { n.prev = tail; tail.next = n; tail = n; }
        size++;
    }

    public T getLast() { return tail == null ? null : tail.value; }
    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public void clear() { head = tail = null; size = 0; }

    private void unlink(Node<T> n) {
        if (n.prev == null) head = n.next; else n.prev.next = n.next;
        if (n.next == null) tail = n.prev; else n.next.prev = n.prev;
        size--;
    }

    /** Removes the last item equal to o (searching from the end). */
    public boolean removeLastOccurrence(Object o) {
        for (Node<T> n = tail; n != null; n = n.prev)
            if (Objects.equals(n.value, o)) { unlink(n); return true; }
        return false;
    }

    /** Removes every item that matches. Returns how many were removed. */
    public int removeIf(Predicate<T> test) {
        int removed = 0;
        for (Node<T> n = head; n != null; ) {
            Node<T> next = n.next;
            if (test.test(n.value)) { unlink(n); removed++; }
            n = next;
        }
        return removed;
    }

    @Override public Iterator<T> iterator() {
        return new Iterator<>() {
            private Node<T> cur = head;
            public boolean hasNext() { return cur != null; }
            public T next() { T v = cur.value; cur = cur.next; return v; }
        };
    }

    /** Newest first. */
    public Iterator<T> descendingIterator() {
        return new Iterator<>() {
            private Node<T> cur = tail;
            public boolean hasNext() { return cur != null; }
            public T next() { T v = cur.value; cur = cur.prev; return v; }
        };
    }
}
