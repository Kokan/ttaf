package dog.giraffe.threads;

public class AsyncJoin<T> implements Continuation<T> {
    private Throwable error;
    private boolean hasError;
    private boolean hasResult;
    private final Object lock=new Object();
    private T result;

    @Override
    public void completed(T result) throws Throwable {
        synchronized (lock) {
            if (hasError || hasResult) {
                throw new RuntimeException("already completed");
            }
            hasResult=true;
            this.result=result;
            lock.notifyAll();
        }
    }

    @Override
    public void failed(Throwable throwable) throws Throwable {
        synchronized (lock) {
            if (hasError || hasResult) {
                throw new RuntimeException("already completed");
            }
            error=throwable;
            hasError=true;
            lock.notifyAll();
        }
    }

    public T join() throws Throwable {
        synchronized (lock) {
            while (true) {
                if (hasError) {
                    throw new RuntimeException(error);
                }
                if (hasResult) {
                    return result;
                }
                lock.wait();
            }
        }
    }
}
