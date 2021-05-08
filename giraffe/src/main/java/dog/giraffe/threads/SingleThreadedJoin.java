package dog.giraffe.threads;

/**
 * Waits non-blocking for an asynchronous computation.
 * SingleThreadedJoins are not thread-safe.
 */
public class SingleThreadedJoin<T> implements Continuation<T> {
    private boolean hasResult;
    private boolean hasThrowable;
    private T result;
    private Throwable throwable;

    /**
     * Returns whether this object has been completed.
     */
    public boolean completed() {
        if (hasThrowable) {
            throw new RuntimeException(throwable);
        }
        return hasResult;
    }

    @Override
    public void completed(T result) {
        if (hasResult || hasThrowable) {
            throw new RuntimeException("already completed");
        }
        hasResult=true;
        this.result=result;
    }

    @Override
    public void failed(Throwable throwable) {
        if (hasResult || hasThrowable) {
            throw new RuntimeException("already completed");
        }
        hasThrowable=true;
        this.throwable=throwable;
    }

    /**
     * If this object is successfully completed it returns the result.
     * If this object is unsuccessfully completed it raises an exception.
     * If this object is not completed then the result of calling this method is undefined.
     */
    public T result() {
        return result;
    }
}
