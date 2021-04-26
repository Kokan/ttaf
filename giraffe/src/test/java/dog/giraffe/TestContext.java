package dog.giraffe;

import dog.giraffe.threads.Continuation;
import dog.giraffe.threads.Executor;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.fail;

public class TestContext implements Context {
    private final Executor executor;
    private final Random random=new Random(123L);

    public TestContext(Executor executor) {
        this.executor=executor;
    }

    @Override
    public void close() {
        fail();
    }

    @Override
    public Executor executor() {
        return executor;
    }

    @Override
    public Continuation<Throwable> logger() {
        fail();
        return null;
    }

    @Override
    public Random random() {
        return random;
    }

    @Override
    public boolean stopped() {
        return false;
    }

    @Override
    public Sum.Factory sum() {
        return Sum.PREFERRED;
    }
}
