package dog.giraffe;

import dog.giraffe.points.Sum;
import dog.giraffe.threads.Executor;
import dog.giraffe.util.Block;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Default implementation of {@link dog.giraffe.Context Context}.
 *
 * Threads are run asynchronously by a standard java executor.
 *
 * Logs errors to the standard error.
 */
public class StandardContext implements Context {
    /**
     * Wrapper for {@link Block Blocks}.
     * Exceptions thrown by the Block will be logged to the standard error.
     */
    protected class BlockRunnable implements Runnable {
        private final Block block;

        public BlockRunnable(Block block) {
            this.block=block;
        }

        @Override
        public void run() {
            try {
                checkStopped();
                block.run();
            }
            catch (Throwable throwable) {
                log(throwable);
            }
        }
    }

    private final Executor executor;
    private final ScheduledExecutorService realExecutor;
    private final AtomicBoolean stopped=new AtomicBoolean(false);

    /**
     * Creates a new instance.
     *
     * @param threads the number of java threads used to run tasks
     */
    public StandardContext(int threads) {
        executor=new Executor() {
            @Override
            public void execute(Block block) throws Throwable {
                checkStopped();
                realExecutor.execute(new BlockRunnable(block));
            }

            @Override
            public int threads() {
                return threads;
            }
        };
        realExecutor=Executors.newScheduledThreadPool(
                threads,
                (runnable)->{
                    Thread thread=new Thread(runnable);
                    thread.setDaemon(true);
                    return thread;
                });
    }

    @Override
    public void close() {
        if (stopped.compareAndSet(false, true)) {
            realExecutor.shutdown();
        }
    }

    @Override
    public Executor executor() {
        return executor;
    }

    private void log(Throwable throwable) {
        if (null==throwable) {
            throwable=new NullPointerException("throwable");
        }
        for (Throwable throwable2=throwable; null!=throwable2; throwable2=throwable2.getCause()) {
            if (throwable2 instanceof StoppedException) {
                return;
            }
        }
        synchronized (this) {
            throwable.printStackTrace(System.err);
            System.err.flush();
        }
    }

    @Override
    public Random random() {
        return ThreadLocalRandom.current();
    }

    @Override
    public boolean stopped() {
        return stopped.get();
    }

    @Override
    public Sum.Factory sum() {
        return Sum.SINGLE_VARIABLE;
    }
}
