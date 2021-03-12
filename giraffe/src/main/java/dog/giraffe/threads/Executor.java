package dog.giraffe.threads;

public interface Executor {
    void execute(Block block) throws Throwable;

    int threads() throws Throwable;
}
