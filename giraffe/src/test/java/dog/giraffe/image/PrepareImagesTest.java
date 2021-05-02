package dog.giraffe.image;

import dog.giraffe.Context;
import dog.giraffe.TestContext;
import dog.giraffe.points.MutablePoints;
import dog.giraffe.threads.Continuation;
import dog.giraffe.threads.batch.SingleThreadedExecutor;
import dog.giraffe.threads.batch.SingleThreadedJoin;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PrepareImagesTest {
    private static class TestImage extends Image.Abstract {
        private int difficulty;
        private final boolean fail;
        private final String name;
        public  boolean prepared;

        public TestImage(String name, int difficulty, boolean fail, Image... dependencies) {
            super(List.of(dependencies));
            this.difficulty=difficulty;
            this.fail=fail;
            this.name=name;
        }

        public void checkPrepared() {
            if (!prepared) {
                throw new RuntimeException("not prepared "+name);
            }
        }

        @Override
        public MutablePoints createPoints(int dimensions, int expectedSize) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void log(Map<String, Object> log) {
            log.put("type", "test-image");
        }

        @Override
        protected void prepareImpl(Context context, Continuation<Dimensions> continuation) throws Throwable {
            for (Image image: dependencies) {
                ((TestImage)image).checkPrepared();
            }
            if (0>=difficulty) {
                prepared=true;
                if (fail) {
                    continuation.failed(new RuntimeException("failed image prepare"));
                }
                else {
                    continuation.completed(new Dimensions(1, 1, 1));
                }
            }
            else {
                --difficulty;
                context.executor().execute(()->prepareImpl(context, continuation));
            }
        }

        @Override
        public Reader reader() {
            throw new UnsupportedOperationException();
        }
    }

    @Test
    public void testCompleted() throws Throwable {
        SingleThreadedExecutor executor=new SingleThreadedExecutor();
        Context context=new TestContext(executor);
        TestImage image0=new TestImage("image0", 0, false);
        TestImage image1=new TestImage("image1", 1, false, image0);
        TestImage image2=new TestImage("image2", 2, false, image0);
        TestImage image3=new TestImage("image3", 3, false, image0);
        TestImage image4=new TestImage("image4", 0, false, image1, image2);
        TestImage image5=new TestImage("image5", 0, false, image1, image3);
        SingleThreadedJoin<Void> join=new SingleThreadedJoin<>();
        PrepareImages.prepareImages(context, List.of(image4, image5), join);
        executor.runJoin(context, join);
        assertTrue(executor.isEmpty());
        for (TestImage image: List.of(image0, image1, image2, image3, image4, image5)) {
            image.checkPrepared();
        }
    }

    @Test
    public void testFailed() throws Throwable {
        SingleThreadedExecutor executor=new SingleThreadedExecutor();
        Context context=new TestContext(executor);
        TestImage image0=new TestImage("image0", 0, false);
        TestImage image1=new TestImage("image1", 10, true, image0);
        TestImage image2=new TestImage("image2", 20, true, image0);
        TestImage image3=new TestImage("image3", 30, false, image0);
        TestImage image4=new TestImage("image4", 0, false, image1, image2);
        TestImage image5=new TestImage("image5", 0, false, image1, image3);
        SingleThreadedJoin<Void> join=new SingleThreadedJoin<>();
        PrepareImages.prepareImages(context, List.of(image4, image5), join);
        try {
            executor.runJoin(context, join);
        }
        catch (RuntimeException ex) {
            assertEquals("java.lang.RuntimeException: failed image prepare", ex.getMessage());
        }
        assertTrue(executor.isEmpty());
        for (TestImage image: List.of(image0, image1, image2, image3)) {
            image.checkPrepared();
        }
    }
}
