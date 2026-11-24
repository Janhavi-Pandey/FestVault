package io;

import service.FinovaService;
import util.InputValidator;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CsvImporter {

    public record PlannedExpense(String accountNo, BigDecimal amount, String purpose) {}

    public record ImportResult(List<PlannedExpense> validExpenses, List<String> errors) {}

    public ImportResult parseExpensesCsv(String filePath) {
        List<PlannedExpense> valid = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }

                // Skip optional header row on line 1
                if (lineNumber == 1 && trimmed.toLowerCase().startsWith("account_no,")) {
                    continue;
                }

                String[] parts = trimmed.split(",", 3);
                if (parts.length < 3) {
                    errors.add("Line " + lineNumber + ": Expected 3 columns (account_no,amount,purpose).");
                    continue;
                }

                String accountNo = parts[0].trim();
                String amountStr = parts[1].trim();
                String purpose = parts[2].trim().replaceAll("^\"|\"$", "");

                if (accountNo.isEmpty()) {
                    errors.add("Line " + lineNumber + ": Missing account number.");
                    continue;
                }

                if (purpose.isEmpty()) {
                    errors.add("Line " + lineNumber + ": Missing expense purpose.");
                    continue;
                }

                BigDecimal amount;
                try {
                    amount = new BigDecimal(amountStr);
                } catch (NumberFormatException ex) {
                    errors.add("Line " + lineNumber + ": Invalid amount '" + amountStr + "'.");
                    continue;
                }

                if (!InputValidator.isPositiveAmount(amount)) {
                    errors.add("Line " + lineNumber + ": Amount must be greater than zero.");
                    continue;
                }

                valid.add(new PlannedExpense(accountNo, amount, purpose));
            }
        } catch (IOException e) {
            errors.add("File read error: " + e.getMessage());
        }

        return new ImportResult(valid, errors);
    }

    public ImportResult importAndRecordExpenses(String filePath, FinovaService service) {
        ImportResult parsed = parseExpensesCsv(filePath);
        List<PlannedExpense> recorded = new ArrayList<>();
        List<String> allErrors = new ArrayList<>(parsed.errors());

        for (PlannedExpense item : parsed.validExpenses()) {
            try {
                service.recordVendorExpense(item.accountNo(), item.amount(), item.purpose());
                recorded.add(item);
            } catch (Exception ex) {
                allErrors.add("Account " + item.accountNo() + " (" + item.purpose() + "): " + ex.getMessage());
            }
        }

        return new ImportResult(recorded, allErrors);
    }
}