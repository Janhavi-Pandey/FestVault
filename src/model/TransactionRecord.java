package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionRecord {
    private final String txnId;
    private final String clubId;
    private final String fromAccount;
    private final String toAccount;
    private final String txnType;
    private final BigDecimal amount;
    private final String sponsorName;
    private final String bankRefId;
    private final String purpose;
    private final String status;
    private final LocalDateTime txnDate;

    public TransactionRecord(String txnId, String clubId, String fromAccount, String toAccount,
                             String txnType, BigDecimal amount, String sponsorName, String bankRefId,
                             String purpose, String status, LocalDateTime txnDate) {
        this.txnId = txnId; this.clubId = clubId; this.fromAccount = fromAccount; this.toAccount = toAccount;
        this.txnType = txnType; this.amount = amount; this.sponsorName = sponsorName;
        this.bankRefId = bankRefId; this.purpose = purpose; this.status = status; this.txnDate = txnDate;
    }

    public TransactionRecord(String txnId, String fromAccount, String toAccount, String txnType,
                             BigDecimal amount, String sponsorName, String bankRefId, String purpose,
                             String status, LocalDateTime txnDate) {
        this(txnId, null, fromAccount, toAccount, txnType, amount, sponsorName, bankRefId,
                purpose, status, txnDate);
    }

    public String getTxnId() { return txnId; }
    public String getClubId() { return clubId; }
    public String getFromAccount() { return fromAccount; }
    public String getToAccount() { return toAccount; }
    public String getTxnType() { return txnType; }
    public BigDecimal getAmount() { return amount; }
    public String getSponsorName() { return sponsorName; }
    public String getBankRefId() { return bankRefId; }
    public String getReferenceId() { return bankRefId; }
    public String getPurpose() { return purpose; }
    public String getStatus() { return status; }
    public LocalDateTime getTxnDate() { return txnDate; }

    @Override public String toString() {
        return txnDate + " | " + txnType + " | Rs. " + amount + " | " + status + " | " + purpose;
    }
}
