package dog.giraffe.threads;

public interface AsyncFunction<T, U> {
    void apply(T input, Continuation<U> continuation) throws Throwable;

    static <T, U> AsyncFunction<T, U> async(AsyncFunction<T, U> function, Executor executor) {
        return (input, continuation)->executor.execute(()->function.apply(input, continuation));
    }

    static <T> AsyncFunction<T, T> identity() {
        return (input, continuation)->continuation.completed(input);
    }
}
