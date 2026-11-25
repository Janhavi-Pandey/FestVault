package concurrency;

import java.util.List;

/** Adds all submitted claims to the bounded queue. */
public class ClaimProducer implements Runnable {
    private final ClaimQueue claimQueue;
    private final List<ExpenseClaimTask> claimsToEnqueue;

    public ClaimProducer(ClaimQueue claimQueue, List<ExpenseClaimTask> claimsToEnqueue) {
        this.claimQueue = claimQueue;
        this.claimsToEnqueue = claimsToEnqueue;
    }

    @Override public void run() {
        try {
            for (ExpenseClaimTask task : claimsToEnqueue) claimQueue.put(task);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            claimQueue.markProductionComplete();
        }
    }
}
