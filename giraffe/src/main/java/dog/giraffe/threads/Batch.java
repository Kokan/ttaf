package dog.giraffe.threads;

import dog.giraffe.Context;
import java.util.Optional;

/**
 * A Batch represents a large number of tasks to be run.
 */
public interface Batch<T> {
    /**
     * Generates a task to be run.
     * Returns an empty optional when there no more tasks.
     */
    Optional<T> next() throws Throwable;

    /**
     * Runs the task value.
     */
    void process(Context context, T value, Continuation<Void> continuation) throws Throwable;
}
