package io;

import model.TransactionRecord;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class CsvExporter {

    public void exportTransactions(String outputFilePath, List<TransactionRecord> records) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {
            writer.write("txn_id,from_account,to_account,txn_type,amount,sponsor_name,bank_ref_id,purpose,status,txn_date");
            writer.newLine();

            for (TransactionRecord r : records) {
                String row = String.join(",",
                        escapeCsv(r.getTxnId()),
                        escapeCsv(r.getFromAccount()),
                        escapeCsv(r.getToAccount()),
                        escapeCsv(r.getTxnType()),
                        escapeCsv(r.getAmount() != null ? r.getAmount().toPlainString() : ""),
                        escapeCsv(r.getSponsorName()),
                        escapeCsv(r.getBankRefId()),
                        escapeCsv(r.getPurpose()),
                        escapeCsv(r.getStatus()),
                        escapeCsv(r.getTxnDate() != null ? r.getTxnDate().toString() : "")
                );
                writer.write(row);
                writer.newLine();
            }
        }
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        boolean needsQuotes = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r");
        String escaped = value.replace("\"", "\"\"");
        return needsQuotes ? "\"" + escaped + "\"" : escaped;
    }
}