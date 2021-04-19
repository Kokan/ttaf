package dog.giraffe;

import dog.giraffe.threads.Block;
import dog.giraffe.threads.Continuation;
import dog.giraffe.threads.Executor;
import dog.giraffe.threads.StoppedException;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

public class StandardContext implements Context {
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
    private final Continuation<Throwable> logger;
    private final ScheduledExecutorService realExecutor;
    private final AtomicBoolean stopped=new AtomicBoolean(false);

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
        logger=new Continuation<>() {
            @Override
            public void completed(Throwable result) {
                log(result);
            }

            @Override
            public void failed(Throwable throwable) {
                log(throwable);
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

    public StandardContext() {
        this(Runtime.getRuntime().availableProcessors());
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
        System.exit(1);
    }

    @Override
    public Continuation<Throwable> logger() {
        return logger;
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
        return Sum.PREFERRED;
    }
}
