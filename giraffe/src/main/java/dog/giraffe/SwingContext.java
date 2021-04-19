package dog.giraffe;

import dog.giraffe.threads.Block;
import dog.giraffe.threads.Executor;
import javax.swing.SwingUtilities;

public class SwingContext extends StandardContext {
    private final Executor executorGui;

    public SwingContext(int threads) {
        super(threads);
        executorGui=new Executor() {
            @Override
            public void execute(Block block) throws Throwable {
                checkStopped();
                SwingUtilities.invokeLater(new BlockRunnable(block));
            }

            @Override
            public int threads() {
                return 1;
            }
        };
    }

    public SwingContext() {
        this(Runtime.getRuntime().availableProcessors());
    }

    public Executor executorGui() {
        return executorGui;
    }
}
