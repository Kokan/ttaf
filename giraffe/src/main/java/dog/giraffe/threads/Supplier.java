package dog.giraffe.threads;

@FunctionalInterface
public interface Supplier<T> {
    T get() throws Throwable;
}
