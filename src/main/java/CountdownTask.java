import javafx.application.Platform;
import javafx.scene.control.Label;

public class CountdownTask implements Runnable {

    // Label used to display the countdown.
    private final Label messageLabel;

    // Time when the account block ends.
    private final long blockEndTime;

    // Action to perform when the block ends.
    private final Runnable onFinished;

    // Constructor.
    public CountdownTask(
            Label messageLabel,
            long remainingBlockMillis,
            Runnable onFinished) {

        this.messageLabel = messageLabel;
        this.blockEndTime = System.currentTimeMillis()
                + remainingBlockMillis;
        this.onFinished = onFinished;
    }

    @Override
    public void run() {

        while (!Thread.currentThread().isInterrupted()) {

            long remainingMillis =
                    blockEndTime - System.currentTimeMillis();

            if (remainingMillis <= 0) {

                Platform.runLater(() -> {
                    messageLabel.setText("");
                    onFinished.run();
                });

                break;
            }

            long remainingSeconds =
                    (remainingMillis + 999) / 1000;

            Platform.runLater(() ->
                    messageLabel.setText(
                            "החשבון חסום, נותרו "
                                    + remainingSeconds
                                    + " שניות"
                    )
            );

            try {
                Thread.sleep(1000);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
