
import javafx.application.Platform;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;

public class LoginTask implements Runnable {

    private final String email;
    private final String password;
    private final ArrayList<User> users;
    private final AttemptTracker tracker;
    private final BlockingQueue<FailedAttempt> failureQueue;
    private final Consumer<LoginResult> resultCallback;

    public LoginTask(
            String email,
            String password,
            ArrayList<User> users,
            AttemptTracker tracker,
            BlockingQueue<FailedAttempt> failureQueue,
            Consumer<LoginResult> resultCallback) {

        this.email = email;
        this.password = password;
        this.users = users;
        this.tracker = tracker;
        this.failureQueue = failureQueue;
        this.resultCallback = resultCallback;
    }

    @Override
    public void run() {

        LoginResult result;

        try {

            // Find the user by email.
            User existingUser = null;

            for (User user : users) {
                if (user.getName().equals(email)) {
                    existingUser = user;
                    break;
                }
            }

            // Unknown email: do not track this attempt.
            if (existingUser == null) {
                result = new LoginResult(
                        LoginResult.Status.INVALID, -1, 0
                );

                reportResult(result);
                return;
            }

            // Check whether the account is blocked.
            BlockCheckThread blockChecker =
                    new BlockCheckThread(tracker, email);

            blockChecker.start();
            blockChecker.join();

            long remainingBlockMillis =
                    blockChecker.getRemainingBlockMillis();

            if (remainingBlockMillis > 0) {
                result = new LoginResult(
                        LoginResult.Status.BLOCKED,
                        0,
                        remainingBlockMillis
                );

                reportResult(result);
                return;
            }

            // Validate the entered password.
            boolean passwordCorrect;

            try {
                User enteredUser = new User(email, password);

                passwordCorrect =
                        existingUser.getPassword().equals(
                                enteredUser.getPassword()
                        );

            } catch (IllegalArgumentException e) {
                passwordCorrect = false;
            }

            // Successful login.
            if (passwordCorrect) {

                long remainingMillis =
                        tracker.completeSuccessfulLogin(email);

                if (remainingMillis > 0) {
                    result = new LoginResult(
                            LoginResult.Status.BLOCKED,
                            0,
                            remainingMillis
                    );
                } else {
                    result = new LoginResult(
                            LoginResult.Status.SUCCESS, 0, 0
                    );
                }

            } else {

                // Send the failed attempt to a FailureWorker.
                FailedAttempt attempt = new FailedAttempt(email);

                failureQueue.put(attempt);

                // Wait for the FailureWorker's response.
                int attempts = attempt.getResponse().take();

                if (attempts == -1 || tracker.isBlocked(email)) {

                    long remainingMillis =
                            tracker.getUserState(email)
                                    .getRemainingBlockMillis();

                    if (remainingMillis > 0) {
                        result = new LoginResult(
                                LoginResult.Status.BLOCKED,
                                0,
                                remainingMillis
                        );
                    } else {
                        result = new LoginResult(
                                LoginResult.Status.INVALID,
                                tracker.getRemainingAttempts(email),
                                0
                        );
                    }

                } else {

                    int remainingAttempts =
                            tracker.getRemainingAttempts(email);

                    result = new LoginResult(
                            LoginResult.Status.INVALID,
                            remainingAttempts,
                            0
                    );
                }
            }

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            result = new LoginResult(
                    LoginResult.Status.INVALID, 0, 0
            );
        }

        reportResult(result);
    }

    private void reportResult(LoginResult result) {
        Platform.runLater(() -> resultCallback.accept(result));
    }
}
