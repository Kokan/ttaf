package dog.giraffe.threads;

public interface Continuation<T> {
    void completed(T result) throws Throwable;

    void failed(Throwable throwable) throws Throwable;
}
