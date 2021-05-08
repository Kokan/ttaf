package dog.giraffe.threads;

import dog.giraffe.Context;
import dog.giraffe.util.Block;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * An {@link Executor} with no threads of its own.
 * The execution of submitted tasks requires the owner of the executor to lend some resources.
 * The tasks are kept in a queue.
 * The owner of the executor must periodically call {@link #runOne()} to execute the task at the head of the queue.
 *
 * SingleThreadedExecutors are not thread-safe.
 */
public class SingleThreadedExecutor implements Executor {
    private final Deque<Block> deque=new ArrayDeque<>();

    /**
     * Removes all of the tasks in the queue without executing them.
     */
    public void clear() {
        deque.clear();
    }

    /**
     * Adds a task to the tail of the queue.
     */
    @Override
    public void execute(Block block) {
        deque.addLast(block);
    }

    /**
     * Returns whether the task queue is empty.
     */
    public boolean isEmpty() {
        return deque.isEmpty();
    }

    /**
     * Runs tasks while the join is not completed.
     * If the join is not completed and there are no more tasks to run it will raise an exception.
     *
     * @param context context is used to check whether the context is closed
     */
    public <T> T runJoin(Context context, SingleThreadedJoin<T> join) throws Throwable {
        while (!join.completed()) {
            context.checkStopped();
            if (isEmpty()) {
                throw new RuntimeException("process not completed");
            }
            runOne();
        }
        return join.result();
    }

    /**
     * If the queue is not empty it will execute the task at the head of the queue.
     */
    public void runOne() throws Throwable {
        Block block=deque.pollFirst();
        if (null!=block) {
            block.run();
        }
    }

    @Override
    public int threads() {
        return 1;
    }
}
