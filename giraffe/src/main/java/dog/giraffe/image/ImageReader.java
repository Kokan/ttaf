package dog.giraffe.image;

import dog.giraffe.Context;
import dog.giraffe.points.MutablePoints;
import dog.giraffe.threads.Continuation;

/**
 * Implementations must be thread-safe.
 */
public interface ImageReader {
    interface Factory {
        <T> void run(Context context, ReadProcess<T> readProcess, Continuation<T> continuation) throws Throwable;
    }

    interface ReadProcess<T> {
        void run(Context context, ImageReader imageReader, Continuation<T> continuation) throws Throwable;
    }

    void addLineTo(int yy, MutablePoints points) throws Throwable;

    default MutablePoints createPoints(int expectedSize) {
        return createPoints(dimensions(), expectedSize);
    }

    MutablePoints createPoints(int dimensions, int expectedSize);

    int dimensions();
    
    int height();

    int width();
}
