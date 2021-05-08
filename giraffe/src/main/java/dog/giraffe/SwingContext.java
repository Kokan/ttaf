package dog.giraffe;

import dog.giraffe.util.Block;
import dog.giraffe.threads.Executor;
import javax.swing.SwingUtilities;

/**
 * {@link dog.giraffe.Context Context} for Swing applications.
 */
public class SwingContext extends StandardContext {
    private final Executor executorGui;

    /**
     * Creates a new instance with the specified number of threads backing the executor.
     */
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

    /**
     * Creates a new instance with a thread for every available processor.
     */
    public SwingContext() {
        this(Runtime.getRuntime().availableProcessors());
    }

    /**
     * Returns an executor which runs tasks on the AWT event-dispatch thread.
     */
    public Executor executorGui() {
        return executorGui;
    }
}
