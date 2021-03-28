package dog.giraffe.threads;

public interface SleepProcess<T> {
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

    void run(Block wakeup, Block sleep, Continuation<T> continuation) throws Throwable;
}
