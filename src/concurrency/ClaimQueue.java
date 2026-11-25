package concurrency;

import java.util.LinkedList;
import java.util.Queue;

/** A small bounded queue shared by the claim producer and processing threads. */
public class ClaimQueue {
    private final Queue<ExpenseClaimTask> queue = new LinkedList<>();
    private final int capacity;
    private boolean productionComplete = false;

    public ClaimQueue(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("Queue capacity must be positive.");
        this.capacity = capacity;
    }

    public synchronized void put(ExpenseClaimTask task) throws InterruptedException {
        while (queue.size() >= capacity) {
            System.out.println("The claim queue is full. Waiting for an available slot...");
            wait();
        }
        queue.add(task);
        System.out.println(task.getClaimId() + " added to the claim queue. Pending: " + queue.size());
        notifyAll();
    }

    public synchronized ExpenseClaimTask take() throws InterruptedException {
        while (queue.isEmpty() && !productionComplete) wait();
        if (queue.isEmpty() && productionComplete) return null;
        ExpenseClaimTask task = queue.poll();
        notifyAll();
        return task;
    }

    public synchronized void markProductionComplete() {
        productionComplete = true;
        notifyAll();
    }
}
