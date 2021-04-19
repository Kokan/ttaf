package dog.giraffe.threads.batch;

import dog.giraffe.threads.Block;
import dog.giraffe.threads.Executor;
import java.util.ArrayDeque;
import java.util.Deque;

class SingleThreadedExecutor implements Executor {
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
