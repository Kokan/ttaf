package dog.giraffe.image;

import dog.giraffe.Context;
import dog.giraffe.points.MutablePoints;
import dog.giraffe.threads.Continuation;
import dog.giraffe.threads.Continuations;
import java.io.IOException;
import java.nio.file.Path;

public interface ImageWriter extends AutoCloseable {
    interface Factory {
        ImageWriter create(int width, int height, int dimension) throws Throwable;
    }

    interface FileFactory {
        ImageWriter create(int width, int height, int dimension, String format, Path path) throws Throwable;
    }

    interface Line {
        void set(int dimension, int xx, double value);

        void setNormalized(int dimension, int xx, double value);

        void write() throws Throwable;
    }

    void close() throws IOException;

    Line getLine(int yy) throws Throwable;

    static void write(
            Context context, Image image, ImageWriter imageWriter, Continuation<Void> continuation) throws Throwable {
        Continuations.forkJoin(
                (from, to)->(continuation2)->{
                    Image.Reader reader=image.reader();
                    MutablePoints points=image.createPoints(image.dimensions(), image.width());
                    for (int yy=from; to>yy; ++yy) {
                        reader.setNormalizedLineTo(yy, points, 0);
                        ImageWriter.Line line=imageWriter.getLine(yy);
                        for (int xx=0; image.width()>xx; ++xx) {
                            for (int dd=0; image.dimensions()>dd; ++dd) {
                                line.setNormalized(dd, xx, points.getNormalized(dd, xx));
                            }
                        }
                        line.write();
                    }
                    continuation2.completed(null);
                },
                0,
                image.height(),
                Continuations.map(
                        (input, continuation2)->continuation2.completed(null),
                        continuation),
                context.executor());
    }
}
