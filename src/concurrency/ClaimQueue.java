package concurrency;

import java.util.LinkedList;
import java.util.Queue;

public class ClaimQueue {
    private final Queue<ExpenseClaimTask> queue = new LinkedList<>();
    private final int capacity;
    private boolean productionComplete = false;

    public ClaimQueue(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Queue capacity must be positive.");
        }
        this.capacity = capacity;
    }

    public synchronized void put(ExpenseClaimTask task) throws InterruptedException {
        while (queue.size() >= capacity) {
            System.out.println("[Producer] Queue full (" + capacity + "). Waiting...");
            wait();
        }
        queue.add(task);
        System.out.println("[Producer] Queued " + task.getClaimId() + " (Rs. " + task.getAmount()
                + ") | Queue size: " + queue.size() + "/" + capacity);
        notifyAll();
    }

    public synchronized ExpenseClaimTask take() throws InterruptedException {
        while (queue.isEmpty() && !productionComplete) {
            wait();
        }
        if (queue.isEmpty() && productionComplete) {
            return null;
        }
        ExpenseClaimTask task = queue.poll();
        System.out.println("[" + Thread.currentThread().getName() + "] Dequeued " + task.getClaimId()
                + " | Remaining in queue: " + queue.size());
        notifyAll();
        return task;
    }

    public synchronized void markProductionComplete() {
        this.productionComplete = true;
        notifyAll();
    }
}
