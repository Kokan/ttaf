package dog.giraffe.threads;

@FunctionalInterface
public interface Function<T, U> {
    U apply(T value) throws Throwable;
}
