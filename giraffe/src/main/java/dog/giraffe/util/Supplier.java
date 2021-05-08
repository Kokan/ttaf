package dog.giraffe.util;

/**
 * An operation that returns a value.
 * Quite similar to {@link java.util.function.Supplier Supplier} but allows checked exceptions.
 */
@FunctionalInterface
public interface Supplier<T> {
    /**
     * Returns a value.
     */
    T get() throws Throwable;
}
