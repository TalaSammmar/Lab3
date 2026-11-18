
import java.util.concurrent.ConcurrentHashMap;

public class AttemptTracker {

    private final ConcurrentHashMap<String, UserState> users;
    private final int maxAttempts;
    private final int blockDurationSeconds;

    public AttemptTracker(int maxAttempts, int blockDurationSeconds) {
        if (maxAttempts < 1 || blockDurationSeconds < 1) {
            throw new IllegalArgumentException(
                    "Both arguments must be at least 1"
            );
        }

        this.maxAttempts = maxAttempts;
        this.blockDurationSeconds = blockDurationSeconds;
        this.users = new ConcurrentHashMap<>();
    }

    public UserState getUserState(String email) {
        return users.computeIfAbsent(email, key -> new UserState());
    }

    public synchronized boolean isBlocked(String email) {
        UserState state = getUserState(email);
        return state.getRemainingBlockMillis() > 0;
    }

    public synchronized int recordFailure(String email) {
        UserState state = getUserState(email);

        if (state.getRemainingBlockMillis() > 0) {
            return -1;
        }

        int attempts = state.recordFailure();

        if (attempts >= maxAttempts) {
            long durationMillis = blockDurationSeconds * 1000L;
            state.blockAccount(durationMillis);
        }

        return attempts;
    }

    public synchronized int getRemainingAttempts(String email) {
        UserState state = getUserState(email);

        if (state.getRemainingBlockMillis() > 0) {
            return 0;
        }

        return Math.max(
                0,
                maxAttempts - state.getFailedAttempts()
        );
    }

    public synchronized void resetAttempts(String email) {
        UserState state = getUserState(email);
        state.reset();
    }

    public synchronized long completeSuccessfulLogin(String email) {

        UserState state = getUserState(email);

        long remainingMillis = state.getRemainingBlockMillis();

        if (remainingMillis > 0) {
            return remainingMillis;
        }

        state.reset();

        return -1;
    }
}
