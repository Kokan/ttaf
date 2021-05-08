package dog.giraffe.util;

/**
 * A generic function, a mapping between objects.
 * Quite similar to {@link java.util.function.Function Function} but allows checked exceptions.
 */
@FunctionalInterface
public interface Function<T, U> {
    /**
     * Returns the object mapped to value.
     */
    U apply(T value) throws Throwable;

    /**
     * Standard function composition. this.compose(before) = this o before = x -&gt; this(before(x)).
     */
    default <S> Function<S, U> compose(Function<S, T> before) {
        return (value)->apply(before.apply(value));
    }

    /**
     * Returns the identity function for any type.
     */
    static <T> Function<T, T> identity() {
        return (value)->value;
    }
}
