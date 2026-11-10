package stocktrack;

import java.util.Arrays;

/** Our own stack (last in, first out) on a growing array. Used for Undo. */
public class MyStack<T> {
    private Object[] items = new Object[16];
    private int size;

    public void push(T value) {
        if (size == items.length) items = Arrays.copyOf(items, size * 2);
        items[size++] = value;
    }

    @SuppressWarnings("unchecked")
    public T pop() {
        if (size == 0) return null;
        T v = (T) items[--size];
        items[size] = null;
        return v;
    }

    @SuppressWarnings("unchecked")
    public T peek() { return size == 0 ? null : (T) items[size - 1]; }

    public boolean isEmpty() { return size == 0; }
    public int size() { return size; }
    public void clear() { items = new Object[16]; size = 0; }
}
