package dog.giraffe.threads.batch;

import dog.giraffe.threads.Continuation;

class Join implements Continuation<Void> {
    private boolean hasResult;
    private boolean hasThrowable;
    private Throwable throwable;

    public boolean completed() throws Throwable {
        if (hasThrowable) {
            throw new RuntimeException(throwable);
        }
        return hasResult;
    }

    @Override
    public void completed(Void result) throws Throwable {
        if (hasResult || hasThrowable) {
            throw new RuntimeException("already completed");
        }
        hasResult=true;
    }

    @Override
    public void failed(Throwable throwable) throws Throwable {
        if (hasResult || hasThrowable) {
            throw new RuntimeException("already completed");
        }
        hasThrowable=true;
        this.throwable=throwable;
    }
}
