package model;

import exception.InactiveAccountException;
import exception.InsufficientBalanceException;
import service.Spendable;

import java.math.BigDecimal;
import java.util.Objects;

public abstract class ClubAccount implements Spendable {
    private final String accountNo;
    private final String clubId;
    private final String accountName;
    private BigDecimal balance;
    private BigDecimal lockedAmount;
    private AccountStatus status;

    protected ClubAccount(String accountNo, String clubId, String accountName, BigDecimal balance) {
        this.accountNo = accountNo;
        this.clubId = clubId;
        this.accountName = accountName;
        this.balance = balance != null ? balance : BigDecimal.ZERO;
        this.lockedAmount = BigDecimal.ZERO;
        this.status = AccountStatus.ACTIVE;
    }

    public String getAccountNo() { return accountNo; }
    public String getClubId() { return clubId; }
    public String getAccountName() { return accountName; }
    public BigDecimal getBalance() { return balance; }
    public BigDecimal getLockedAmount() { return lockedAmount; }
    public AccountStatus getStatus() { return status; }

    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public void setLockedAmount(BigDecimal lockedAmount) { this.lockedAmount = lockedAmount; }
    public void setStatus(AccountStatus status) { this.status = status; }

    @Override
    public BigDecimal getSpendableBalance() {
        return balance.subtract(lockedAmount);
    }

    @Override
    public void validateSpend(BigDecimal amount) {
        Objects.requireNonNull(amount, "Amount cannot be null");
        if (this.status != AccountStatus.ACTIVE) {
            throw new InactiveAccountException("Account " + accountNo + " is INACTIVE. Operation blocked.");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }
        if (getSpendableBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                "Insufficient spendable balance in " + accountNo +
                " (Available: Rs. " + getSpendableBalance() + ", Requested: Rs. " + amount + ")"
            );
        }
    }

    public abstract String getAccountType();
}
