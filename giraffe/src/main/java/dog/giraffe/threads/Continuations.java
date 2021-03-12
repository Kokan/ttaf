package dog.giraffe.threads;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Continuations {
    private Continuations() {
    }

    public static <T> Continuation<T> async(Continuation<T> continuation, Executor executor) {
        Continuation<T> continuation2=singleRun(continuation);
        return singleRun(new Continuation<T>() {
            @Override
            public void completed(T result) throws Throwable {
                try {
                    executor.execute(()->continuation2.completed(result));
                }
                catch (Throwable throwable) {
                    continuation2.failed(throwable);
                }
            }

            @Override
            public void failed(Throwable throwable) throws Throwable {
                try {
                    executor.execute(()->continuation2.failed(throwable));
                }
                catch (Throwable throwable2) {
                    continuation2.failed(throwable2);
                }
            }
        });
    }

    public static <T> Continuation<T> consume(Consumer<T> consumer, Continuation<Throwable> logger) {
        return singleRun(new Continuation<T>() {
            @Override
            public void completed(T result) throws Throwable {
                consumer.accept(result);
            }

            @Override
            public void failed(Throwable throwable) throws Throwable {
                logger.failed(throwable);
            }
        });
    }

    public static <T> void forkJoin(
            List<AsyncSupplier<T>> forks, Continuation<List<T>> join, Executor executor) throws Throwable {
        Continuation<List<T>> join2=singleRun(async(singleRun(join), executor));
        if (forks.isEmpty()) {
            join2.completed(new ArrayList<>(0));
            return;
        }
        AtomicInteger remaining=new AtomicInteger(forks.size());
        List<T> results=new ArrayList<>(forks.size());
        Continuation<Void> continuation=new Continuation<Void>() {
            @Override
            public void completed(Void result) throws Throwable {
                while (true) {
                    int remaining2=remaining.get();
                    if (0>=remaining2) {
                        throw new RuntimeException("already completed");
                    }
                    if (remaining.compareAndSet(remaining2, remaining2-1)) {
                        if (1==remaining2) {
                            join2.completed(results);
                        }
                        break;
                    }
                }
            }

            @Override
            public void failed(Throwable throwable) throws Throwable {
                while (true) {
                    int remaining2=remaining.get();
                    if (0>=remaining2) {
                        throw new RuntimeException("already completed", throwable);
                    }
                    if (remaining.compareAndSet(remaining2, 0)) {
                        join2.failed(throwable);
                        break;
                    }
                }
            }
        };
        try {
            for (int ii=forks.size(); 0<ii; --ii) {
                results.add(null);
            }
            for (int ii=0; forks.size()>ii; ++ii) {
                int jj=ii;
                executor.execute(()->{
                    try {
                        if (0<remaining.get()) {
                            forks.get(jj).get(map(
                                    (result, continuation2)->{
                                        synchronized (results) {
                                            results.set(jj, result);
                                        }
                                        continuation2.completed(null);
                                    },
                                    continuation));
                        }
                    }
                    catch (Throwable throwable) {
                        continuation.failed(throwable);
                    }
                });
            }
        }
        catch (Throwable throwable) {
            continuation.failed(throwable);
        }
    }

    public static <T, U> Continuation<T> map(AsyncFunction<T, U> function, Continuation<U> continuation) {
        Continuation<U> continuation2=singleRun(continuation);
        return singleRun(new Continuation<T>() {
            @Override
            public void completed(T result) throws Throwable {
                try {
                    function.apply(result, continuation2);
                }
                catch (Throwable throwable) {
                    continuation2.failed(throwable);
                }
            }

            @Override
            public void failed(Throwable throwable) throws Throwable {
                continuation2.failed(throwable);
            }
        });
    }

    public static <T> Continuation<T> singleRun(Continuation<T> continuation) {
        return SingleRunContinuation.wrap(continuation);
    }
}
