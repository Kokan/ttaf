package dog.giraffe.threads.batch;

import dog.giraffe.Context;
import dog.giraffe.TestContext;
import dog.giraffe.threads.Block;
import dog.giraffe.threads.Continuation;
import dog.giraffe.threads.Executor;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class BatchRunnerTest {
    private static class DelegatorExecutor implements Executor {
        private final Executor executor;

        public DelegatorExecutor(Executor executor) {
            this.executor=executor;
        }

        @Override
        public void execute(Block block) throws Throwable {
            executor.execute(block);
        }

        @Override
        public int threads() {
            return executor.threads();
        }
    }

    @FunctionalInterface
    private interface Runner {
        <T> void run(Batch<T> batch, Context context, Continuation<Void> continuation) throws Throwable;

        static Runner parallelSingleThreaded(SingleThreadedExecutor executor, int threads) {
            return new Runner() {
                @Override
                public <T> void run(
                        Batch<T> batch, Context context, Continuation<Void> continuation) throws Throwable {
                    SingleThreadedJoin join=new SingleThreadedJoin();
                    BatchRunner.runParallelSingleThreaded(
                            batch,
                            new DelegatorContext(context) {
                                @Override
                                public Executor executor() {
                                    return new DelegatorExecutor(super.executor()) {
                                        @Override
                                        public int threads() {
                                            return threads;
                                        }
                                    };
                                }
                            },
                            join);
                    while (!join.completed()) {
                        if (executor.isEmpty()) {
                            fail();
                        }
                        executor.runOne();
                    }
                    continuation.completed(null);
                }
            };
        }

        static Runner multiThreaded(SingleThreadedExecutor executor, int parallelism, int threads) {
            return new Runner() {
                @Override
                public <T> void run(
                        Batch<T> batch, Context context, Continuation<Void> continuation) throws Throwable {
                    SingleThreadedJoin join=new SingleThreadedJoin();
                    BatchRunner.runMultiThreaded(
                            batch,
                            new DelegatorContext(context) {
                                @Override
                                public Executor executor() {
                                    return new DelegatorExecutor(super.executor()) {
                                        @Override
                                        public int threads() {
                                            return threads;
                                        }
                                    };
                                }
                            },
                            parallelism,
                            join);
                    while (!join.completed()) {
                        if (executor.isEmpty()) {
                            fail();
                        }
                        executor.runOne();
                    }
                    continuation.completed(null);
                }
            };
        }

        static Runner singleThreaded() {
            return new Runner() {
                @Override
                public <T> void run(
                        Batch<T> batch, Context context, Continuation<Void> continuation) throws Throwable {
                    BatchRunner.runSingleThreaded(batch, context);
                    continuation.completed(null);
                }
            };
        }
    }

    @Test
    public void test() throws Throwable {
        test(
                new TestContext(null),
                1,
                Runner.singleThreaded());
        SingleThreadedExecutor executor=new SingleThreadedExecutor();
        for (int threads: new int[]{1, 3}) {
            test(
                    new TestContext(executor),
                    1,
                    Runner.parallelSingleThreaded(executor, threads));
            for (int parallelism: new int[]{1, 3}) {
                test(
                        new TestContext(executor),
                        parallelism,
                        Runner.multiThreaded(executor, parallelism, threads));
            }
        }
    }

    public void test(Context context, int parallelism, Runner runner) throws Throwable {
        class TestBatch implements Batch<Integer> {
            public int next;
            public int parallelism;
            public int running;

            @Override
            public Optional<Integer> next() {
                if (100>next) {
                    Optional<Integer> result=Optional.of(next);
                    ++next;
                    return result;
                }
                return Optional.empty();
            }

            @Override
            public void process(Context context, Integer value, Continuation<Void> continuation) throws Throwable {
                ++running;
                parallelism=Math.max(parallelism, running);
                context.executor().execute(()->{
                    --running;
                    continuation.completed(null);
                });
            }
        }
        TestBatch batch=new TestBatch();
        SingleThreadedJoin join=new SingleThreadedJoin();
        runner.run(batch, context, join);
        assertTrue(join.completed());
        assertEquals(100, batch.next);
        assertEquals(parallelism, batch.parallelism);
    }
}
