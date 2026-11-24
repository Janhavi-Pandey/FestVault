package concurrency;

import model.ClubAccount;

import java.math.BigDecimal;

public class ExpenseClaimTask implements Runnable {
    private final ExpenseClaimManager claimManager;
    private final ClubAccount targetAccount;
    private final String claimId;
    private final String coordinatorTeam;
    private final BigDecimal amount;
    private boolean approved;

    public ExpenseClaimTask(
            ExpenseClaimManager claimManager,
            ClubAccount targetAccount,
            String claimId,
            String coordinatorTeam,
            BigDecimal amount
    ) {
        this.claimManager = claimManager;
        this.targetAccount = targetAccount;
        this.claimId = claimId;
        this.coordinatorTeam = coordinatorTeam;
        this.amount = amount;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(150);
            this.approved = claimManager.processClaim(
                    targetAccount,
                    claimId,
                    coordinatorTeam,
                    amount,
                    Thread.currentThread().getName()
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("[" + Thread.currentThread().getName() + "] Interrupted while processing " + claimId);
        }
    }

    public boolean isApproved() { return approved; }
    public String getClaimId() { return claimId; }
    public BigDecimal getAmount() { return amount; }
}
