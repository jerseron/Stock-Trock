# Performance test (scalability)

The course asks the system to handle at least 500 to 1,000 records. StockTrack was tested up to 100,000 products.

## 1. Try it in the app (1,000 records)
Log in as `owner`, open Inventory, click **Samples**, then **Load 1,000 products**. This adds 1,000 fake products with about 2,000 sales, so the app has 1,005 products and over 2,000 history rows. Open every page and try the search box. Click **Samples > Remove samples** to go back to the demo data.
The status line shows how long it took to update the screen and save everything to the database (about 0.3 seconds on the test machine, with SQLite).

## 2. Benchmark (up to 100,000 products)
Run `Benchmark.java` (right-click > Run File in NetBeans) on your own computer and paste your numbers in the second table.
The numbers below were measured on a cloud test machine (Java 17) using our own data structures, so yours will be different.

| Products (n) | 100,000 hash lookups (ms) | Merge sort (ms) | 10,000 binary searches (ms) | Low-stock heap (ms) | ABC analysis (ms) |
|---|---|---|---|---|---|
| 1,000 | 10.9 | 3.1 | 13.3 | 4.0 | 1.3 |
| 10,000 | 16.8 | 22.8 | 18.2 | 8.9 | 25.9 |
| 100,000 | 39.3 | 391.1 | 26.8 | 56.0 | 140.5 |

## How to explain the results
- **Hash lookup and binary search** barely grow when n grows 100 times. This matches O(1) average and O(log n).
- **Merge sort, heap alerts, and ABC analysis** grow a little faster than n, which matches O(n log n).
- Even with 100,000 products, every operation finishes in well under one second, which is far more than a small store needs.
- Small variations (for example at 1,000 products) come from the computer's memory cache and from Java warming up, not from the algorithm.

## Your own results (fill in)
| Products (n) | Hash lookups | Merge sort | Binary search | Heap | ABC |
|---|---|---|---|---|---|
| 1,000 | | | | | |
| 10,000 | | | | | |
| 100,000 | | | | | |

Computer used: ____________  (CPU, RAM, Java version)
