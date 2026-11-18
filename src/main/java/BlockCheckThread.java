public class BlockCheckThread extends Thread {

    // Tracks the state of users.
    private final AttemptTracker tracker;

    // Email of the user to check.
    private final String email;

    // Stores the remaining block time.
    private long remainingBlockMillis;

    // Constructor.
    public BlockCheckThread(AttemptTracker tracker, String email) {
        this.tracker = tracker;
        this.email = email;
        this.remainingBlockMillis = 0;
    }

    @Override
    public void run() {

        UserState state = tracker.getUserState(email);

        remainingBlockMillis = state.getRemainingBlockMillis();
    }

    // Return the remaining block time.
    public long getRemainingBlockMillis() {
        return remainingBlockMillis;
    }

    // Check whether the account is blocked.
    public boolean isBlocked() {
        return remainingBlockMillis > 0;
    }
}