package stocktrack;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

/** Tests for our own data structures and the KMP search. */
class StructuresTest {
    @Test void hashMapPutGetRemoveAndResize() {
        MyHashMap<String, Integer> m = new MyHashMap<>();
        for (int i = 0; i < 5000; i++) assertNull(m.put("key" + i, i));
        assertEquals(5000, m.size());
        for (int i = 0; i < 5000; i++) assertEquals(i, m.get("key" + i));
        assertEquals(7, m.put("key7", 70));        // replaces and returns the old value
        assertEquals(70, m.get("key7"));
        for (int i = 0; i < 5000; i += 2) assertNotNull(m.remove("key" + i));
        assertEquals(2500, m.size());
        assertNull(m.get("key0"));
        assertFalse(m.containsKey("key0"));
        assertEquals(2500, m.values().size());
        assertEquals(1, m.putIfAbsent("key1", 99));
        assertNull(m.putIfAbsent("new", 5));
        assertEquals(0, m.getOrDefault("missing", 0));
    }

    @Test void linkedListKeepsOrderAndRemoves() {
        MyLinkedList<Integer> l = new MyLinkedList<>();
        for (int i = 1; i <= 5; i++) l.addLast(i);
        assertEquals(5, l.size());
        assertEquals(5, l.getLast());
        assertTrue(l.removeLastOccurrence(3));
        assertFalse(l.removeLastOccurrence(42));
        StringBuilder forward = new StringBuilder(), backward = new StringBuilder();
        for (int v : l) forward.append(v);
        for (Iterator<Integer> it = l.descendingIterator(); it.hasNext(); ) backward.append(it.next());
        assertEquals("1245", forward.toString());
        assertEquals("5421", backward.toString());
        assertEquals(2, l.removeIf(v -> v % 2 == 0));
        assertEquals(2, l.size());
    }

    @Test void stackIsLastInFirstOut() {
        MyStack<String> s = new MyStack<>();
        for (int i = 0; i < 100; i++) s.push("x" + i);
        assertEquals("x99", s.pop());
        assertEquals("x98", s.peek());
        assertEquals(99, s.size());
        s.clear();
        assertTrue(s.isEmpty());
        assertNull(s.pop());
    }

    @Test void queueIsFirstInFirstOut() {
        MyQueue<Integer> q = new MyQueue<>();
        for (int i = 1; i <= 4; i++) q.enqueue(i);
        assertEquals(1, q.dequeue());
        assertEquals(2, q.peek());
        assertEquals(List.of(2, 3, 4), q.toList());
        q.dequeue(); q.dequeue(); q.dequeue();
        assertTrue(q.isEmpty());
        assertNull(q.dequeue());
        q.enqueue(9);
        assertEquals(9, q.dequeue());
    }

    @Test void heapReturnsSmallestFirst() {
        MyMinHeap<Integer> h = new MyMinHeap<Integer>(Comparator.naturalOrder());
        Random r = new Random(1);
        List<Integer> expected = new ArrayList<>();
        for (int i = 0; i < 1000; i++) { int v = r.nextInt(10000); h.add(v); expected.add(v); }
        Collections.sort(expected);
        for (int e : expected) assertEquals(e, h.poll());
        assertTrue(h.isEmpty());
        assertNull(h.poll());
    }

    @Test void kmpFindsPatterns() {
        assertEquals(10, StringSearch.indexOf("ababcabcabababd", "ababd"));
        assertEquals(0, StringSearch.indexOf("anything", ""));
        assertEquals(-1, StringSearch.indexOf("rice", "noodles"));
        assertEquals(4, StringSearch.indexOf("sku-001", "001"));
        assertTrue(StringSearch.contains("instant noodles", "noo"));
        assertFalse(StringSearch.contains("abc", "abcd"));
        assertArrayEquals(new int[]{0, 0, 1, 2, 0}, StringSearch.failure("ababd"));
    }
}
