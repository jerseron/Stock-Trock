# StockTrack (Java, NetBeans)

Inventory and supply chain system for small retail stores. Swing desktop app.

**Current version: 2.0.0** (see `CHANGELOG.md`). The version is shown on the login screen and in the window title.

## Open in NetBeans
File > Open Project > select the `StockTrack` folder (Maven project, needs JDK 17+ and internet the first time, so Maven can download the SQLite driver).
Run: right-click project > Run (starts `App.java`, login first). Test: right-click project > Test.
`Main.java` is a console version for testing logic only.

## Demo logins
- `owner` / `owner123`: full access (add/delete products, add suppliers, approve orders)
- `staff` / `staff123`: no add/delete, cannot approve orders

## Side menu pages
- **Inventory:** search, stock in (with expiry), stock out (earliest expiry first), undo, add, edit, delete, discard expired stock (not counted as sales), and a Samples button that loads 1,000 test products
- **Suppliers:** supplier list and price comparison
- **Orders:** the system suggests what to order (low stock, with the cheapest supplier and the quantity from EOQ). Order one or all, or use New order for any product. Then approve, receive, or cancel orders.
- **Sales:** sales per day (with a chart) and per month
- **Reports:** best sellers, slow movers, FEFO queue (which products to sell first), ABC summary, find product by exact name
- **History:** every stock in and stock out

Data is saved automatically to a database. By default it is an SQLite file, `stocktrack.db`, created in the project folder the first time you run the app. To use MariaDB instead, follow `docs/DATABASE_SETUP.md`. Delete that file to reset to the demo data. You can open it with DB Browser for SQLite to see the tables: products, batches, transactions, suppliers, supplier_prices, orders.

## Data structures and algorithms
Our own code (no `java.util` collections for the core): Hash Table (`MyHashMap`), Linked List (`MyLinkedList`), Stack (`MyStack`), Queue (`MyQueue`, FEFO), Min-Heap (`MyMinHeap`), Merge Sort, Binary Search, KMP string search (`StringSearch`), Reorder Point, EOQ, ABC analysis.
See `docs/COMPLEXITY.md` for Big-O.

## Versions
Every zip is named `StockTrack_v<version>.zip`. When you change the code, update `Version.java`, the version in `pom.xml`, and add a line to `CHANGELOG.md`.

## For documentation
- `docs/DATABASE_SETUP.md`: how to use SQLite or MariaDB
- `docs/COMPLEXITY.md`: time and space complexity table
- Test cases are not in this zip. They are kept in a separate document ("StockTrack Test Cases") so the team can fill in the results online.
- `docs/PERFORMANCE.md`: scalability results (run `Benchmark.java` and fill in your own numbers)
- `docs/INTERVIEW_TEMPLATE.md`: fill in after talking to a real store

## Start Git (required by the course)
```
git init
git add .
git commit -m "Initial StockTrack project"
```
Then push to a GitHub repo and commit as you work. Each member should commit their own parts.

## Still to do by the team
- Interview a real store and fill in `docs/INTERVIEW_TEMPLATE.md`
- Add your own screenshots and final report
- Optional: change the colors in `Theme.java`
