package concurrency;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class BatchClaimRunner {

    public static int runBatch(ClaimQueue queue, List<ExpenseClaimTask> tasks)
            throws InterruptedException, ExecutionException {
        return runBatch(queue, tasks, 3);
    }

    public static int runBatch(ClaimQueue queue, List<ExpenseClaimTask> tasks, int threadCount)
            throws InterruptedException, ExecutionException {
        if (threadCount < 1 || threadCount > 100) {
            throw new IllegalArgumentException("Thread count must be between 1 and 100.");
        }
        AtomicInteger approvedCount = new AtomicInteger(0);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        Thread producerThread = new Thread(new ClaimProducer(queue, tasks), "Claim-Producer");
        producerThread.start();

        for (int i = 1; i <= threadCount; i++) {
            final String workerName = "Worker-" + i;
            executor.submit(() -> {
                Thread.currentThread().setName(workerName);
                try {
                    while (true) {
                        ExpenseClaimTask task = queue.take();
                        if (task == null) {
                            break;
                        }
                        task.run();
                        if (task.isApproved()) {
                            approvedCount.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        producerThread.join();

        Callable<Integer> summaryTask = approvedCount::get;
        Future<Integer> futureApproved = executor.submit(summaryTask);

        executor.shutdown();
        if (!executor.awaitTermination(15, TimeUnit.SECONDS)) {
            executor.shutdownNow();
        }

        return futureApproved.get();
    }
}
