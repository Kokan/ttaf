package dog.giraffe.gui;

import dog.giraffe.Context;
import dog.giraffe.points.Sum;
import dog.giraffe.threads.Executor;
import java.util.Random;

public class SubContext implements Context {
    private volatile boolean closed;
    private final Context context;

    public SubContext(Context context) {
        this.context=context;
    }

    @Override
    public void close() {
        closed=true;
    }

    @Override
    public Executor executor() {
        return context.executor();
    }

    @Override
    public Random random() {
        return context.random();
    }

    @Override
    public boolean stopped() {
        return closed;
    }

    @Override
    public Sum.Factory sum() {
        return context.sum();
    }
}
