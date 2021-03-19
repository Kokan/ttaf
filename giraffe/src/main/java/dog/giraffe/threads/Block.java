package dog.giraffe.threads;

import java.util.Objects;

public interface Block {
    void run() throws Throwable;

    static <T> Block supply(AsyncSupplier<T> supplier, Continuation<T> continuation) {
        Continuation<T> continuation2
                =Continuations.singleRun(Objects.requireNonNull(continuation, "continuation"));
        return ()->{
            try {
                supplier.get(continuation2);
            }
            catch (Throwable throwable) {
                continuation2.failed(throwable);
            }
        };
    }
}
