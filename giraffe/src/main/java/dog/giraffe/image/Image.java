package dog.giraffe.image;

import dog.giraffe.Context;
import dog.giraffe.Log;
import dog.giraffe.points.MutablePoints;
import dog.giraffe.threads.Continuation;
import dog.giraffe.threads.Continuations;
import java.util.List;

/**
 * An image is a rectangle of vectors.
 * The main operation supported by an image is reading its contents line by line.
 */
public interface Image extends Log {
    /**
     * Implementation of some trivial methods.
     */
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

            public Dimensions(Image image) {
                this(image.dimensions(), image.height(), image.width());
            }
        }

        private int dimensions;
        protected final List<Image> dependencies;
        private int height;
        private int width;

        /**
         * Creates a new instance that depends on the specified images.
         */
        public Abstract(List<Image> dependencies) {
            this.dependencies=List.copyOf(dependencies);
        }

        @Override
        public List<Image> dependencies() {
            return dependencies;
        }

        @Override
        public int dimensions() {
            return dimensions;
        }

        @Override
        public int height(){
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

        /**
         * Prepares the image and returns the dimensions of this image.
         */
        protected abstract void prepareImpl(Context context, Continuation<Dimensions> continuation) throws Throwable;

        @Override
        public int width(){
            return width;
        }
    }

    /**
     * A {@link Transform} is an image which performs some operation on a line read before
     * returning it to the caller.
     */
    abstract class Transform extends Abstract {
        /**
         * {@link Reader} that performs the transformation line by line.
         */
        protected abstract class TransformReader implements Reader {
            protected final MutablePoints line=image.createPoints(image.dimensions(), image.width());
            protected final Reader reader=image.reader();

            public TransformReader() throws Throwable {
                line.size(image.width());
            }

            @Override
            public void setNormalizedLineTo(int yy, MutablePoints points, int offset) throws Throwable {
                reader.setNormalizedLineTo(yy, line, 0);
                setNormalizedLineToTransform(points, offset);
            }

            /**
             * The transformation applied to every line.
             * The result must be set to points starting at index offset.
             */
            protected abstract void setNormalizedLineToTransform(MutablePoints points, int offset) throws Throwable;
        }

        protected final Image image;

        /**
         * Creates a new instance depending on image.
         */
        public Transform(Image image) {
            super(List.of(image));
            this.image=image;
        }

        @Override
        public MutablePoints createPoints(int dimensions, int expectedSize) throws Throwable {
            return image.createPoints(dimensions, expectedSize);
        }
    }

    /**
     * A Reader contains all information to read a line of the image.
     * Implementation don't have to be thread-safe.
     */
    @FunctionalInterface
    interface Reader {
        /**
         * Reads a line and copies it to points starting at offset.
         *
         * @param yy the index of the line
         */
        void setNormalizedLineTo(int yy, MutablePoints points, int offset) throws Throwable;
    }

    /**
     * Creates a {@link MutablePoints} that is compatible with the image.
     */
    MutablePoints createPoints(int dimensions, int expectedSize) throws Throwable;

    /**
     * Returns the images this image depends on.
     */
    List<Image> dependencies();

    /**
     * Returns the number of dimensions the vectors of this image have.
     */
    int dimensions();

    /**
     * Returns the number of line this image have.
     */
    int height();

    /**
     * Performs all computations this image needs for its line to be readable.
     * This method can be called after all of the dependencies of this image has been prepared.
     */
    void prepare(Context context, Continuation<Void> continuation) throws Throwable;

    /**
     * Returns a new {@link Reader} object that can be used to read lines of this image.
     */
    Reader reader() throws Throwable;

    /**
     * Returns the number of vectors this image have in one line.
     */
    int width();
}
