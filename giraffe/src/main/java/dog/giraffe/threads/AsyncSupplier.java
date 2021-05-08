package dog.giraffe.threads;

/**
 * Asynchronous form of {@link java.util.function.Supplier Supplier}.
 */
public interface AsyncSupplier<T> {
    /**
     * Completes the continuation with a value.
     */
    void get(Continuation<T> continuation) throws Throwable;
}
