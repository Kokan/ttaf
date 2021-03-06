package dog.giraffe.threads;

public interface Consumer<T> {
    void accept(T value) throws Throwable;
}
