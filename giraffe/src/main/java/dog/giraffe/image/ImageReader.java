package dog.giraffe.image;

import java.io.IOException;

/**
 * An image that can be closed.
 */
public interface ImageReader extends AutoCloseable, Image {
    @Override
    void close() throws IOException;
}
