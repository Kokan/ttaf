package dog.giraffe.threads;

public class AsyncJoin implements Continuation<Void> {
    private Throwable error;
    private boolean hasError;
    private boolean hasResult;
    private final Object lock=new Object();

    @Override
    public void completed(Void result) {
        synchronized (lock) {
            if (hasError || hasResult) {
                throw new RuntimeException("already completed");
            }
            hasResult=true;
            lock.notifyAll();
        }
    }

    @Override
    public void failed(Throwable throwable) {
        synchronized (lock) {
            if (hasError || hasResult) {
                throw new RuntimeException("already completed");
            }
            error=throwable;
            hasError=true;
            lock.notifyAll();
        }
    }

    public void join() throws Throwable {
        synchronized (lock) {
            while (true) {
                if (hasError) {
                    throw new RuntimeException(error);
                }
                if (hasResult) {
                    return;
                }
                lock.wait();
            }
        }
    }
}
