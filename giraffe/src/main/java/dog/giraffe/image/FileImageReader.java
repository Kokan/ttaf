package dog.giraffe.image;

import dog.giraffe.Context;
import dog.giraffe.points.MutablePoints;
import dog.giraffe.points.UnsignedByteArrayPoints;
import dog.giraffe.points.UnsignedShortArrayPoints;
import dog.giraffe.threads.Continuation;
import dog.giraffe.threads.Supplier;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;

public abstract class FileImageReader implements ImageReader {
    private static class UnsignedByte extends FileImageReader {
        public UnsignedByte(
                ImageInputStream imageInputStream, javax.imageio.ImageReader imageReader, Path path) throws Throwable {
            super(imageInputStream, imageReader, path);
        }

        @Override
        public UnsignedByteArrayPoints createPoints(int dimensions, int expectedSize) {
            return new UnsignedByteArrayPoints(dimensions, expectedSize);
        }

        @Override
        protected String logType() {
            return "unsigned-byte";
        }

        @Override
        protected void setNormalized(MutablePoints points, int dimension, int index, int value) {
            points.set(dimension, index, (byte)(value&0xff));
        }
    }

    private static class UnsignedShort extends FileImageReader {
        public UnsignedShort(
                ImageInputStream imageInputStream, javax.imageio.ImageReader imageReader, Path path) throws Throwable {
            super(imageInputStream, imageReader, path);
        }

        @Override
        public UnsignedShortArrayPoints createPoints(int dimensions, int expectedSize) {
            return new UnsignedShortArrayPoints(dimensions, expectedSize);
        }

        @Override
        protected String logType() {
            return "unsigned-short";
        }

        @Override
        protected void setNormalized(MutablePoints points, int dimension, int index, int value) {
            points.set(dimension, index, (short)(value&0xffff));
        }
    }

    protected final int dimensions;
    protected final int height;
    protected final ImageInputStream imageInputStream;
    protected final javax.imageio.ImageReader imageReader;
    protected final Object lock=new Object();
    private final Path path;
    protected final int width;

    public FileImageReader(
            ImageInputStream imageInputStream, javax.imageio.ImageReader imageReader, Path path) throws Throwable {
        this.imageInputStream=imageInputStream;
        this.imageReader=imageReader;
        this.path=path;
        dimensions=imageReader.getRawImageType(0).getNumBands();
        height=imageReader.getHeight(0);
        width=imageReader.getWidth(0);
    }

    @Override
    public void close() throws IOException {
        try {
            imageReader.dispose();
        }
        finally {
            imageInputStream.close();
        }
    }

    public static FileImageReader create(Path path) throws Throwable {
        boolean error=true;
        ImageInputStream iis=new FileImageInputStream(path.toFile());
        try {
            javax.imageio.ImageReader ir
                    =Objects.requireNonNull(ImageIO.getImageReaders(iis).next(), "imageReader");
            try {
                ir.setInput(iis);
                FileImageReader result;
                switch (ir.getRawImageType(0).getSampleModel().getDataType()) {
                    case DataBuffer.TYPE_BYTE:
                        result=new UnsignedByte(iis, ir, path);
                        break;
                    case DataBuffer.TYPE_USHORT:
                        result=new UnsignedShort(iis, ir, path);
                        break;
                    default:
                        throw new RuntimeException(
                                "unsupported sample model "+ir.getRawImageType(0).getSampleModel());
                }
                error=false;
                return result;
            }
            finally {
                if (error) {
                    ir.dispose();
                }
            }
        }
        finally {
            if (error) {
                iis.close();
            }
        }
    }

    @Override
    public List<Image> dependencies() {
        return List.of();
    }

    @Override
    public int dimensions() {
        return dimensions;
    }

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

    protected abstract String logType();

    @Override
    public void prepare(Context context, Continuation<Void> continuation) throws Throwable {
        continuation.completed(null);
    }

    @Override
    public Reader reader() {
        return new Reader() {
            private int[] buffer=new int[dimensions*width];

            @Override
            public void setNormalizedLineTo(int yy, MutablePoints points, int offset) throws Throwable {
                ImageReadParam readParam=new ImageReadParam();
                readParam.setSourceRegion(new Rectangle(0, yy, width, 1));
                BufferedImage lineImage;
                synchronized (lock) {
                    lineImage=imageReader.read(0, readParam);
                }
                Raster raster=lineImage.getRaster();
                buffer=raster.getPixels(0, 0, width, 1, buffer);
                for (int ii=0, xx=0; width>xx; ++xx, ++offset) {
                    for (int dd=0; dimensions>dd; ++dd, ++ii) {
                        setNormalized(points, dd, offset, buffer[ii]);
                    }
                }
            }
        };
    }

    protected abstract void setNormalized(MutablePoints points, int dimension, int index, int value);

    @Override
    public int width() {
        return width;
    }
}
