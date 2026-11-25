package io;

import service.FinovaService;
import util.InputValidator;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/** Imports planned vendor bills by team name or account number. */
public class CsvImporter {
    public static final String DEFAULT_FILE_PATH = "data/planned_expenses.csv";

    public static class PlannedExpense {
        private final String teamIdentifier;
        private final BigDecimal amount;
        private final String description;

        public PlannedExpense(String teamIdentifier, BigDecimal amount, String description) {
            this.teamIdentifier = teamIdentifier;
            this.amount = amount;
            this.description = description;
        }

        public String teamIdentifier() { return teamIdentifier; }
        public String teamName() { return teamIdentifier; }
        public BigDecimal amount() { return amount; }
        public String description() { return description; }
    }

    public static class ImportResult {
        private final List<PlannedExpense> validExpenses;
        private final List<String> errors;

        public ImportResult(List<PlannedExpense> validExpenses, List<String> errors) {
            this.validExpenses = validExpenses;
            this.errors = errors;
        }

        public List<PlannedExpense> validExpenses() { return validExpenses; }
        public List<String> errors() { return errors; }
    }

    public ImportResult parseExpensesCsv() {
        return parseExpensesCsv(DEFAULT_FILE_PATH);
    }

    public ImportResult parseExpensesCsv(String filePath) {
        List<PlannedExpense> valid = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Path path = Paths.get(filePath);
        try {
            if (path.getParent() != null) Files.createDirectories(path.getParent());
            try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                String line;
                int lineNumber = 0;
                boolean firstContentLine = true;
                boolean accountNumberColumn = false;
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    if (line.isBlank()) continue;
                    if (firstContentLine) {
                        firstContentLine = false;
                        String header = line.trim().toLowerCase(java.util.Locale.ROOT);
                        if (header.startsWith("team_name,") || header.startsWith("account_no,")) {
                            accountNumberColumn = header.startsWith("account_no,");
                            continue;
                        }
                    }
                    List<String> columns = parseCsvLine(line);
                    if (columns.size() != 3) {
                        errors.add("Line " + lineNumber + ": Expected team_name or account_no,amount,description.");
                        continue;
                    }
                    String teamIdentifier = columns.get(0).trim();
                    String description = columns.get(2).trim();
                    if (!InputValidator.isValidText(teamIdentifier) || !InputValidator.isValidText(description)) {
                        errors.add("Line " + lineNumber + ": Team name/account number and description are required.");
                        continue;
                    }
                    if (accountNumberColumn && !teamIdentifier.matches("FV-[0-9]+")) {
                        errors.add("Line " + lineNumber + ": Invalid account number '" + teamIdentifier + "'.");
                        continue;
                    }
                    try {
                        BigDecimal amount = new BigDecimal(columns.get(1).trim());
                        if (!InputValidator.isPositiveAmount(amount)) {
                            errors.add("Line " + lineNumber + ": Amount must be greater than zero.");
                            continue;
                        }
                        valid.add(new PlannedExpense(teamIdentifier, amount, description));
                    } catch (NumberFormatException e) {
                        errors.add("Line " + lineNumber + ": Invalid amount '" + columns.get(1) + "'.");
                    }
                }
            }
        } catch (IOException e) {
            errors.add("Could not read " + filePath + ": " + e.getMessage());
        }
        return new ImportResult(valid, errors);
    }

    public ImportResult importAndRecordExpenses(String clubId, FinovaService service) {
        return importAndRecordExpenses(DEFAULT_FILE_PATH, clubId, service);
    }

    public ImportResult importAndRecordExpenses(String filePath, String clubId, FinovaService service) {
        ImportResult parsed = parseExpensesCsv(filePath);
        List<PlannedExpense> processed = new ArrayList<>();
        List<String> errors = new ArrayList<>(parsed.errors());
        for (PlannedExpense expense : parsed.validExpenses()) {
            try {
                service.importPlannedExpense(clubId, expense.teamIdentifier(), expense.amount(), expense.description());
                processed.add(expense);
            } catch (Exception e) {
                errors.add(expense.teamIdentifier() + " / " + expense.description() + ": " + e.getMessage());
            }
        }
        return new ImportResult(processed, errors);
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    value.append('"'); i++;
                } else quoted = !quoted;
            } else if (c == ',' && !quoted) {
                values.add(value.toString()); value.setLength(0);
            } else value.append(c);
        }
        values.add(value.toString());
        return values;
    }
}
