import java.util.concurrent.BlockingQueue;

public class FailureWorker extends Thread {

    // Queue containing failed login attempts.
    private final BlockingQueue<FailedAttempt> queue;

    // Tracks failed attempts for each user.
    private final AttemptTracker tracker;

    // Constructor.
    public FailureWorker(
            BlockingQueue<FailedAttempt> queue,
            AttemptTracker tracker) {

        this.queue = queue;
        this.tracker = tracker;

        // Worker threads should not prevent the application from closing.
        setDaemon(true);
    }

    @Override
    public void run() {

        while (!Thread.currentThread().isInterrupted()) {

            try {
                // Wait for a failed attempt from the queue.
                FailedAttempt attempt = queue.take();

                // Record the failure.
                int result = tracker.recordFailure(attempt.getEmail());

                // Return the result to LoginTask.
                // -1 means that the account is already blocked.
                attempt.getResponse().put(result);

            } catch (InterruptedException e) {
                // Stop the worker when it is interrupted.
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}