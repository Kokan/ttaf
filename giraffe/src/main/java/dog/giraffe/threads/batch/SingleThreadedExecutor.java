package dog.giraffe.threads.batch;

import dog.giraffe.Context;
import dog.giraffe.threads.Block;
import dog.giraffe.threads.Executor;
import java.util.ArrayDeque;
import java.util.Deque;

public class SingleThreadedExecutor implements Executor {
    private final Deque<Block> deque=new ArrayDeque<>();

    public void clear() {
        deque.clear();
    }

    @Override
    public void execute(Block block) {
        deque.addLast(block);
    }

    public boolean isEmpty() {
        return deque.isEmpty();
    }

    public void runJoin(Context context, SingleThreadedJoin join) throws Throwable {
        while (!join.completed()) {
            context.checkStopped();
            if (isEmpty()) {
                throw new RuntimeException("process not completed");
            }
            runOne();
        }
    }

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
