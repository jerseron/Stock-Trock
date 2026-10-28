# Time and space complexity

n = number of products, t = number of transactions in the history, b = batches of one product, s = suppliers.

All core data structures and algorithms are our own code (in `src/main/java/stocktrack`). No `java.util` collection is used for them.

| Data structure / algorithm | File | Used for | Operation | Time | Space |
|---|---|---|---|---|---|
| Hash Table (separate chaining, doubles at 75% full) | `MyHashMap` | Find product by SKU, units sold per product | search / insert / delete | O(1) average, O(n) worst case | O(n) |
| Doubly Linked List | `MyLinkedList` | Transaction history | add at end / traverse / remove last match | O(1) / O(t) / O(t) | O(t) |
| Stack (growing array) | `MyStack` | Undo | push / pop / peek | O(1) amortized | O(t) |
| Min-Heap (array) | `MyMinHeap` | Low-stock and expiry alerts, most urgent first | add / poll | O(log n) each, O(n log n) to list all | O(n) |
| Queue (linked nodes), batches per product | `MyQueue` in `Product` | Sell the earliest-expiring batch first (FEFO) | peek / dequeue at the front | O(1) per batch used; adding a batch re-sorts, O(b log b) | O(b) |
| Queue, product order | `MyQueue` in `InventoryManager` | "Sell first" list on the Reports page | enqueue / dequeue | O(1) each after one O(n log n) sort | O(n) |
| Merge Sort | `InventoryManager.mergeSort` | Sort products, suggestions, supplier prices, batches | sort | O(n log n) always | O(n) |
| Binary Search | `InventoryManager.binarySearchByName` | Find a product by exact name | search | O(log n) | O(1) |
| KMP string matching | `StringSearch` | Inventory search box (SKU and name) | search one text | O(L + m), L = text length, m = pattern length; whole table O(n(L + m)) | O(m) |
| Reorder Point formula | `InventoryManager` | When to reorder | compute | O(1) per product | O(1) |
| EOQ formula | `InventoryManager` | How much to order | compute | O(1) per product | O(1) |
| ABC Analysis | `InventoryManager` | Rank products by stock value | classify | O(n log n) (sort) + O(n) | O(n) |
| Best supplier search | `InventoryManager` | Cheapest supplier per product | linear scan of suppliers | O(s) | O(1) |
| Sales per day / month | `InventoryManager` | Totals from the transaction history | one pass over the history, grouped with a `TreeMap` | O(t log d), d = days or months shown | O(d) |

Why these were chosen is explained in the proposal (Section 3).
