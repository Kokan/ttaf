package dog.giraffe.threads.batch;

import dog.giraffe.threads.Continuation;

public class SingleThreadedJoin<T> implements Continuation<T> {
    private boolean hasResult;
    private boolean hasThrowable;
    private T result;
    private Throwable throwable;

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

    public T result() {
        return result;
    }
}
