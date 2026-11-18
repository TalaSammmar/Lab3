public class ExperimentCounter {

    private int count;
    private boolean blocked;

    public ExperimentCounter() {
        count = 0;
        blocked = false;
    }

    public synchronized int recordFailureSynchronized() {
        if (blocked) {
            return count;
        }

        count++;

        if (count >= 10000) {
            blocked = true;
        }

        return count;
    }

    public int recordFailureUnsynchronized() {
        if (blocked) {
            return count;
        }

        int current = count;
        Thread.yield();
        count = current + 1;

        if (count >= 10000) {
            blocked = true;
        }

        return count;
    }

    public synchronized int getCount() {
        return count;
    }

    public synchronized boolean isBlocked() {
        return blocked;
    }
}