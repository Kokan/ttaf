package dog.giraffe;

import dog.giraffe.points.Sum;
import dog.giraffe.threads.Executor;
import java.util.Random;

/**
 * Runtime context of an asynchronous computation. This interface provides hooks to interact with the host system.
 *
 * Contexts can be closed. Closed context may not accept new tasks for execution.
 */
public interface Context extends AutoCloseable {
    /**
     * Throws StoppedException if this Context is closed.
     */
    default void checkStopped() throws Throwable {
        if (stopped()) {
            throw new StoppedException();
        }
    }

    /**
     * Closes this Context.
     */
    @Override
    void close();

    /**
     * Returns the executor associated with this Context.
     */
    Executor executor();

    /**
     * Returns a thread-safe source of randomness.
     */
    Random random();

    /**
     * Returns whether this Context has been closed.
     */
    boolean stopped();

    /**
     * Returns the Sum factory associated with this Context.
     */
    Sum.Factory sum();
}
