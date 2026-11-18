public class LoginResult {

    // Possible login results.
    public enum Status {
        SUCCESS,
        INVALID,
        BLOCKED
    }

    // Type of the login result.
    private final Status status;

    // Number of failed attempts remaining.
    private final int remainingAttempts;

    // Remaining block time in milliseconds.
    private final long remainingBlockMillis;

    // Constructor.
    public LoginResult(
            Status status,
            int remainingAttempts,
            long remainingBlockMillis) {

        this.status = status;
        this.remainingAttempts = remainingAttempts;
        this.remainingBlockMillis = remainingBlockMillis;
    }

    // Return the result status.
    public Status getStatus() {
        return status;
    }

    // Return the number of remaining attempts.
    public int getRemainingAttempts() {
        return remainingAttempts;
    }

    // Return the remaining block time.
    public long getRemainingBlockMillis() {
        return remainingBlockMillis;
    }
}