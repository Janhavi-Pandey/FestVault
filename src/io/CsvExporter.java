package io;

import model.TransactionRecord;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/** Exports the global transaction ledger in CSV format. */
public class CsvExporter {
    public static final String DEFAULT_FILE_PATH = "data/finova_audit.csv";

    public void exportTransactions(List<TransactionRecord> records) throws IOException {
        exportTransactions(DEFAULT_FILE_PATH, records);
    }

    public void exportTransactions(String outputFilePath, List<TransactionRecord> records) throws IOException {
        Path path = Paths.get(outputFilePath);
        if (path.getParent() != null) Files.createDirectories(path.getParent());
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("txn_id,club_id,from_account,to_account,txn_type,amount,sponsor_name,bank_ref_id,purpose,status,txn_date");
            writer.newLine();
            for (TransactionRecord record : records) {
                String[] values = {
                        record.getTxnId(), record.getClubId(), record.getFromAccount(), record.getToAccount(),
                        record.getTxnType(), record.getAmount() == null ? "" : record.getAmount().toPlainString(),
                        record.getSponsorName(), record.getBankRefId(), record.getPurpose(),
                        record.getStatus(), record.getTxnDate() == null ? "" : record.getTxnDate().toString()
                };
                for (int i = 0; i < values.length; i++) {
                    if (i > 0) writer.write(",");
                    writer.write(escape(values[i]));
                }
                writer.newLine();
            }
        }
    }

    private String escape(String value) {
        if (value == null) return "";
        String cleaned = value.replace("\"", "\"\"");
        if (cleaned.contains(",") || cleaned.contains("\"") || cleaned.contains("\n") || cleaned.contains("\r")) {
            return "\"" + cleaned + "\"";
        }
        return cleaned;
    }
}
