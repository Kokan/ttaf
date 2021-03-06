package dog.giraffe.threads;

public interface Executor {
    void execute(Block block) throws Throwable;

    void execute(Block block, long delayMillis) throws Throwable;

    int threads() throws Throwable;
}
