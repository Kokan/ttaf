package dog.giraffe.threads;

import dog.giraffe.util.Block;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Helper methods to compose asynchronous computations.
 */
public class Continuations {
    /**
     * Decomposition of a list of computations.
     */
    @FunctionalInterface
    public interface IntForks<T> {
        /**
         * Returns a computation corresponding to the interval [from, to).
         */
        AsyncSupplier<T> fork(int from, int to);
    }

    private Continuations() {
    }

    /**
     * Returns a new continuation which on completion submits a task to the executor that completes the
     * specified continuation the same way as it was completed.
     */
    public static <T> Continuation<T> async(Continuation<T> continuation, Executor executor) {
        Continuation<T> continuation2=singleRun(continuation);
        return singleRun(new Continuation<>() {
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

    /**
     * Returns a new continuation which on completion executes a block of code and than completes
     * the specified continuation the same way as it was completed.
     * When the continuation was completed successfully and the block throws an exception
     * the continuation will be completed as a failure.
     */
    public static <T> Continuation<T> finallyBlock(Block block, Continuation<T> continuation) {
        return singleRun(new Continuation<>() {
            @Override
            public void completed(T result) throws Throwable {
                boolean noError=false;
                try {
                    block.run();
                    noError=true;
                }
                catch (Throwable throwable) {
                    continuation.failed(throwable);
                }
                if (noError) {
                    continuation.completed(result);
                }
            }

            @Override
            public void failed(Throwable throwable) throws Throwable {
                try {
                    block.run();
                }
                catch (Throwable throwable2) {
                    if (null==throwable) {
                        throwable=throwable2;
                    }
                    else {
                        throwable.addSuppressed(throwable2);
                    }
                }
                continuation.failed(throwable);
            }
        });
    }

    /**
     * Executes all of the computations of forks in parallel.
     * When all of the computations were successful then it will complete the join with the list of all of the results.
     * The position of the results correspond to the positions of the suppliers in forks that generated it.
     * When any of the computation has failed then it will complete the join unsuccessfully.
     *
     * @param executor the executor used to run all forks and the join
     */
    public static <T> void forkJoin(
            List<AsyncSupplier<T>> forks, Continuation<List<T>> join, Executor executor) throws Throwable {
        Continuation<List<T>> join2=singleRun(async(singleRun(join), executor));
        if (forks.isEmpty()) {
            join2.completed(new ArrayList<>(0));
            return;
        }
        AtomicInteger remaining=new AtomicInteger(forks.size());
        List<T> results=new ArrayList<>(forks.size());
        Continuation<Void> continuation=new Continuation<>() {
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

    /**
     * Helper method for {@link #forkJoin(List, Continuation, Executor)}.
     * Executes all the tasks of forks in the interval [forksFrom, forksTo) in parallel.
     * After all the forks completed it will complete join.
     * The interval will be split uniformly into executor.threads() parts.
     */
    public static <T> void forkJoin(
            IntForks<T> forks, int forksFrom, int forksTo, Continuation<List<T>> join, Executor executor)
            throws Throwable {
        if (forksFrom>=forksTo) {
            join.completed(List.of());
            return;
        }
        int length=forksTo-forksFrom;
        int threads=Math.max(1, Math.min(executor.threads(), length));
        List<AsyncSupplier<T>> forks2=new ArrayList<>(threads);
        for (int tt=0; threads>tt; ++tt) {
            forks2.add(forks.fork(forksFrom+tt*length/threads, forksFrom+(tt+1)*length/threads));
        }
        Continuations.forkJoin(forks2, join, executor);
    }

    /**
     * Maps the result of a computation.
     * When the computation is successful it will run the function with the result
     * which may produce a new value or fail.
     * When the computation fails this will fail the continuation.
     */
    public static <T, U> Continuation<T> map(AsyncFunction<T, U> function, Continuation<U> continuation) {
        Continuation<U> continuation2=singleRun(continuation);
        return singleRun(new Continuation<>() {
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

    /**
     * Executes all of the computations of steps in sequentially.
     * When all of the computations were successful then it will complete the join with the list of all of the results.
     * The position of the results correspond to the positions of the suppliers in steps that generated it.
     * When any of the computation has failed then it will complete the join unsuccessfully.
     *
     * @param executor the executor used to run all steps and the join
     */
    public static <T> void sequence(
            List<AsyncSupplier<T>> steps, Continuation<List<T>> continuation, Executor executor) throws Throwable {
        sequence(steps, continuation, executor, new ArrayList<>(steps.size()));
    }

    private static <T> void sequence(
            List<AsyncSupplier<T>> steps, Continuation<List<T>> continuation, Executor executor,
            List<T> results) throws Throwable {
        if (steps.size()<=results.size()) {
            continuation.completed(results);
            return;
        }
        steps.get(results.size()).get(
                Continuations.async(
                        Continuations.map(
                                (result, continuation2)->{
                                    results.add(result);
                                    sequence(steps, continuation2, executor, results);
                                },
                                continuation),
                        executor));
    }

    /**
     * Returns a new continuation which can be completed at most once.
     * When the continuation is completed more than once it will raise an exception in the current thread.
     */
    public static <T> Continuation<T> singleRun(Continuation<T> continuation) {
        return SingleRunContinuation.wrap(continuation);
    }
}
