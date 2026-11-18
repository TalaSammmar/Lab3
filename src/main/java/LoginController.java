
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

public class LoginController {

    private ArrayList<User> users;
    private AttemptTracker tracker;
    private BlockingQueue<FailedAttempt> failureQueue;

    private final Set<Thread> activeThreads =
            ConcurrentHashMap.newKeySet();

    private volatile boolean closed = false;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label messageLabel;

    @FXML
    private Button loginButton;

    @FXML
    private Button synchronizationButton;

    public void setDependencies(
            ArrayList<User> users,
            AttemptTracker tracker,
            BlockingQueue<FailedAttempt> failureQueue) {

        this.users = users;
        this.tracker = tracker;
        this.failureQueue = failureQueue;
    }

    @FXML
    private void handleLogin() {

        if (closed) {
            return;
        }

        String email = usernameField.getText();
        String password = passwordField.getText();

        usernameField.setDisable(true);
        passwordField.setDisable(true);
        loginButton.setDisable(true);
        synchronizationButton.setDisable(true);

        messageLabel.setText("Checking login...");

        LoginTask task = new LoginTask(
                email,
                password,
                users,
                tracker,
                failureQueue,
                this::handleLoginResult
        );

        Thread loginThread = new Thread(() -> {
            try {
                task.run();
            } finally {
                activeThreads.remove(Thread.currentThread());
            }
        });

        loginThread.setDaemon(true);

        activeThreads.add(loginThread);
        loginThread.start();
    }

    @FXML
    private void handleSynchronizationTest() {

        if (closed) {
            return;
        }

        usernameField.setDisable(true);
        passwordField.setDisable(true);
        loginButton.setDisable(true);
        synchronizationButton.setDisable(true);

        messageLabel.setText("Running synchronization test...");

        Thread testThread = new Thread(() -> {
            try {

                String testResult = SynchronizationTest.runTest();

                Platform.runLater(() -> {

                    if (closed) {
                        return;
                    }

                    Alert alert = new Alert(
                            Alert.AlertType.INFORMATION
                    );

                    alert.setTitle("Synchronization Test Results");
                    alert.setHeaderText("Experiment Results");
                    alert.setContentText(testResult);
                    alert.getDialogPane().setPrefWidth(500);
                    alert.show();

                    messageLabel.setText("");
                    enableLoginControls();
                });

            } finally {
                activeThreads.remove(Thread.currentThread());
            }
        });

        testThread.setDaemon(true);

        activeThreads.add(testThread);
        testThread.start();
    }

    private void handleLoginResult(LoginResult result) {

        if (closed) {
            return;
        }

        switch (result.getStatus()) {

            case SUCCESS:
                openWelcomeScreen(usernameField.getText());
                break;

            case INVALID:

                if (result.getRemainingAttempts() == -1) {
                    messageLabel.setText(
                            "Invalid username or password."
                    );
                } else {
                    messageLabel.setText(
                            "Invalid username or password. " +
                                    "Remaining attempts: " +
                                    result.getRemainingAttempts()
                    );
                }

                enableLoginControls();
                break;

            case BLOCKED:

                usernameField.setDisable(true);
                passwordField.setDisable(true);
                loginButton.setDisable(true);
                synchronizationButton.setDisable(true);

                messageLabel.setText(
                        "החשבון חסום, נותרו " +
                                ((result.getRemainingBlockMillis() + 999) / 1000) +
                                " שניות"
                );

                Thread countdownThread = new Thread(() -> {
                    try {
                        new CountdownTask(
                                messageLabel,
                                result.getRemainingBlockMillis(),
                                this::enableLoginControls
                        ).run();
                    } finally {
                        activeThreads.remove(Thread.currentThread());
                    }
                });

                countdownThread.setDaemon(true);

                activeThreads.add(countdownThread);
                countdownThread.start();

                break;
        }
    }

    private void openWelcomeScreen(String username) {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/welcome.fxml")
            );

            Parent root = loader.load();

            WelcomeController controller = loader.getController();
            controller.setUsername(username);

            Stage welcomeStage = new Stage();
            welcomeStage.setTitle("Welcome");
            welcomeStage.setScene(new Scene(root));
            welcomeStage.show();

            Stage loginStage =
                    (Stage) usernameField.getScene().getWindow();

            loginStage.close();

        } catch (Exception e) {

            messageLabel.setText(
                    "Could not open Welcome screen."
            );

            e.printStackTrace();
            enableLoginControls();
        }
    }

    private void enableLoginControls() {

        if (closed) {
            return;
        }

        usernameField.setDisable(false);
        passwordField.setDisable(false);
        loginButton.setDisable(false);
        synchronizationButton.setDisable(false);
    }

    public void stopBackgroundThreads() {

        closed = true;

        for (Thread thread : activeThreads) {
            thread.interrupt();
        }

        activeThreads.clear();
    }
}
