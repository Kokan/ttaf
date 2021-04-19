package dog.giraffe;

import dog.giraffe.threads.Continuation;
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

    Continuation<Throwable> logger();

    Random random();

    boolean stopped();

    Sum.Factory sum();
}
