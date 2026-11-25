package util;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class ReceiptFormatter {
    private ReceiptFormatter() { }

    public static String formatExpenseReceipt(String txnId, String clubName, String accountName,
                                              BigDecimal amountPaid, String purpose,
                                              BigDecimal remainingBalance, String status) {
        String line = "====================================================================";
        StringBuilder text = new StringBuilder();
        text.append(line).append("\n");
        text.append("FINOVA EXPENSE RECEIPT\n");
        text.append(line).append("\n");
        text.append("Transaction ID : ").append(txnId).append("\n");
        text.append("Club           : ").append(clubName).append("\n");
        text.append("Paying Wallet  : ").append(accountName).append("\n");
        text.append("Amount         : Rs. ").append(amountPaid).append("\n");
        text.append("Description    : ").append(purpose).append("\n");
        text.append("Balance Left   : Rs. ").append(remainingBalance).append("\n");
        text.append("Status         : ").append(status).append("\n");
        text.append("Date           : ").append(LocalDateTime.now()).append("\n");
        text.append(line);
        return text.toString();
    }
}
