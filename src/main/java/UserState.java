public class UserState {

    private int failedAttempts;
    private long blockEndTime;

    public UserState() {
        failedAttempts = 0;
        blockEndTime = 0;
    }

    public synchronized int getFailedAttempts() {
        return failedAttempts;
    }

    public synchronized long getBlockEndTime() {
        return blockEndTime;
    }

    public synchronized int recordFailure() {
        failedAttempts++;
        return failedAttempts;
    }

    public synchronized void blockAccount(long durationMillis) {
        blockEndTime = System.currentTimeMillis() + durationMillis;
    }

    public synchronized long getRemainingBlockMillis() {
        long remaining = blockEndTime - System.currentTimeMillis();

        if (remaining <= 0) {
            if (blockEndTime != 0) {
                reset();
            }
            return 0;
        }

        return remaining;
    }

    public synchronized void reset() {
        failedAttempts = 0;
        blockEndTime = 0;
    }
}