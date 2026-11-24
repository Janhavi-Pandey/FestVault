# FestVault — College Fest & Club Budget & Expense Control System (CA-3 Prototype)

## Project Structure (VS Code Java Project)
- `src/model/` — `User` and `ClubAccount` hierarchies, `AccountStatus`, `TransactionRecord`
- `src/exception/` — Custom runtime exceptions (`InvalidPinException`, `AccountBlockedException`, `InactiveAccountException`, `InsufficientBalanceException`, `UnauthorizedTransferException`)
- `src/service/` — `Spendable`, `InMemoryLedgerStore`, `FestVaultService`
- `src/concurrency/` — `ExpenseClaimTask`, `ExpenseClaimManager`, `ClaimQueue`, `ClaimProducer`, `BatchClaimRunner`
- `src/repository/` — Generic `Repository<T, ID>` contract
- `src/dao/` — `ClubDAO`, `AccountDAO`, `TransactionDAO`
- `src/database/` — `DBConnection`
- `src/util/` — `InputValidator`, `ReceiptFormatter`
- `src/main/` — `FestVaultApp` console entry point
- `sql/festvault_schema.sql` — MySQL relational database schema
- `.vscode/settings.json` — VS Code Java source/output/library configuration

## Setup & Run in VS Code
1. Open the `FestVault_CA3` folder in VS Code (with **Extension Pack for Java** installed).
2. Download **MySQL Connector/J** (`mysql-connector-j-8.x.x.jar`) and place it inside `lib/`.
3. Execute `sql/festvault_schema.sql` in MySQL Workbench or MySQL CLI.
4. Update `USER` and `PASSWORD` inside `src/database/DBConnection.java`.
5. Open `src/main/FestVaultApp.java` and click **Run** (or compile via PowerShell):
   ```powershell
   javac -cp "lib/*" -d bin (Get-ChildItem -Recurse -Filter *.java src).FullName
   java -cp "bin;lib/*" main.FestVaultApp
   ```
