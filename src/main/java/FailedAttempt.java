import java.util.concurrent.ArrayBlockingQueue;

public class FailedAttempt {

    // Email of the user whose login attempt failed.
    private final String email;

    // Queue used to return the result to LoginTask.
    private final ArrayBlockingQueue<Integer> response;

    // Constructor.
    public FailedAttempt(String email) {
        this.email = email;
        this.response = new ArrayBlockingQueue<>(1);
    }

    // Return the user's email.
    public String getEmail() {
        return email;
    }

    // Return the response queue.
    public ArrayBlockingQueue<Integer> getResponse() {
        return response;
    }
}