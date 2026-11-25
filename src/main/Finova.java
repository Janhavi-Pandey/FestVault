package main;

import io.CsvImporter;
import model.AccountStatus;
import model.CentralAdmin;
import model.Club;
import model.ClubAccount;
import model.EventSubAccount;
import model.FacultyHead;
import model.MainClubAccount;
import model.StudentClubHead;
import model.TransactionRecord;
import service.FinovaService;

import java.io.IOException;
import java.sql.Connection;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/** Console entry point for the Finova College Club & Event Budget System. */
public class Finova {
    private static final FinovaService service = new FinovaService();

    public static void main(String[] args) {
        try {
            try (Connection connection = database.DBConnection.getConnection()) {
                if (!connection.isValid(3)) throw new IllegalStateException("The database connection is not valid.");
                System.out.println("Database connection ready.");
            }
        } catch (Exception e) {
            System.out.println("Database connection failed: " + e.getMessage());
            System.out.println("Run sql/finova_schema.sql in MySQL Workbench and check your DB settings.");
            return;
        }

        try (Scanner scanner = new Scanner(System.in)) {
            boolean running = true;
            while (running) {
                printMainMenu();
                String choice = scanner.nextLine().trim();
                try {
                    switch (choice) {
                        case "1" -> centralAdminLogin(scanner);
                        case "2" -> facultyLogin(scanner);
                        case "3" -> studentLogin(scanner);
                        case "0" -> {
                            running = false;
                            System.out.println("Thank you for using Finova. Goodbye!");
                        }
                        default -> System.out.println("Please select 0, 1, 2 or 3.");
                    }
                } catch (Exception e) {
                    System.out.println("[Notice] " + e.getMessage());
                }
            }
        }
    }

    private static void printMainMenu() {
        System.out.println("\n====================================================================");
        System.out.println("           FINOVA — COLLEGE CLUB & EVENT BUDGET SYSTEM");
        System.out.println("====================================================================");
        System.out.println("Select Your Role:");
        System.out.println("  1. Central Admin      (Register Clubs, Manage Status & Global Audit)");
        System.out.println("  2. Faculty Head       (Create Teams, Log QR Sponsors & Set Student Lead)");
        System.out.println("  3. Student Club Head  (Allocate Team Budgets, Pay Bills & Track Spend)");
        System.out.println("  0. Exit");
        System.out.print("Enter choice: ");
    }

    private static void centralAdminLogin(Scanner scanner) {
        System.out.println("\n--- CENTRAL ADMIN LOGIN ---");
        String username = readText(scanner, "Admin Username", "admin");
        String password = readText(scanner, "Admin Password", "admin123");
        CentralAdmin admin = service.loginAdmin(username, password);
        try {
            System.out.println("Welcome, " + admin.getName() + ".");
            centralAdminMenu(scanner);
        } finally {
            service.logout();
        }
    }

    private static void facultyLogin(Scanner scanner) {
        System.out.println("\n--- FACULTY HEAD LOGIN ---");
        String clubId = readText(scanner, "Club ID", "CLB-01");
        String username = readText(scanner, "Faculty Username", "faculty.sharma");
        String password = readText(scanner, "Faculty Password", "Faculty@123");
        FacultyHead faculty = service.loginFaculty(clubId, username, password);
        try {
            System.out.println("Login successful.");
            facultyMenu(scanner, faculty);
        } finally {
            service.logout();
        }
    }

    private static void studentLogin(Scanner scanner) {
        System.out.println("\n--- STUDENT CLUB HEAD LOGIN ---");
        String clubId = readText(scanner, "Club ID", "CLB-01");
        String username = readText(scanner, "Student Username", "student.lead");
        String password = readText(scanner, "Student Password", "Student@123");
        StudentClubHead student = service.loginStudent(clubId, username, password);
        try {
            System.out.println("Login successful.");
            studentMenu(scanner, student);
        } finally {
            service.logout();
        }
    }

    private static void centralAdminMenu(Scanner scanner) {
        boolean loggedIn = true;
        while (loggedIn) {
            System.out.println("\n--- CENTRAL ADMIN MENU ---");
            System.out.println("1. Register New Club & Assign Faculty Head");
            System.out.println("2. Freeze / Activate an Account");
            System.out.println("3. View All Clubs, Balances & Sponsor Directory");
            System.out.println("4. Export Global Audit Report (CSV)");
            System.out.println("0. Logout");
            System.out.print("Enter choice: ");
            try {
                switch (scanner.nextLine().trim()) {
                    case "1" -> registerClub(scanner);
                    case "2" -> toggleAccount(scanner);
                    case "3" -> viewGlobalDirectory();
                    case "4" -> exportGlobalAudit();
                    case "0" -> loggedIn = false;
                    default -> System.out.println("Invalid option. Please try again.");
                }
            } catch (Exception e) {
                System.out.println("[Notice] " + e.getMessage());
            }
        }
        System.out.println("Central Admin logged out.");
    }

    private static void registerClub(Scanner scanner) {
        System.out.println("\n--- REGISTER NEW CLUB ---");
        String clubId = readText(scanner, "Club ID", "CLB-01");
        String clubName = readText(scanner, "Club Name", "Coding Club");
        String facultyName = readText(scanner, "Faculty Name", "Dr. Sharma");
        String facultyUsername = readText(scanner, "Faculty Username", "faculty.sharma");
        String facultyPassword = readText(scanner, "Faculty Password", "Faculty@123");
        service.registerClubWithFaculty(clubId, clubName, facultyName, facultyUsername, facultyPassword);
        System.out.println("Club registered. Its MAIN fund was created with Rs. 0.00 and status ACTIVE.");
    }

    private static void toggleAccount(Scanner scanner) {
        List<ClubAccount> accounts = service.getAllAccounts();
        if (accounts.isEmpty()) {
            System.out.println("No accounts have been registered yet.");
            return;
        }
        Map<String, String> clubNames = clubNameMap(service.getAllClubs());
        System.out.println("\nSelect an account to toggle its status:");
        printAccountList(accounts, clubNames);
        System.out.println("  0. Back");
        int selected = readInt(scanner, "Account selection number", 0, accounts.size());
        if (selected == 0) return;
        ClubAccount account = accounts.get(selected - 1);
        AccountStatus newStatus = service.toggleAccountStatus(account.getAccountNo());
        System.out.println(account.getAccountName() + " is now " + newStatus + ".");
    }

    private static void viewGlobalDirectory() {
        System.out.println("\n================ CLUB DIRECTORY & BALANCES ================");
        List<Club> clubs = service.getAllClubs();
        for (Club club : clubs) {
            System.out.println("\n" + club.getClubName() + " (" + club.getClubId() + ")");
            System.out.println("Faculty Head: " + club.getFacultyName());
            System.out.println("Student Lead: " + (club.getStudentName() == null ? "Not assigned" : club.getStudentName()));
            List<ClubAccount> accounts = service.getClubAccounts(club.getClubId());
            for (ClubAccount account : accounts) {
                String label = account instanceof MainClubAccount ? "Main Pool" : "Team Wallet";
                System.out.printf("  %-11s | %-30s | Rs. %-12s | %s%n",
                        label, account.getAccountName(), account.getBalance(), account.getStatus());
            }
        }
        System.out.println("\n================ VERIFIED SPONSOR / GRANT INFLOWS ================");
        Map<String, String> names = clubNameMap(clubs);
        List<TransactionRecord> sponsors = service.getAllSponsorInflows();
        if (sponsors.isEmpty()) System.out.println("No sponsor inflows recorded yet.");
        for (TransactionRecord txn : sponsors) {
            System.out.printf("Sponsor: %s | Club: %s | UTR: %s | Amount: Rs. %s%n",
                    txn.getSponsorName(), names.getOrDefault(txn.getClubId(), txn.getClubId()),
                    txn.getBankRefId(), txn.getAmount());
        }
    }

    private static void exportGlobalAudit() {
        try {
            int recordCount = service.exportGlobalAuditCsv();
            System.out.println("Global audit exported to data/finova_audit.csv (" + recordCount + " records).");
        } catch (IOException e) {
            System.out.println("Could not export audit report: " + e.getMessage());
        }
    }

    private static void facultyMenu(Scanner scanner, FacultyHead faculty) {
        boolean loggedIn = true;
        while (loggedIn) {
            System.out.println("\n--- FACULTY HEAD: " + faculty.getName() + " | " + faculty.getClubName() + " ---");
            System.out.println("1. Create Team / Event Sub-Account");
            System.out.println("2. Log Sponsor / Grant Inflow (via Club QR / UTR Reference)");
            System.out.println("3. Assign / Reset Student Club Head Credentials");
            System.out.println("4. View My Club's Team Balances, Expenses & Sponsor History");
            System.out.println("0. Logout");
            System.out.print("Enter choice: ");
            try {
                switch (scanner.nextLine().trim()) {
                    case "1" -> createTeam(scanner, faculty.getClubId());
                    case "2" -> logSponsorInflow(scanner, faculty.getClubId());
                    case "3" -> assignStudentHead(scanner, faculty.getClubId());
                    case "4" -> viewFacultyClub(faculty.getClubId());
                    case "0" -> loggedIn = false;
                    default -> System.out.println("Invalid option. Please try again.");
                }
            } catch (Exception e) { System.out.println("[Notice] " + e.getMessage()); }
        }
        System.out.println("Faculty Head logged out.");
    }

    private static void createTeam(Scanner scanner, String clubId) {
        List<ClubAccount> existing = service.getClubAccounts(clubId);
        System.out.println("\nExisting team wallets:");
        printEventAccounts(existing);
        String teamName = readText(scanner, "New Team / Event Name", "Web Development Team");
        ClubAccount created = service.createTeamSubAccount(clubId, teamName);
        System.out.println("Team wallet created: " + created.getAccountName() + " | Opening balance Rs. 0.00 | ACTIVE");
    }

    private static void logSponsorInflow(Scanner scanner, String clubId) {
        String sponsor = readText(scanner, "Sponsor Name", "ABC Technologies Pvt Ltd");
        String utr = readText(scanner, "UPI UTR / Bank Reference ID", "UTR20261125001");
        BigDecimal amount = readAmount(scanner, "Amount");
        service.logSponsorInflow(clubId, sponsor, utr, amount);
        System.out.println("Sponsor inflow verified and credited to your club's MAIN fund.");
    }

    private static void assignStudentHead(Scanner scanner, String clubId) {
        String name = readText(scanner, "Student Lead Name", "Aarav Sharma");
        String username = readText(scanner, "Student Username", "student.lead");
        String password = readText(scanner, "Student Password", "Student@123");
        service.assignOrResetStudentHead(clubId, name, username, password);
        System.out.println("Student Club Head credentials saved. Previous failed-login count was reset.");
    }

    private static void viewFacultyClub(String clubId) {
        Club club = service.getClub(clubId);
        System.out.println("\nClub: " + club.getClubName());
        for (ClubAccount account : service.getClubAccounts(clubId)) {
            System.out.printf("%-30s | %-8s | Rs. %-12s | %s%n", account.getAccountName(),
                    account.getAccountType(), account.getBalance(), account.getStatus());
        }
        System.out.println("\nSponsor / grant history:");
        List<TransactionRecord> transactions = service.getClubTransactions(clubId);
        boolean foundSponsor = false;
        for (TransactionRecord txn : transactions) {
            if ("SPONSOR_INFLOW".equals(txn.getTxnType())) {
                foundSponsor = true;
                System.out.printf("%s | %s | UTR %s | Rs. %s | %s%n", txn.getSponsorName(),
                        txn.getTxnDate(), txn.getBankRefId(), txn.getAmount(), txn.getStatus());
            }
        }
        if (!foundSponsor) System.out.println("No sponsor inflows recorded.");
        System.out.println("\nVendor expenses:");
        boolean foundExpense = false;
        for (TransactionRecord txn : transactions) {
            if ("VENDOR_EXPENSE".equals(txn.getTxnType())) {
                foundExpense = true;
                System.out.printf("%s | Rs. %s | %s | %s%n", txn.getTxnDate(), txn.getAmount(), txn.getStatus(), txn.getPurpose());
            }
        }
        if (!foundExpense) System.out.println("No vendor expenses recorded.");
    }

    private static void studentMenu(Scanner scanner, StudentClubHead student) {
        boolean loggedIn = true;
        while (loggedIn) {
            System.out.println("\n--- STUDENT CLUB HEAD: " + student.getName() + " | " + student.getClubName() + " ---");
            System.out.println("1. View Club & Team Balances");
            System.out.println("2. Team Budget Allocation & Vendor Expense");
            System.out.println("3. Process Batch Team Claims");
            System.out.println("4. View Spending Summary & Import Planned Expenses (CSV)");
            System.out.println("0. Logout");
            System.out.print("Enter choice: ");
            try {
                switch (scanner.nextLine().trim()) {
                    case "1" -> viewStudentBalances(student.getClubId());
                    case "2" -> budgetAndExpenseMenu(scanner, student);
                    case "3" -> processBatchClaims(scanner, student.getClubId());
                    case "4" -> spendingAndImportMenu(scanner, student.getClubId());
                    case "0" -> loggedIn = false;
                    default -> System.out.println("Invalid option. Please try again.");
                }
            } catch (Exception e) { System.out.println("[Notice] " + e.getMessage()); }
        }
        System.out.println("Student Club Head logged out.");
    }

    private static void viewStudentBalances(String clubId) {
        System.out.println("\n--- CLUB & TEAM BALANCES ---");
        for (ClubAccount account : service.getClubAccounts(clubId)) {
            if (account instanceof MainClubAccount) {
                System.out.printf("Main Pool: Rs. %s | %s%n", account.getBalance(), account.getStatus());
            } else {
                System.out.printf("Team: %-30s | Rs. %-12s | %s%n", account.getAccountName(), account.getBalance(), account.getStatus());
            }
        }
    }

    private static void budgetAndExpenseMenu(Scanner scanner, StudentClubHead student) {
        boolean back = false;
        while (!back) {
            List<ClubAccount> accounts = service.getClubAccounts(student.getClubId());
            ClubAccount main = accounts.stream().filter(a -> a instanceof MainClubAccount).findFirst()
                    .orElseThrow(() -> new IllegalStateException("Club Main Pool was not found."));
            List<ClubAccount> teams = eventAccounts(accounts);
            System.out.println("\n--- TEAM BUDGET & VENDOR EXPENSE ---");
            System.out.println("Main Pool Balance: Rs. " + main.getBalance() + " | " + main.getStatus());
            System.out.println("Team Wallets:");
            printEventAccounts(teams);
            System.out.println("1. Allocate Funds from Main Pool to a Team");
            System.out.println("2. Pay Vendor Bill & Print Receipt (from Team Wallet)");
            System.out.println("0. Back");
            System.out.print("Enter choice: ");
            switch (scanner.nextLine().trim()) {
                case "1" -> {
                    ClubAccount selected = selectTeam(scanner, teams);
                    if (selected == null) continue;
                    BigDecimal amount = readAmount(scanner, "Allocation Amount (Rs.)");
                    boolean simulateDrop = readYesNo(scanner, "Simulate a network interruption? (Y/N)");
                    service.transferMainToTeam(student.getClubId(), selected.getAccountNo(), amount, simulateDrop);
                    System.out.println("Budget allocation completed. Main Pool and team wallet were updated together.");
                }
                case "2" -> {
                    ClubAccount selected = selectTeam(scanner, teams);
                    if (selected == null) continue;
                    BigDecimal amount = readAmount(scanner, "Bill Amount (Rs.)");
                    String description = readText(scanner, "Expense Description", "Stage lighting rental");
                    String receipt = service.recordVendorExpense(student.getClubId(), selected.getAccountNo(), amount, description);
                    System.out.println(receipt);
                }
                case "0" -> back = true;
                default -> System.out.println("Invalid option. Please try again.");
            }
        }
    }

    private static void processBatchClaims(Scanner scanner, String clubId) throws Exception {
        List<ClubAccount> teams = eventAccounts(service.getClubAccounts(clubId));
        ClubAccount team = selectTeam(scanner, teams);
        if (team == null) return;
        BigDecimal amount = readAmount(scanner, "Claim Amount per Coordinator (Rs.)");
        int threadCount = readInt(scanner, "Thread Count", 1, 100);
        int approved = service.processBatchClaims(clubId, team.getAccountNo(), amount, threadCount);
        System.out.println("Batch completed: " + approved + " of " + threadCount + " claims approved.");
        System.out.println("Rejected claims were recorded as FAILED. Remaining balance: Rs. " +
                service.getClubAccounts(clubId).stream().filter(a -> a.getAccountNo().equals(team.getAccountNo())).findFirst().orElseThrow().getBalance());
    }

    private static void spendingAndImportMenu(Scanner scanner, String clubId) {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- SPENDING SUMMARY & CSV IMPORT ---");
            System.out.println("1. View Team-Wise & Monthly Spending Breakdown");
            System.out.println("2. Import Planned Bills from data/planned_expenses.csv");
            System.out.println("0. Back");
            System.out.print("Enter choice: ");
            switch (scanner.nextLine().trim()) {
                case "1" -> {
                    System.out.println("Team-wise successful spending (account number is kept internal):");
                    Map<String, BigDecimal> teamSpend = service.getTeamWiseSpending(clubId);
                    if (teamSpend.isEmpty()) System.out.println("No successful team expenses recorded.");
                    teamSpend.forEach((name, total) -> System.out.println(name + " : Rs. " + total));
                    System.out.println("Monthly spending:");
                    Map<String, BigDecimal> monthly = service.getMonthlySpending(clubId);
                    if (monthly.isEmpty()) System.out.println("No monthly spending recorded.");
                    monthly.forEach((month, total) -> System.out.println(month + " : Rs. " + total));
                }
                case "2" -> {
                    CsvImporter.ImportResult result = new CsvImporter().importAndRecordExpenses(clubId, service);
                    System.out.println("Planned expense rows processed successfully: " + result.validExpenses().size());
                    for (String error : result.errors()) System.out.println("[CSV Notice] " + error);
                }
                case "0" -> back = true;
                default -> System.out.println("Invalid option. Please try again.");
            }
        }
    }

    private static ClubAccount selectTeam(Scanner scanner, List<ClubAccount> teams) {
        if (teams.isEmpty()) throw new IllegalStateException("No team wallets exist. Ask your Faculty Head to create a team first.");
        System.out.println("Select a team wallet:");
        printEventAccounts(teams);
        System.out.println("  0. Back");
        int selected = readInt(scanner, "Team selection number", 0, teams.size());
        if (selected == 0) return null;
        return teams.get(selected - 1);
    }

    private static void printEventAccounts(List<ClubAccount> accounts) {
        List<ClubAccount> teams = eventAccounts(accounts);
        if (teams.isEmpty()) System.out.println("  No team wallets created yet.");
        for (int i = 0; i < teams.size(); i++) {
            ClubAccount account = teams.get(i);
            System.out.printf("  %d. %-30s | Balance: Rs. %-12s | %s%n",
                    i + 1, account.getAccountName(), account.getBalance(), account.getStatus());
        }
    }

    private static List<ClubAccount> eventAccounts(List<ClubAccount> accounts) {
        List<ClubAccount> teams = new ArrayList<>();
        for (ClubAccount account : accounts) if (account instanceof EventSubAccount) teams.add(account);
        return teams;
    }

    private static void printAccountList(List<ClubAccount> accounts, Map<String, String> clubNames) {
        for (int i = 0; i < accounts.size(); i++) {
            ClubAccount account = accounts.get(i);
            String clubName = clubNames.getOrDefault(account.getClubId(), account.getClubId());
            System.out.printf("  %d. [%s] %s | Rs. %s | %s%n", i + 1, clubName,
                    account.getAccountName(), account.getBalance(), account.getStatus());
        }
    }

    private static Map<String, String> clubNameMap(List<Club> clubs) {
        Map<String, String> map = new HashMap<>();
        for (Club club : clubs) map.put(club.getClubId(), club.getClubName());
        return map;
    }

    private static String readText(Scanner scanner, String label, String example) {
        System.out.print(label + " (e.g., " + example + "): ");
        String value = scanner.nextLine().trim();
        if (value.isEmpty()) throw new IllegalArgumentException(label + " cannot be empty.");
        return value;
    }

    private static BigDecimal readAmount(Scanner scanner, String label) {
        System.out.print(label + " (e.g., 1250.00): ");
        try { return new BigDecimal(scanner.nextLine().trim()); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Enter a valid amount, such as 1250.00."); }
    }

    private static int readInt(Scanner scanner, String label, int minimum, int maximum) {
        System.out.print(label + " (e.g., " + minimum + "): ");
        try {
            int value = Integer.parseInt(scanner.nextLine().trim());
            if (value < minimum || value > maximum) throw new IllegalArgumentException("Choose a number from " + minimum + " to " + maximum + ".");
            return value;
        } catch (NumberFormatException e) { throw new IllegalArgumentException("Enter a whole number from " + minimum + " to " + maximum + "."); }
    }

    private static boolean readYesNo(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt + " ");
            String value = scanner.nextLine().trim();
            if (value.equalsIgnoreCase("Y")) return true;
            if (value.equalsIgnoreCase("N")) return false;
            System.out.println("Please enter Y or N.");
        }
    }
}
