package stocktrack;

import java.util.Arrays;
import java.util.Comparator;

/** Our own min-heap (priority queue) on an array. add and poll are O(log n); peek is O(1). */
public class MyMinHeap<T> {
    private Object[] a = new Object[16];
    private int size;
    private final Comparator<T> cmp;

    public MyMinHeap(Comparator<T> cmp) { this.cmp = cmp; }

    @SuppressWarnings("unchecked")
    private T at(int i) { return (T) a[i]; }

    public void add(T value) {
        if (size == a.length) a = Arrays.copyOf(a, size * 2);
        a[size] = value;
        siftUp(size++);
    }

    public T peek() { return size == 0 ? null : at(0); }

    /** Removes and returns the smallest item (the most urgent), or null if empty. */
    public T poll() {
        if (size == 0) return null;
        T top = at(0);
        a[0] = a[--size];
        a[size] = null;
        if (size > 0) siftDown(0);
        return top;
    }

    public boolean isEmpty() { return size == 0; }
    public int size() { return size; }

    private void siftUp(int i) {
        while (i > 0) {
            int parent = (i - 1) / 2;
            if (cmp.compare(at(i), at(parent)) >= 0) break;
            swap(i, parent);
            i = parent;
        }
    }

    private void siftDown(int i) {
        while (true) {
            int left = 2 * i + 1, right = left + 1, smallest = i;
            if (left < size && cmp.compare(at(left), at(smallest)) < 0) smallest = left;
            if (right < size && cmp.compare(at(right), at(smallest)) < 0) smallest = right;
            if (smallest == i) break;
            swap(i, smallest);
            i = smallest;
        }
    }

    private void swap(int i, int j) { Object t = a[i]; a[i] = a[j]; a[j] = t; }
}
