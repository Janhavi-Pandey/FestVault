package util;

import java.math.BigDecimal;

public class ReceiptFormatter {
    private ReceiptFormatter() {}

    public static String formatExpenseReceipt(
            String txnId,
            String accountNo,
            String accountName,
            BigDecimal amountPaid,
            String purpose,
            BigDecimal remainingBalance,
            String status
    ) {
        StringBuilder sb = new StringBuilder();
        String line = "------------------------------------------------------------";
        sb.append(line).append("\n");
        sb.append("                  FESTVAULT EXPENSE RECEIPT\n");
        sb.append(line).append("\n");
        sb.append("Transaction ID : ").append(txnId).append("\n");
        sb.append("Account        : ").append(accountNo).append(" (").append(accountName).append(")\n");
        sb.append("Amount Paid    : Rs. ").append(amountPaid).append("\n");
        sb.append("Purpose        : ").append(purpose).append("\n");
        sb.append("Remaining Bal. : Rs. ").append(remainingBalance).append("\n");
        sb.append("Status         : ").append(status).append("\n");
        sb.append(line);
        return sb.toString();
    }
}
