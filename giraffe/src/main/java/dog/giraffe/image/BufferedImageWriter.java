package dog.giraffe.image;

import dog.giraffe.points.UnsignedByteArrayPoints;
import dog.giraffe.util.Consumer;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * An {@link ImageWriter} that writes to an internal memory buffer.
 */
public class BufferedImageWriter implements ImageWriter {
    private class LineImpl implements Line {
        private final int yy;

        public LineImpl(int yy) {
            this.yy=yy;
        }

        @Override
        public void setNormalized(int dimension, int xx, double value) {
            data[dimensions*(yy*width+xx)+dimension]=UnsignedByteArrayPoints.denormalize(value);
        }

        @Override
        public void write() {
        }
    }

    private final byte[] data;
    private final int dimensions;
    private final int height;
    private final Path path;
    private final int width;

    private BufferedImageWriter(int dimensions, int height, Path path, int width) {
        this.dimensions=dimensions;
        this.height=height;
        this.path=path;
        this.width=width;
        data=new byte[dimensions*height*width];
    }

    @Override
    public void close() throws IOException {
    }

    private static BufferedImageWriter create(int width, int height, int dimensions, Consumer<BufferedImage> consumer) {
        return new BufferedImageWriter(dimensions, height, null, width) {
            @Override
            public void close() throws IOException {
                super.close();
                try {
                    consumer.accept(createImage());
                }
                catch (Error|IOException|RuntimeException ex) {
                    throw ex;
                }
                catch (Throwable throwable) {
                    throw new RuntimeException(throwable);
                }
            }
        };
    }

    private static BufferedImageWriter createFile(int width, int height, int dimensions, String format, Path path) {
        return new BufferedImageWriter(dimensions, height, path, width) {
            @Override
            public void close() throws IOException {
                super.close();
                writeImage(format, path);
            }
        };
    }

    /**
     * Creates an image from the memory buffer.
     */
    public BufferedImage createImage() {
        int[] buffer=new int[dimensions*width];
        BufferedImage image=Images.createUnsignedByte(width, height, dimensions);
        for (int ii=0, yy=0; height>yy; ++yy) {
            for (int jj=0, xx=0; width>xx; ++xx) {
                for (int dd=0; dimensions>dd; ++dd, ++ii, ++jj) {
                    buffer[jj]=data[ii]&0xff;
                }
            }
            image.getRaster().setPixels(0, yy, width, 1, buffer);
        }
        return image;
    }

    @Override
    public Line getLine(int yy) {
        return new LineImpl(yy);
    }

    /**
     * Creates a factory for {@link BufferedImageWriter BufferedImageWriters} that upon close
     * supplies the written image to the consumer.
     */
    public static Factory factory(Consumer<BufferedImage> consumer) {
        return (width, height, dimension)->create(width, height, dimension, consumer);
    }

    /**
     * Creates a factory for {@link BufferedImageReader BufferedImageReaders} that is associated with the given path.
     */
    public static Factory factory(String format, Path path) {
        return (width, height, dimension)->createFile(width, height, dimension, format, path);
    }

    @Override
    public void log(Map<String, Object> log) {
        log.put("buffered", true);
        log.put("file", path);
    }

    /**
     * Writes the contents of the memory buffer to the file path.
     */
    public void writeImage(String format, Path path) throws IOException {
        if (!ImageIO.write(createImage(), format, path.toFile())) {
            throw new RuntimeException("no image writer for "+format);
        }
    }
}
