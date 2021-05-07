package dog.giraffe.threads;

public interface AsyncSupplier<T> {
    void get(Continuation<T> continuation) throws Throwable;
}
