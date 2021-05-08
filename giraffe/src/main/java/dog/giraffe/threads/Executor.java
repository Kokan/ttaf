package dog.giraffe.threads;

import dog.giraffe.util.Block;

/**
 * Executors accept tasks for execution.
 *
 * A task most not block waiting for the completion of another task.
 *
 * Executors must not call a block in the context of another block.
 * Implementations like <code>void execute(Block block){block.run();}</code> can interfere locks.
 */
public interface Executor {
    /**
     * Run block eventually.
     */
    void execute(Block block) throws Throwable;

    /**
     * Returns the maximum number of tasks that can be executed in parallel.
     */
    int threads();
}
