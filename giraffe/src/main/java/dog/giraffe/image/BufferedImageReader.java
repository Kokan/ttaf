package dog.giraffe.image;

import dog.giraffe.Context;
import dog.giraffe.points.MutablePoints;
import dog.giraffe.points.UnsignedByteArrayPoints;
import dog.giraffe.points.UnsignedShortArrayPoints;
import dog.giraffe.threads.Continuation;
import dog.giraffe.util.Supplier;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * An {@link ImageWriter} that reads line from a memory buffer.
 */
public abstract class BufferedImageReader implements ImageReader {
    private final int height;
    private final Path path;
    private final MutablePoints points;
    private final int width;

    private BufferedImageReader(int height, Path path, MutablePoints points, int width) {
        this.height=height;
        this.path=path;
        this.points=points;
        this.width=width;
    }

    @Override
    public void close() {
    }

    private static BufferedImageReader create(BufferedImage image, Path path) {
        switch (image.getSampleModel().getDataType()) {
            case DataBuffer.TYPE_BYTE:
                return createUnsignedByte(image, path);
            case DataBuffer.TYPE_USHORT:
                return createUnsignedShort(image, path);
            default:
                throw new RuntimeException("unsupported sample model "+image.getSampleModel());
        }
    }

    private static BufferedImageReader createUnsignedByte(BufferedImage image, Path path) {
        int dimensions=image.getSampleModel().getNumBands();
        int height=image.getHeight();
        int width=image.getWidth();
        Raster raster=image.getRaster();
        byte[] data=new byte[dimensions*height*width];
        int[] buffer=new int[dimensions*width];
        for (int ii=0, yy=0; height>yy; ++yy) {
            buffer=raster.getPixels(0, yy, width, 1, buffer);
            for (int jj=0, mm=dimensions*width; mm>jj; ++ii, ++jj) {
                data[ii]=(byte)buffer[jj];
            }
        }
        return new BufferedImageReader(height, path, new UnsignedByteArrayPoints(data, dimensions), width) {
                    @Override
                    public UnsignedByteArrayPoints createPoints(int dimensions, int expectedSize) {
                        return new UnsignedByteArrayPoints(dimensions, expectedSize);
                    }

            @Override
            protected String logType() {
                return "unsigned-byte";
            }
        };
    }

    private static BufferedImageReader createUnsignedShort(BufferedImage image, Path path) {
        int dimensions=image.getSampleModel().getNumBands();
        int height=image.getHeight();
        int width=image.getWidth();
        Raster raster=image.getRaster();
        short[] data=new short[dimensions*height*width];
        int[] buffer=new int[dimensions*width];
        for (int ii=0, yy=0; height>yy; ++yy) {
            buffer=raster.getPixels(0, yy, width, 1, buffer);
            for (int jj=0, mm=dimensions*width; mm>jj; ++ii, ++jj) {
                data[ii]=(short)buffer[jj];
            }
        }
        return new BufferedImageReader(height, path, new UnsignedShortArrayPoints(data, dimensions), width) {
                    @Override
                    public UnsignedShortArrayPoints createPoints(int dimensions, int expectedSize) {
                        return new UnsignedShortArrayPoints(dimensions, expectedSize);
                    }

            @Override
            protected String logType() {
                return "unsigned-short";
            }
        };
    }

    private static BufferedImageReader create(Path path) throws Throwable {
        return create(ImageIO.read(path.toFile()), path);
    }

    @Override
    public List<Image> dependencies() {
        return List.of();
    }

    @Override
    public int dimensions() {
        return points.dimensions();
    }

    /**
     * Create a factory for {@link BufferedImageReader BufferedImageReaders} that buffers the image file path.
     */
    public static Supplier<ImageReader> factory(Path path) {
        return ()->create(path);
    }

    @Override
    public int height() {
        return height;
    }

    @Override
    public void log(Map<String, Object> log) {
        log.put("buffered", true);
        log.put("file", path);
        log.put("type", logType());
    }

    /**
     * Returns the type of the image.
     */
    protected abstract String logType();

    @Override
    public void prepare(Context context, Continuation<Void> continuation) throws Throwable {
        continuation.completed(null);
    }

    @Override
    public Reader reader() {
        return (yy, points, offset)->
                BufferedImageReader.this.points.setNormalizedTo(yy*width, width, points, offset);
    }

    @Override
    public int width() {
        return width;
    }
}
