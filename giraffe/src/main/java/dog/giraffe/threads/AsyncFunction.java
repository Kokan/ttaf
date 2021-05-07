package dog.giraffe.threads;

public interface AsyncFunction<T, U> {
    void apply(T input, Continuation<U> continuation) throws Throwable;
}
