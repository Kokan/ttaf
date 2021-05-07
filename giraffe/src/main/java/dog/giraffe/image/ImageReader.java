package dog.giraffe.image;

import java.io.IOException;

public interface ImageReader extends AutoCloseable, Image {
    @Override
    void close() throws IOException;
}
