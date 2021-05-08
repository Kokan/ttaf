package dog.giraffe.threads;

import dog.giraffe.util.Block;

/**
 * An asynchronous computation that waits for an external event.
 * {@link #run(Block, Block, Continuation)} is called repeatedly.
 * At each call the process can choose to complete the computation or to sleep.
 * The wakeup method can be used by the external event to cause a call to {@link #run(Block, Block, Continuation)}.
 */
public interface SleepProcess<T> {
    /**
     * Starts a new {@link SleepProcess}.
     *
     * @param executor executor is used to run calls to {@link #run(Block, Block, Continuation)}.
     * @return a wakeup block associated with process
     */
    static <T> Block create(Continuation<T> continuation, Executor executor, SleepProcess<T> process) {
        class Wakeup implements Block {
            class SleepContinuation implements Block, Continuation<T> {
                private boolean completed2;

                @Override
                public void completed(T result) throws Throwable {
                    synchronized (lock) {
                        if (completed2) {
                            throw new RuntimeException("already completed");
                        }
                        completed2=true;
                        completed=true;
                    }
                    continuation.completed(result);
                }

                @Override
                public void failed(Throwable throwable) throws Throwable {
                    synchronized (lock) {
                        if (completed2) {
                            throw new RuntimeException("already completed");
                        }
                        completed2=true;
                        completed=true;
                    }
                    continuation.failed(throwable);
                }

                @Override
                public void run() throws Throwable {
                    synchronized (lock) {
                        if (completed2) {
                            throw new RuntimeException("already completed");
                        }
                        completed2=true;
                        if (!wakeup) {
                            running=false;
                            return;
                        }
                        wakeup=false;
                    }
                    execute();
                }
            }

            private boolean completed;
            private final Object lock=new Object();
            private boolean running;
            private boolean wakeup;

            private void execute() throws Throwable {
                SleepContinuation sleepContinuation=new SleepContinuation();
                executor.execute(()->{
                    try {
                        process.run(Wakeup.this, sleepContinuation, sleepContinuation);
                    }
                    catch (Throwable throwable) {
                        sleepContinuation.failed(throwable);
                    }
                });
            }

            @Override
            public void run() throws Throwable {
                synchronized (lock) {
                    if (completed) {
                        return;
                    }
                    if (running) {
                        wakeup=true;
                        return;
                    }
                    running=true;
                }
                execute();
            }
        }
        return new Wakeup();
    }

    /**
     * Completes the continuation
     * or calls sleep to signify it can not proceed with the computation in its current state.
     * This method cannot be called while another call is in progress.
     * A call is considered completed when the continuation is completed or sleep is called.
     *
     * @param wakeup wakeup can be used to cause the run method to be called eventually
     */
    void run(Block wakeup, Block sleep, Continuation<T> continuation) throws Throwable;
}
