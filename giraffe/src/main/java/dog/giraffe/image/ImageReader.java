package dog.giraffe.image;

import dog.giraffe.Context;
import dog.giraffe.points.MutablePoints;
import dog.giraffe.threads.Continuation;

/**
 * Implementations must be thread-safe.
 */
public interface ImageReader<P extends MutablePoints> {
    interface Factory {
        <T> void run(Context context, ReadProcess<T> readProcess, Continuation<T> continuation) throws Throwable;
    }

    interface ReadProcess<T> {
        <P extends MutablePoints> void run(
                Context context, ImageReader<P> imageReader, Continuation<T> continuation) throws Throwable;
    }

    void addLineTo(int yy, P points) throws Throwable;

    default P createPoints(int expectedSize) {
        return createPoints(dimensions(), expectedSize);
    }

    P createPoints(int dimensions, int expectedSize);

    int dimensions();
    
    int height();

    int width();
}
