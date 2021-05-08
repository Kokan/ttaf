package dog.giraffe.threads;

/**
 * Asynchronous form of {@link dog.giraffe.util.Function Function}.
 */
public interface AsyncFunction<T, U> {
    /**
     * Completes the continuation with then object mapped to input.
     */
    void apply(T input, Continuation<U> continuation) throws Throwable;
}
