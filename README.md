# Finova — College Club & Event Budget System

Finova is a Java console application for managing college club budgets. It provides role-based access for a Central Admin, Faculty Head and Student Club Head. Club funds are split into a MAIN pool and EVENT team wallets. Faculty Heads record verified sponsor inflows, Student Club Heads allocate budgets and record vendor bills, and the system keeps a transaction ledger in MySQL.

## Project structure

- `src/main/Finova.java` — main console menus and role login flow
- `src/model/` — Club, User, role and account models
- `src/service/FinovaService.java` — authentication and business rules
- `src/dao/` — JDBC data access for clubs, accounts and transactions
- `src/database/DBConnection.java` — MySQL JDBC connection
- `src/concurrency/` — producer-consumer batch claim processing
- `src/exception/` — application-specific exceptions
- `src/io/` — CSV import and export
- `src/util/` — input validation and expense receipt formatting
- `sql/finova_schema.sql` — MySQL database and table definitions
- `data/planned_expenses.csv` — sample planned team expenses
- `data/finova_audit.csv` — generated global transaction export
- `lib/` — MySQL Connector/J driver

## Requirements

- JDK 17 or newer
- MySQL Server 8.0 or newer
- MySQL Workbench (or MySQL command line)
- MySQL Connector/J JAR in the `lib` directory (already included in this project archive)

## Database setup

1. Start MySQL Server.
2. Open MySQL Workbench and connect to your local server.
3. Open `sql/finova_schema.sql` and execute the complete script.
4. The script creates the `finova` database and its four tables, then seeds the Central Admin account.

The Java application reads these environment variables:

- `FINOVA_DB_URL` (default: `jdbc:mysql://localhost:3306/finova?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC`)
- `FINOVA_DB_USER` (default: `root`)
- `FINOVA_DB_PASSWORD` (default: empty)

In Windows PowerShell, set the values for the current terminal session before running:

```powershell
$env:FINOVA_DB_URL = "jdbc:mysql://localhost:3306/finova?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:FINOVA_DB_USER = "root"
$env:FINOVA_DB_PASSWORD = "YOUR_MYSQL_PASSWORD"
.\run.bat
```

Replace `YOUR_MYSQL_PASSWORD` with your local MySQL password. Do not commit real database credentials to source control.

## Run the console application

From the project root folder, run:

```powershell
.\run.bat
```

`run.bat` calls `build.bat`, which compiles all Java source files into `out`, then starts `main.Finova` with the MySQL driver on the classpath. You can also run `build.bat` separately to compile only.

## First-time application flow

1. Select `1. Central Admin`.
2. Demo credentials: username `admin`, password `admin123`.
3. Register a club and assign a Faculty Head username/password.
4. Log out and select Faculty Head. Enter the Club ID and Faculty credentials created in step 3.
5. Create one or more team wallets, record a sponsor/grant inflow into the Main Fund, and assign Student Club Head credentials.
6. Log out and select Student Club Head. Enter the same Club ID and the student username/password created by the Faculty Head.
7. Allocate money from the Main Pool to a team, pay a vendor bill, run batch claims and view spending summaries.

Account numbers such as `FV-1001` are generated internally. Users select wallets from numbered lists and do not need to enter those numbers manually.

## Planned-expense CSV

`data/planned_expenses.csv` uses this format:

```csv
team_name,amount,description
Web Development Team,1250.00,Purchase of cables and adapters
Design Team,850.00,Printing of event posters
Logistics Team,2100.00,Transport and setup supplies
```

The `team_name` must match an EVENT wallet name in the currently logged-in club. The sample rows are examples; create those team names in the database before importing them, or edit the CSV to match your club's team names. CSV import is restricted to the logged-in club.

## Main workflows

### Central Admin

- Register clubs and create their zero-balance MAIN fund.
- Toggle account status using a numbered list.
- View club, faculty/student lead, wallet balances and sponsor inflows.
- Export the full transaction ledger to `data/finova_audit.csv`.

### Faculty Head

- Create EVENT team wallets for the authenticated club.
- Record sponsor/grant inflows using a unique UTR/reference ID.
- Assign/reset Student Club Head credentials.
- View only the authenticated club's accounts, sponsor history and vendor expenses.

### Student Club Head

- View the club Main Pool and its EVENT wallets.
- Allocate funds from the Main Pool to a selected team wallet. The optional Y/N network-drop simulation demonstrates JDBC rollback.
- Pay vendor bills and print a Finova expense receipt.
- Run concurrent batch claims against a selected team wallet.
- View team/monthly spending summaries and import planned expenses from CSV.

## Important implementation notes

- The application uses transactions and row-level locks for budget allocation, sponsor inflow, vendor payments and batch-claim persistence.
- Batch claims also use synchronized access to the shared in-memory account object so parallel claim threads cannot spend the same balance twice.
- `transactions.bank_ref_id` is UNIQUE. MySQL rejects a repeated UTR/reference ID; the duplicate insert is rolled back with the associated sponsor credit.
- Failed-login counters and block status are stored per user. Three incorrect password attempts block that user; a successful login resets the counter.
- Student users can be unblocked by a database administrator in MySQL Workbench with `UPDATE users SET failed_attempts = 0, is_blocked = FALSE WHERE username = 'student.lead';` (replace the example username).
- Central Admin demo credentials are `admin` / `admin123`.
- Passwords are SHA-256 hashed in the database; for production use, replace this with a salted adaptive password hash and harden audit/access controls.

## Testing status

The Java source is intended for Java 17. Run `build.bat` on Windows to compile the whole project. JDBC operations require a reachable MySQL Server with `finova_schema.sql` installed; those database workflows should be tested on the local machine after configuring the environment variables.
