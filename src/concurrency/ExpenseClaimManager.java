package concurrency;

import exception.InactiveAccountException;
import model.AccountStatus;
import model.ClubAccount;

import java.math.BigDecimal;

public class ExpenseClaimManager {

    public boolean processClaim(
            ClubAccount account,
            String claimId,
            String teamName,
            BigDecimal claimAmount,
            String workerName
    ) {
        synchronized (account) {
            if (account.getStatus() != AccountStatus.ACTIVE) {
                throw new InactiveAccountException(
                        "[" + workerName + "] " + claimId + " (" + teamName + ") BLOCKED: Account "
                                + account.getAccountNo() + " is INACTIVE."
                );
            }

            BigDecimal spendableBefore = account.getSpendableBalance();
            if (claimAmount == null || claimAmount.compareTo(BigDecimal.ZERO) <= 0) {
                System.out.println("[" + workerName + "] " + claimId + " (" + teamName + ") REJECTED: Invalid amount.");
                return false;
            }

            if (spendableBefore.compareTo(claimAmount) < 0) {
                System.out.println("[" + workerName + "] " + claimId + " (" + teamName + ") REJECTED -> "
                        + "Insufficient spendable funds (Rs. " + spendableBefore + " left)");
                return false;
            }

            BigDecimal updatedBalance = account.getBalance().subtract(claimAmount);
            account.setBalance(updatedBalance);

            System.out.println("[" + workerName + "] " + claimId + " (" + teamName + ") APPROVED -> "
                    + "New Spendable Balance: Rs. " + account.getSpendableBalance());
            return true;
        }
    }
}
