package dog.giraffe.threads;

/**
 * A block of code that waits for the completion of a computation.
 * Quite similar to {@link java.nio.channels.CompletionHandler CompletionHandler} but allows checked exceptions
 * and doesn't support attachments.
 */
public interface Continuation<T> {
    /**
     * Runs this block of code with the result of a successful computation.
     */
    void completed(T result) throws Throwable;

    /**
     * Runs this block of code with the exception of a failed computation.
     */
    void failed(Throwable throwable) throws Throwable;
}
