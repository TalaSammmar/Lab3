
import java.util.ArrayList;
import java.util.List;

public class SynchronizationTest {

    private static final int NUMBER_OF_THREADS = 20;
    private static final int FAILURES_PER_THREAD = 499;
    private static final int MAX_ATTEMPTS = 10000;
    private static final int BLOCK_DURATION = 30;
    private static final int EXPECTED_COUNT =
            NUMBER_OF_THREADS * FAILURES_PER_THREAD;

    private static final String TEST_EMAIL =
            "sync-test@example.com";

    public static String runTest() {

        StringBuilder result = new StringBuilder();

        try {
            // TEST 1: Real AttemptTracker
            AttemptTracker tracker =
                    new AttemptTracker(
                            MAX_ATTEMPTS, BLOCK_DURATION);

            runThreads(() ->
                    tracker.recordFailure(TEST_EMAIL));

            int count = tracker.getUserState(
                    TEST_EMAIL).getFailedAttempts();

            result.append("TEST 1: REAL ATTEMPT TRACKER\n");
            result.append("Expected: ")
                    .append(EXPECTED_COUNT).append("\n");
            result.append("Actual: ")
                    .append(count).append("\n");
            result.append("Passed: ")
                    .append(count == EXPECTED_COUNT)
                    .append("\n\n");

            if (count != EXPECTED_COUNT) {
                result.append("TEST FAILED: Wrong count.\n");
                return result.toString();
            }

            // Add exactly 19 failures.
            for (int i = 0; i < 19; i++) {
                tracker.recordFailure(TEST_EMAIL);
            }

            result.append("After 19 additional: ")
                    .append(tracker.getUserState(
                            TEST_EMAIL).getFailedAttempts())
                    .append("\n");

            result.append("Blocked after 19: ")
                    .append(tracker.isBlocked(TEST_EMAIL))
                    .append("\n");

            // Failure number 10000.
            tracker.recordFailure(TEST_EMAIL);

            result.append("After 20 additional: ")
                    .append(tracker.getUserState(
                            TEST_EMAIL).getFailedAttempts())
                    .append("\n");

            result.append("Blocked after 20: ")
                    .append(tracker.isBlocked(TEST_EMAIL))
                    .append("\n\n");

            // TEST 2: Synchronized counter
            ExperimentCounter safeCounter =
                    new ExperimentCounter();

            runThreads(
                    safeCounter::recordFailureSynchronized);

            result.append("TEST 2: SYNCHRONIZED COUNTER\n");
            result.append("Expected: ")
                    .append(EXPECTED_COUNT).append("\n");
            result.append("Actual: ")
                    .append(safeCounter.getCount())
                    .append("\n");
            result.append("Passed: ")
                    .append(safeCounter.getCount()
                            == EXPECTED_COUNT)
                    .append("\n\n");

            // TEST 3: Unsynchronized counter
            ExperimentCounter unsafeCounter =
                    new ExperimentCounter();

            runThreads(
                    unsafeCounter::recordFailureUnsynchronized);

            result.append("TEST 3: UNSYNCHRONIZED COUNTER\n");
            result.append("Expected: ")
                    .append(EXPECTED_COUNT).append("\n");
            result.append("Actual: ")
                    .append(unsafeCounter.getCount())
                    .append("\n");

            result.append("Lost updates: ")
                    .append(EXPECTED_COUNT
                            - unsafeCounter.getCount())
                    .append("\n");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Synchronization test interrupted.";
        }

        return result.toString();
    }

    private static void runThreads(Runnable action)
            throws InterruptedException {

        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < NUMBER_OF_THREADS; i++) {

            Thread thread = new Thread(() -> {
                for (int j = 0;
                     j < FAILURES_PER_THREAD
                             && !Thread.currentThread().isInterrupted();
                     j++) {

                    action.run();
                }
            });

            thread.setDaemon(true);
            threads.add(thread);
            thread.start();
        }

        try {
            for (Thread thread : threads) {
                thread.join();
            }
        } catch (InterruptedException e) {
            for (Thread thread : threads) {
                thread.interrupt();
            }
            throw e;
        }
    }
}
