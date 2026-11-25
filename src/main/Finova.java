package main;

import concurrency.BatchClaimRunner;
import concurrency.ClaimQueue;
import concurrency.ExpenseClaimTask;
import model.AccountStatus;
import model.ClubAccount;
import service.FinovaService;
import util.InputValidator;

import java.math.BigDecimal;
import java.util.List;
import java.util.Scanner;
import io.CsvExporter;
import io.CsvImporter;
import model.TransactionRecord;
import java.io.IOException;
import java.util.List;

// Example: exporting records from your list or database

public class Finova {
    private static final FinovaService service = new FinovaService();

    public static void main(String[] args) {
        try {
            service.syncFromDatabase();
        } catch (Exception e) {
            System.out.println("[Warning] Could not pre-sync from DB yet: " + e.getMessage());
        }

        try (Scanner scanner = new Scanner(System.in)) {
            boolean running = true;
            while (running) {
                printMainMenu();
                String choice = scanner.nextLine().trim();
                try {
                    switch (choice) {
                        case "1" -> studentMenu(scanner);
                        case "2" -> facultyMenu(scanner);
                        case "3" -> adminMenu(scanner);
                        case "0" -> {
                            running = false;
                            System.out.println("Exiting FestVault. Goodbye!");
                        }
                        default -> System.out.println("Invalid option. Please enter 0, 1, 2, or 3.");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Invalid numeric input: " + e.getMessage());
                } catch (Exception e) {
                    System.out.println("[Error] " + e.getMessage());
                }
            }
        }
        CsvImporter importer = new CsvImporter();
        CsvImporter.ImportResult result = importer.importAndRecordExpenses("planned_expenses.csv", service);

        System.out.println("Recorded expenses: " + result.validExpenses().size());
        for (String err : result.errors()) {
            System.out.println("[CSV Notice] " + err);
        }
        CsvExporter exporter = new CsvExporter();
        List<TransactionRecord> records = service.getMemoryStore().getTopExpensesDescending();
        try{
            exporter.exportTransactions("fest_audit.csv", records);
            System.out.println("Exported audit log to fest_audit.csv");
        }catch(Exception e){
            System.out.println("Error in Exporting");
        }
    }

    private static void printMainMenu() {
        System.out.println("\n============================================================");
        System.out.println("         FINOVA - COLLEGE FEST MANAGEMENT PLATFORM (CA-3)");
        System.out.println("============================================================");
        System.out.println("1. Student Club Head Menu");
        System.out.println("2. Faculty Head Menu");
        System.out.println("3. Central Admin Menu");
        System.out.println("0. Exit");
        System.out.print("Enter your choice: ");
    }

    private static void adminMenu(Scanner scanner) {
        System.out.println("\n--- CENTRAL ADMIN MENU ---");
        System.out.println("1. Create Club & Main Account");
        System.out.println("2. Add Sponsor Funds");
        System.out.println("3. Freeze / Activate Account");
        System.out.println("4. View All Accounts");
        System.out.print("Choice: ");
        switch (scanner.nextLine().trim()) {
            case "1" -> {
                System.out.print("Club ID (e.g. CLB-01): ");
                String clubId = scanner.nextLine().trim();
                System.out.print("Club Name: ");
                String clubName = scanner.nextLine().trim();
                System.out.print("Main Account No (e.g. FV-1001): ");
                String accNo = scanner.nextLine().trim();
                System.out.print("Main Account Name: ");
                String accName = scanner.nextLine().trim();
                service.createClubWithMainAccount(clubId, clubName, accNo, accName);
                System.out.println("Club and Main Account created.");
            }
            case "2" -> {
                System.out.print("Target Main Account No: ");
                String accNo = scanner.nextLine().trim();
                System.out.print("Sponsor Name: ");
                String sponsor = scanner.nextLine().trim();
                System.out.print("Bank Reference ID: ");
                String ref = scanner.nextLine().trim();
                System.out.print("Amount (Rs.): ");
                BigDecimal amt = new BigDecimal(scanner.nextLine().trim());
                service.addSponsorFunds(accNo, sponsor, ref, amt);
                System.out.println("Sponsor funds credited.");
            }
            case "3" -> {
                System.out.print("Account No: ");
                String accNo = scanner.nextLine().trim();
                System.out.print("Status (ACTIVE / INACTIVE): ");
                AccountStatus status = AccountStatus.valueOf(scanner.nextLine().trim().toUpperCase());
                service.setAccountStatus(accNo, status);
                System.out.println("Account status updated to " + status);
            }
            case "4" -> service.getAccountDAO().findAll().forEach(Finova::printAccount);
            default -> System.out.println("Invalid admin option.");
        }
    }

    private static void facultyMenu(Scanner scanner) {
        System.out.println("\n--- FACULTY HEAD MENU ---");
        System.out.println("1. Create Event / Sub-Team Account");
        System.out.println("2. View Club Accounts");
        System.out.print("Choice: ");
        switch (scanner.nextLine().trim()) {
            case "1" -> {
                System.out.print("Event Account No (e.g. FV-1002): ");
                String accNo = scanner.nextLine().trim();
                System.out.print("Club ID (e.g. CLB-01): ");
                String clubId = scanner.nextLine().trim();
                System.out.print("Event / Sub-Team Name: ");
                String name = scanner.nextLine().trim();
                service.createEventAccount(accNo, clubId, name);
                System.out.println("Event sub-account created.");
            }
            case "2" -> {
                System.out.print("Club ID: ");
                String clubId = scanner.nextLine().trim();
                service.getAccountDAO().findByClubId(clubId).forEach(Finova::printAccount);
            }
            default -> System.out.println("Invalid faculty option.");
        }
    }

    private static void studentMenu(Scanner scanner) throws Exception {
        System.out.print("Enter 4-digit PIN to continue: ");
        String pin = scanner.nextLine().trim();
        if (!InputValidator.isValidPin(pin)) {
            System.out.println("Invalid PIN format. Must be 4 digits.");
            return;
        }

        System.out.println("\n--- STUDENT CLUB HEAD MENU ---");
        System.out.println("1. Own-Club Sub-Team Transfer (Atomic JDBC)");
        System.out.println("2. Record Vendor Expense (Receipt)");
        System.out.println("3. Run Concurrent Batch Claims Demo");
        System.out.println("4. View Monthly Spend & Top Expenses (Collections)");
        System.out.print("Choice: ");
        switch (scanner.nextLine().trim()) {
            case "1" -> {
                System.out.print("From Main Account: ");
                String from = scanner.nextLine().trim();
                System.out.print("To Event Account: ");
                String to = scanner.nextLine().trim();
                System.out.print("Amount (Rs.): ");
                BigDecimal amt = new BigDecimal(scanner.nextLine().trim());
                System.out.print("Simulate mid-transfer failure? (true/false): ");
                boolean simFail = Boolean.parseBoolean(scanner.nextLine().trim());
                service.transferOwnClubFunds(from, to, amt, simFail);
                System.out.println("Transfer completed.");
            }
            case "2" -> {
                System.out.print("Account No: ");
                String accNo = scanner.nextLine().trim();
                System.out.print("Amount (Rs.): ");
                BigDecimal amt = new BigDecimal(scanner.nextLine().trim());
                System.out.print("Purpose / Vendor: ");
                String purpose = scanner.nextLine().trim();
                String receipt = service.recordVendorExpense(accNo, amt, purpose);
                System.out.println(receipt);
            }
            case "3" -> {
                System.out.print("Target Event Account No: ");
                String accNo = scanner.nextLine().trim();
                ClubAccount acc = service.getAccountDAO().findById(accNo)
                        .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accNo));
                List<ExpenseClaimTask> tasks = List.of(
                        new ExpenseClaimTask(service.getClaimManager(), acc, "CLM-1", "Design Team",
                                new BigDecimal("1500.00")),
                        new ExpenseClaimTask(service.getClaimManager(), acc, "CLM-2", "Logistics Team",
                                new BigDecimal("1500.00")),
                        new ExpenseClaimTask(service.getClaimManager(), acc, "CLM-3", "PR Team",
                                new BigDecimal("1500.00")));
                int approved = BatchClaimRunner.runBatch(new ClaimQueue(5), tasks);
                service.getAccountDAO().updateBalanceAndStatus(acc);
                System.out.println("Batch Approved Count (Future.get()): " + approved);
            }
            case "4" -> {
                System.out.println("Monthly Spend Totals: " + service.getMemoryStore().getMonthlySpendTotals());
                System.out.println("Top Expenses: " + service.getMemoryStore().getTopExpensesDescending());
            }
            default -> System.out.println("Invalid student option.");
        }
    }

    private static void printAccount(ClubAccount a) {
        System.out.printf("%s | %s (%s) | Club: %s | Bal: Rs. %s | Locked: Rs. %s | Spendable: Rs. %s | %s%n",
                a.getAccountNo(), a.getAccountName(), a.getAccountType(),
                a.getClubId(), a.getBalance(), a.getLockedAmount(),
                a.getSpendableBalance(), a.getStatus());
    }
}
