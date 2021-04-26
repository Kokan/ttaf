package dog.giraffe.image;

import java.io.IOException;
import java.nio.file.Path;

public interface ImageReader extends AutoCloseable, Image {
    interface FileFactory {
        ImageReader create(Path path) throws Throwable;
    }

    @Override
    void close() throws IOException;
}
