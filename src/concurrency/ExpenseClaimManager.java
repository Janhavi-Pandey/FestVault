package concurrency;

import exception.InactiveAccountException;
import model.AccountStatus;
import model.ClubAccount;

import java.math.BigDecimal;

/** Synchronizes withdrawals from the same team wallet during a batch. */
public class ExpenseClaimManager {
    public boolean processClaim(ClubAccount account, String claimId, BigDecimal claimAmount) {
        synchronized (account) {
            if (account.getStatus() != AccountStatus.ACTIVE) {
                throw new InactiveAccountException("The selected team wallet is INACTIVE.");
            }
            if (claimAmount == null || claimAmount.compareTo(BigDecimal.ZERO) <= 0) return false;
            if (account.getBalance().compareTo(claimAmount) < 0) {
                System.out.println(claimId + " was rejected: insufficient team balance. Remaining Rs. " + account.getBalance());
                return false;
            }
            account.setBalance(account.getBalance().subtract(claimAmount));
            System.out.println(claimId + " was approved. Remaining team balance: Rs. " + account.getBalance());
            return true;
        }
    }
}
