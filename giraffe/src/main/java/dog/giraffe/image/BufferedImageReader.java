package dog.giraffe.image;

import dog.giraffe.Context;
import dog.giraffe.points.MutablePoints;
import dog.giraffe.points.UnsignedByteArrayPoints;
import dog.giraffe.points.UnsignedShortArrayPoints;
import dog.giraffe.threads.Continuation;
import dog.giraffe.threads.Supplier;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;

public abstract class BufferedImageReader implements ImageReader {
    private final int height;
    private final MutablePoints points;
    private final int width;

    private BufferedImageReader(int height, MutablePoints points, int width) {
        this.height=height;
        this.points=points;
        this.width=width;
    }

    @Override
    public void close() throws IOException {
    }

    public static BufferedImageReader create(BufferedImage image) {
        switch (image.getSampleModel().getDataType()) {
            case DataBuffer.TYPE_BYTE:
                return createUnsignedByte(image);
            case DataBuffer.TYPE_USHORT:
                return createUnsignedShort(image);
            default:
                throw new RuntimeException("unsupported sample model "+image.getSampleModel());
        }
    }

    private static BufferedImageReader createUnsignedByte(BufferedImage image) {
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
        return new BufferedImageReader(height, new UnsignedByteArrayPoints(data, dimensions), width) {
                    @Override
                    public UnsignedByteArrayPoints createPoints(int dimensions, int expectedSize) {
                        return new UnsignedByteArrayPoints(dimensions, expectedSize);
                    }
                };
    }

    private static BufferedImageReader createUnsignedShort(BufferedImage image) {
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
        return new BufferedImageReader(height, new UnsignedShortArrayPoints(data, dimensions), width) {
                    @Override
                    public UnsignedShortArrayPoints createPoints(int dimensions, int expectedSize) {
                        return new UnsignedShortArrayPoints(dimensions, expectedSize);
                    }
                };
    }

    public static BufferedImageReader create(Path path) throws Throwable {
        return create(ImageIO.read(path.toFile()));
    }

    @Override
    public List<Image> dependencies() {
        return List.of();
    }

    @Override
    public int dimensions() {
        return points.dimensions();
    }

    public static Supplier<ImageReader> factory(Path path) {
        return ()->create(path);
    }

    @Override
    public int height() {
        return height;
    }

    @Override
    public void prepare(Context context, Continuation<Void> continuation) throws Throwable {
        continuation.completed(null);
    }

    @Override
    public Reader reader() {
        return new Reader() {
            @Override
            public Image image() {
                return BufferedImageReader.this;
            }

            @Override
            public void setNormalizedLineTo(int yy, MutablePoints points, int offset) {
                BufferedImageReader.this.points.setNormalizedTo(yy*width, width, points, offset);
            }
        };
    }

    @Override
    public int width() {
        return width;
    }
}
