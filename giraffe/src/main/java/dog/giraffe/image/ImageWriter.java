package dog.giraffe.image;

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
}
