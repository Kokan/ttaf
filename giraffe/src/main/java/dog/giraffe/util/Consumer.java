package dog.giraffe.util;

/**
 * An operation that accepts values.
 * Quite similar to {@link java.util.function.Consumer Consumer} but allows checked exceptions.
 */
public interface Consumer<T> {
    /**
     * Performs an operation on the value.
     */
    void accept(T value) throws Throwable;
}
