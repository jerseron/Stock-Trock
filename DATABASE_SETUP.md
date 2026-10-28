# Database setup

StockTrack works with **two databases**. You choose by using (or not using) one small file.

| | SQLite (default) | MariaDB |
|---|---|---|
| Setup | None | Install MariaDB and create a database (steps below) |
| Data location | File `stocktrack.db` in the project folder | MariaDB server |
| Use when | Testing, or running on any computer | The course or panel wants a server database |

If there is **no** `stocktrack.properties` file, the app uses SQLite. Nothing else to do.

## Use MariaDB

### 1. Install and start MariaDB
- Easiest on Windows: install **XAMPP**, then press **Start** next to MySQL (it is MariaDB). Or install MariaDB from mariadb.org.

### 2. Create the database and user
Open the MariaDB console (in XAMPP, click **Shell** and type `mysql -u root`), then run:
```sql
CREATE DATABASE stocktrack CHARACTER SET utf8mb4;
CREATE USER 'stocktrack'@'localhost' IDENTIFIED BY 'stocktrack123';
GRANT ALL PRIVILEGES ON stocktrack.* TO 'stocktrack'@'localhost';
FLUSH PRIVILEGES;
```
(You do not create tables. The app creates them the first time it runs.)

### 3. Tell the app how to connect
In the project folder (the same folder as `pom.xml`), copy `stocktrack.properties.example` to `stocktrack.properties`:
```
db.url=jdbc:mariadb://localhost:3306/stocktrack
db.user=stocktrack
db.password=stocktrack123
```
Quick alternative on XAMPP: use `db.user=root` and leave `db.password=` empty, and skip the `CREATE USER` step (still create the database).

### 4. Run
Run `App.java`. The login screen shows "Database: jdbc:mariadb://..." at the bottom so you can confirm which database is in use.

## Tables the app creates
`products`, `batches`, `transactions`, `suppliers`, `supplier_prices`, `orders`.
View them with HeidiSQL, phpMyAdmin (XAMPP), or DB Browser for SQLite (for the SQLite file).

## Troubleshooting
| Message or problem | What to check |
|---|---|
| "Cannot connect to the database ... Connection refused" | MariaDB is not running. Start it (XAMPP: Start MySQL). Check the port in `db.url`. |
| "Access denied for user" | Wrong `db.user` or `db.password`, or the user was not created. |
| "Unknown database 'stocktrack'" | Run `CREATE DATABASE stocktrack;` |
| "Could not create the tables" after upgrading from v1.0.0 | Delete the old `stocktrack.db` file (the table columns were renamed in v1.1.0). |
| Want to reset to the demo data | SQLite: delete `stocktrack.db`. MariaDB: run `DROP DATABASE stocktrack; CREATE DATABASE stocktrack CHARACTER SET utf8mb4;` |

## How it works (for the defense)
The program keeps products, history and orders in memory using the data structures (HashMap, LinkedList, heap, and so on) so searching and sorting are fast. `DataStore.java` copies everything to the database tables inside one transaction after every change, and reads it back when the app starts. Because all the database code is in `DataStore.java`, switching from SQLite to MariaDB only needed the connection settings, not changes to the logic.
