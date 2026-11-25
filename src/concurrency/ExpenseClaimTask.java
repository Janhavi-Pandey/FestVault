package concurrency;

import model.ClubAccount;
import java.math.BigDecimal;

public class ExpenseClaimTask implements Runnable {
    private final ExpenseClaimManager claimManager;
    private final ClubAccount targetAccount;
    private final String claimId;
    private final String coordinatorTeam;
    private final BigDecimal amount;
    private volatile boolean approved;

    public ExpenseClaimTask(ExpenseClaimManager claimManager, ClubAccount targetAccount,
                            String claimId, String coordinatorTeam, BigDecimal amount) {
        this.claimManager = claimManager;
        this.targetAccount = targetAccount;
        this.claimId = claimId;
        this.coordinatorTeam = coordinatorTeam;
        this.amount = amount;
    }

    @Override public void run() {
        try {
            // Small processing delay makes simultaneous claim execution visible in the console.
            Thread.sleep(80);
            approved = claimManager.processClaim(targetAccount, claimId, amount);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            approved = false;
        }
    }

    public boolean isApproved() { return approved; }
    public String getClaimId() { return claimId; }
    public String getCoordinatorTeam() { return coordinatorTeam; }
    public BigDecimal getAmount() { return amount; }
}
