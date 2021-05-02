package dog.giraffe.image;

import dog.giraffe.Context;
import dog.giraffe.Log;
import dog.giraffe.points.MutablePoints;
import dog.giraffe.threads.Continuation;
import dog.giraffe.threads.Continuations;
import java.util.List;

public interface Image extends Log {
    abstract class Abstract implements Image {
        protected static class Dimensions {
            public final int dimensions;
            public final int height;
            public final int width;

            public Dimensions(int dimensions, int height, int width) {
                this.dimensions=dimensions;
                this.height=height;
                this.width=width;
            }

            public Dimensions(Image image) throws Throwable {
                this(image.dimensions(), image.height(), image.width());
            }
        }

        private int dimensions;
        protected final List<Image> dependencies;
        private int height;
        private int width;

        public Abstract(List<Image> dependencies) {
            this.dependencies=List.copyOf(dependencies);
        }

        @Override
        public List<Image> dependencies() {
            return dependencies;
        }

        @Override
        public int dimensions() throws Throwable {
            return dimensions;
        }

        @Override
        public int height() throws Throwable {
            return height;
        }

        @Override
        public void prepare(Context context, Continuation<Void> continuation) throws Throwable {
            prepareImpl(
                    context,
                    Continuations.map(
                            (result, continuation2)->{
                                dimensions=result.dimensions;
                                height=result.height;
                                width=result.width;
                                continuation2.completed(null);
                            },
                            continuation));
        }

        protected abstract void prepareImpl(Context context, Continuation<Dimensions> continuation) throws Throwable;

        @Override
        public int width() throws Throwable {
            return width;
        }
    }

    abstract class Transform extends Abstract {
        protected abstract class TransformReader implements Reader {
            protected final MutablePoints line=image.createPoints(image.dimensions(), image.width());
            protected final Reader reader=image.reader();

            public TransformReader() throws Throwable {
                line.size(image.width());
            }

            @Override
            public Image image() {
                return Transform.this;
            }

            @Override
            public void setNormalizedLineTo(int yy, MutablePoints points, int offset) throws Throwable {
                reader.setNormalizedLineTo(yy, line, 0);
                setNormalizedLineToTransform(yy, points, offset);
            }

            protected abstract void setNormalizedLineToTransform(
                    int yy, MutablePoints points, int offset) throws Throwable;
        }

        protected final Image image;

        public Transform(Image image) {
            super(List.of(image));
            this.image=image;
        }

        @Override
        public MutablePoints createPoints(int dimensions, int expectedSize) throws Throwable {
            return image.createPoints(dimensions, expectedSize);
        }
    }

    interface Reader {
        default void addNormalizedLineTo(int yy, MutablePoints points) throws Throwable {
            int size=points.size();
            points.size(size+image().width());
            setNormalizedLineTo(yy, points, size);
        }

        Image image();

        void setNormalizedLineTo(int yy, MutablePoints points, int offset) throws Throwable;
    }

    MutablePoints createPoints(int dimensions, int expectedSize) throws Throwable;

    List<Image> dependencies() throws Throwable;

    int dimensions() throws Throwable;

    int height() throws Throwable;

    void prepare(Context context, Continuation<Void> continuation) throws Throwable;

    Reader reader() throws Throwable;

    int width() throws Throwable;
}
