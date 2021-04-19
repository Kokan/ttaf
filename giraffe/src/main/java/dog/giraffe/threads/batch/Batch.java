package dog.giraffe.threads.batch;

import dog.giraffe.Context;
import dog.giraffe.threads.Continuation;
import java.util.Optional;

public interface Batch<T> {
    Optional<T> next() throws Throwable;

    void process(Context context, T value, Continuation<Void> continuation) throws Throwable;
}
