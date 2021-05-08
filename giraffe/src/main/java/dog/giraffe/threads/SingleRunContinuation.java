package dog.giraffe.threads;

import java.util.concurrent.atomic.AtomicBoolean;

class SingleRunContinuation<T> implements Continuation<T> {
    private final AtomicBoolean completed=new AtomicBoolean(false);
    private final Continuation<? super T> continuation;

    private SingleRunContinuation(Continuation<? super T> continuation) {
        this.continuation=continuation;
    }

    private void complete(Throwable throwable) {
        if (!completed.compareAndSet(false, true)) {
            throw new RuntimeException("already completed ("+continuation+")", throwable);
        }
    }

    @Override
    public void completed(T result) throws Throwable {
        complete(null);
        continuation.completed(result);
    }

    @Override
    public void failed(Throwable throwable) throws Throwable {
        complete(throwable);
        continuation.failed(throwable);
    }

    static <T> Continuation<T> wrap(Continuation<T> continuation) {
        return (continuation instanceof SingleRunContinuation)
                ?(SingleRunContinuation<T>)continuation
                :new SingleRunContinuation<>(continuation);
    }
}
