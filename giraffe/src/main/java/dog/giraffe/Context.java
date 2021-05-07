package dog.giraffe;

import dog.giraffe.threads.Executor;
import dog.giraffe.threads.StoppedException;
import java.util.Random;

public interface Context extends AutoCloseable {
    default void checkStopped() throws Throwable {
        if (stopped()) {
            throw new StoppedException();
        }
    }

    @Override
    void close();

    Executor executor();

    Random random();

    boolean stopped();

    Sum.Factory sum();
}
