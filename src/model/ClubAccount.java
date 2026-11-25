package model;

import exception.InactiveAccountException;
import exception.InsufficientBalanceException;
import service.Spendable;

import java.math.BigDecimal;

public abstract class ClubAccount implements Spendable {
    private final String accountNo;
    private final String clubId;
    private final String accountName;
    private BigDecimal balance;
    private AccountStatus status;

    protected ClubAccount(String accountNo, String clubId, String accountName, BigDecimal balance) {
        this.accountNo = accountNo;
        this.clubId = clubId;
        this.accountName = accountName;
        this.balance = balance == null ? BigDecimal.ZERO : balance;
        this.status = AccountStatus.ACTIVE;
    }

    public String getAccountNo() { return accountNo; }
    public String getClubId() { return clubId; }
    public String getAccountName() { return accountName; }
    public BigDecimal getBalance() { return balance; }
    public AccountStatus getStatus() { return status; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public void setStatus(AccountStatus status) { this.status = status; }

    @Override
    public BigDecimal getSpendableBalance() { return balance; }

    @Override
    public void validateSpend(BigDecimal amount) {
        if (status != AccountStatus.ACTIVE) {
            throw new InactiveAccountException("This account is INACTIVE.");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient balance. Available: Rs. " + balance);
        }
    }

    public abstract String getAccountType();
}
