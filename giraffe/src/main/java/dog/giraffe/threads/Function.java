package dog.giraffe.threads;

@FunctionalInterface
public interface Function<T, U> {
    U apply(T value) throws Throwable;

    default <S> Function<S, U> compose(Function<S, T> before) {
        return (value)->apply(before.apply(value));
    }

    static <T> Function<T, T> identity() {
        return (value)->value;
    }
}
