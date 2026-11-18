
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.GridPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Main extends Application {

    private FailureWorker worker1;
    private FailureWorker worker2;

    private LoginController loginController;
    private Thread loadingThread;
    private volatile boolean shuttingDown = false;

    @Override
    public void start(Stage stage) {

        // Keep JavaFX alive while users are loading.
        Platform.setImplicitExit(false);

        int[] parameters = getValidArguments();

        if (parameters == null) {
            Platform.exit();
            return;
        }

        int maxAttempts = parameters[0];
        int blockDurationSeconds = parameters[1];

        AttemptTracker tracker = new AttemptTracker(
                maxAttempts,
                blockDurationSeconds
        );

        BlockingQueue<FailedAttempt> failureQueue =
                new LinkedBlockingQueue<>();

        loadingThread = new Thread(() -> {

            try {
                // File loading happens outside the JavaFX thread.
                ArrayList<User> users = UsersApp.loadUsers();

                if (Thread.currentThread().isInterrupted()
                        || shuttingDown) {
                    return;
                }

                Platform.runLater(() -> {

                    if (shuttingDown) {
                        return;
                    }

                    try {
                        worker1 = new FailureWorker(
                                failureQueue, tracker
                        );

                        worker2 = new FailureWorker(
                                failureQueue, tracker
                        );

                        worker1.start();
                        worker2.start();

                        FXMLLoader loader = new FXMLLoader(
                                Main.class.getResource("/login.fxml")
                        );

                        Parent root = loader.load();

                        loginController = loader.getController();

                        loginController.setDependencies(
                                users,
                                tracker,
                                failureQueue
                        );

                        Scene scene = new Scene(root);

                        stage.setTitle("Login");
                        stage.setScene(scene);

                        stage.setOnCloseRequest(
                                event -> shutdownThreads()
                        );

                        // Restore automatic exit when windows close.
                        Platform.setImplicitExit(true);

                        stage.show();

                    } catch (Exception e) {

                        System.err.println(
                                "Application could not start: "
                                        + e.getMessage()
                        );

                        e.printStackTrace();
                        shutdownThreads();
                        Platform.exit();
                    }
                });

            } catch (Exception e) {

                if (!shuttingDown) {
                    System.err.println(
                            "Could not load users: "
                                    + e.getMessage()
                    );

                    e.printStackTrace();
                    Platform.runLater(() -> {
                        shutdownThreads();
                        Platform.exit();
                    });
                }
            }

        }, "UserLoadingThread");

        loadingThread.setDaemon(true);
        loadingThread.start();
    }

    private int[] getValidArguments() {

        var arguments = getParameters().getRaw();

        if (arguments.size() == 2) {
            try {
                int n = Integer.parseInt(arguments.get(0));
                int t = Integer.parseInt(arguments.get(1));

                if (n >= 1 && t >= 1) {
                    return new int[]{n, t};
                }

            } catch (NumberFormatException ignored) {
                // Invalid command-line arguments.
            }
        }

        return askForArguments();
    }

    private int[] askForArguments() {

        while (true) {

            TextField nField = new TextField();
            TextField tField = new TextField();

            nField.setPromptText("Enter n (at least 1)");
            tField.setPromptText(
                    "Enter t in seconds (at least 1)"
            );

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);

            grid.add(
                    new Label("Maximum failed attempts (n):"),
                    0, 0
            );
            grid.add(nField, 1, 0);

            grid.add(
                    new Label("Block duration in seconds (t):"),
                    0, 1
            );
            grid.add(tField, 1, 1);

            Alert dialog = new Alert(
                    Alert.AlertType.CONFIRMATION
            );

            dialog.setTitle("Invalid or missing arguments");
            dialog.setHeaderText(
                    "Please enter valid values for n and t."
            );

            dialog.getDialogPane().setContent(grid);

            Optional<javafx.scene.control.ButtonType> result =
                    dialog.showAndWait();

            if (result.isEmpty()
                    || result.get()
                    != javafx.scene.control.ButtonType.OK) {
                return null;
            }

            try {
                int n = Integer.parseInt(
                        nField.getText().trim()
                );

                int t = Integer.parseInt(
                        tField.getText().trim()
                );

                if (n >= 1 && t >= 1) {
                    return new int[]{n, t};
                }

            } catch (NumberFormatException ignored) {
                // Show an error and ask again.
            }

            Alert error = new Alert(
                    Alert.AlertType.ERROR
            );

            error.setTitle("Invalid input");
            error.setHeaderText("Invalid values");
            error.setContentText(
                    "Both n and t must be integers "
                            + "greater than or equal to 1."
            );

            error.showAndWait();
        }
    }

    private void stopWorkers() {

        if (worker1 != null) {
            worker1.interrupt();
        }

        if (worker2 != null) {
            worker2.interrupt();
        }
    }

    private void shutdownThreads() {

        shuttingDown = true;

        if (loadingThread != null) {
            loadingThread.interrupt();
        }

        if (loginController != null) {
            loginController.stopBackgroundThreads();
        }

        stopWorkers();
    }

    @Override
    public void stop() {
        shutdownThreads();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
