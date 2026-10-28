# Changelog

Format: version, date, what changed. Newest on top.
Rule: fixing a bug = 1.0.1, 1.0.2 ... / adding a feature = 1.1.0, 1.2.0 ... / big change = 2.0.0.

## 2.0.0 - 2026-10-24
- **Our own data structures.** The Hash Table (`MyHashMap`), Linked List (`MyLinkedList`), Stack (`MyStack`), Queue (`MyQueue`), and Min-Heap (`MyMinHeap`) are now written by the team. The program no longer uses `HashMap`, `LinkedList`, `PriorityQueue`, or `ArrayDeque` for its core logic. This follows the course rule that core data structures and algorithms must be implemented by the students.
- **New algorithm: KMP string matching** (`StringSearch`). The Inventory search box uses it for SKU and name.
- **Samples button (Inventory, owner only):** loads 1,000 fake products with sales history to test many records, and can remove them again.
- 7 new unit tests for the data structures and KMP. All 24 unit tests were run and pass.
- New numbers in `docs/PERFORMANCE.md`, and `docs/COMPLEXITY.md` now lists our own structures.
- The screens and the database are not changed, so data from v1.x opens normally.

## 1.5.0 - 2026-10-24
- The Reorder page is gone. Its suggestions are now at the top of the **Orders** page ("Suggested to order"), and the purchase orders are below. Less clicking and less confusion.
- New buttons: **Order selected** (one product, you can change the quantity) and **Order all** (one order per suggested product, with a total and a confirmation)
- Products that already have a pending or approved order show "On order (#id)" and cannot be ordered twice
- The side menu shows how many products need ordering, for example "Orders (1)"
- Added a unit test for this (18 tests in total)

## 1.4.0 - 2026-10-23
- New **Sales** page in the side menu: sales per day (last 30 days) with a bar chart of the last 14 days, and sales per month (last 12 months). Shows units sold, sales amount, and how many sales were made.
- Each sale now remembers the selling price at that time, so past sales do not change when you edit a product's price. (New column `unit_price` in the transactions table.)
- Databases from v1.3.0 and older are upgraded automatically, no need to delete them. Old sales use the product's current price.
- The demo data now has past sales on different dates (two months) so the Sales page has something to show
- Added a unit test for daily and monthly sales (17 tests in total)

## 1.3.0 - 2026-10-23
- Orders page has a new "New order" button. You can now order any product from any supplier that sells it (cheapest supplier is listed first), not only the products shown on the Reorder page.
- A product with a pending or approved order can no longer be deleted (cancel or receive the order first)
- Clearer hints on the Orders page when there are no orders yet
- Added a unit test for these (16 tests in total)

## 1.2.0 - 2026-10-22
- The Queue is now really used. Each product keeps its batches in a queue (earliest expiry at the head) and stock out takes from the head (FEFO). The Reports page has a new "Sell first (FEFO queue)" table that is built from `fefoQueue()`.
- Added a unit test for the FEFO queue (15 tests in total)
- Updated `docs/COMPLEXITY.md` for the batch queue

## 1.1.2 - 2026-10-22
- Removed `docs/TEST_CASES.md` from the zip. The test cases now live only in the separate "StockTrack Test Cases" document. No code changes.

## 1.1.1 - 2026-10-22
- Fixed the build error "Source option 5 is no longer supported" in NetBeans (Clean and Build). The old Maven compiler plugin that NetBeans uses ignored the Java 17 setting, so `pom.xml` now sets the compiler plugin version and Java 17 directly.

## 1.1.0 - 2026-10-21
- Added MariaDB support. Use it by copying `stocktrack.properties.example` to `stocktrack.properties` (see `docs/DATABASE_SETUP.md`). Without that file the app still uses SQLite.
- Login screen now shows which database is in use
- If the database cannot be reached, the app shows an error message and closes instead of failing quietly
- Empty database no longer reloads the demo data after you delete every product
- Renamed some table columns (`qty_change`, `moved_at`, `order_date`) because `change` is a reserved word in MariaDB. **If you used v1.0.0, delete the old `stocktrack.db` before running v1.1.0.**
- Added test cases TC-32 and TC-33 for MariaDB (in the test cases document)

## 1.0.0 - 2026-10-21 (first versioned release)
- Login with Owner and Staff roles
- Side menu: Inventory, Reorder, Suppliers, Orders, Reports, History
- Inventory: search, stock in with expiry, stock out (earliest expiry first), undo, add, edit, delete, discard expired stock
- Reorder suggestions (Reorder Point and EOQ) with cheapest supplier
- Suppliers and price comparison, purchase orders (pending, approved, received, cancelled)
- Reports: best sellers, slow movers, ABC summary, find by exact name (binary search)
- History of all stock movements
- SQLite database (`stocktrack.db`) saved automatically
- Docs: complexity table, performance test, interview template, test cases (later moved out of the zip in 1.1.2)
- Benchmark.java for scalability tests

## Before 1.0.0 (internal builds, not versioned)
- Build 1: console starter with the core data structures and algorithms
- Build 2: first Swing dashboard
- Build 3: login and new design
- Build 4: reorder, suppliers, orders, reports, history, batches and expiry (FEFO)
- Build 5: side menu
- Build 6: edit product, discard expired, cancel order, benchmark
- Build 7: SQLite database instead of a text file
