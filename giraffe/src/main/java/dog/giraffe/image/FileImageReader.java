package dog.giraffe.image;

import dog.giraffe.Context;
import dog.giraffe.points.MutablePoints;
import dog.giraffe.points.UnsignedByteArrayPoints;
import dog.giraffe.points.UnsignedShortArrayPoints;
import dog.giraffe.threads.AsyncSupplier;
import dog.giraffe.threads.Continuation;
import dog.giraffe.threads.Continuations;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.nio.file.Path;
import java.util.Objects;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;

public abstract class FileImageReader implements ImageReader {
    public static class Factory implements ImageReader.Factory {
        private final Path path;

        public Factory(Path path) {
            this.path=path;
        }

        @Override
        public <T> void run(
                Context context, ReadProcess<T> readProcess, Continuation<T> continuation) throws Throwable {
            Continuation<T> continuation2;
            AsyncSupplier<T> read;
            boolean error=true;
            ImageInputStream iis=new FileImageInputStream(path.toFile());
            try {
                javax.imageio.ImageReader ir
                        =Objects.requireNonNull(ImageIO.getImageReaders(iis).next(), "imageReader");
                try {
                    ir.setInput(iis);
                    switch (ir.getRawImageType(0).getSampleModel().getDataType()) {
                        case DataBuffer.TYPE_BYTE:
                            UnsignedByte unsignedByteImageReader=new UnsignedByte(ir);
                            read=(continuation3)->readProcess.run(context, unsignedByteImageReader, continuation3);
                            break;
                        case DataBuffer.TYPE_USHORT:
                            UnsignedShort unsignedShortImageReader=new UnsignedShort(ir);
                            read=(continuation3)->readProcess.run(context, unsignedShortImageReader, continuation3);
                            break;
                        default:
                            throw new RuntimeException(
                                    "unsupported sample model "+ir.getRawImageType(0).getSampleModel());
                    }
                    continuation2=Continuations.finallyBlock(
                            ()->{
                                try {
                                    ir.dispose();
                                }
                                finally {
                                    iis.close();
                                }
                            },
                            continuation);
                    error=false;
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
            try {
                read.get(continuation2);
            }
            catch (Throwable throwable) {
                continuation2.failed(throwable);
            }
        }
    }

    private static class UnsignedByte extends FileImageReader {
        public UnsignedByte(javax.imageio.ImageReader imageReader) throws Throwable {
            super(imageReader);
        }

        @Override
        public UnsignedByteArrayPoints createPoints(int dimensions, int expectedSize) {
            return new UnsignedByteArrayPoints(dimensions, expectedSize);
        }

        @Override
        protected void set(MutablePoints points, int dimension, int index, int value) {
            points.set(dimension, index, (byte)(value&0xff));
        }
    }

    private static class UnsignedShort extends FileImageReader {
        public UnsignedShort(javax.imageio.ImageReader imageReader) throws Throwable {
            super(imageReader);
        }

        @Override
        public UnsignedShortArrayPoints createPoints(int dimensions, int expectedSize) {
            return new UnsignedShortArrayPoints(dimensions, expectedSize);
        }

        @Override
        protected void set(MutablePoints points, int dimension, int index, int value) {
            points.set(dimension, index, (short)(value&0xffff));
        }
    }

    protected final int dimensions;
    protected final int height;
    protected final javax.imageio.ImageReader imageReader;
    protected final Object lock=new Object();
    protected final int width;

    public FileImageReader(javax.imageio.ImageReader imageReader) throws Throwable {
        this.imageReader=imageReader;
        dimensions=imageReader.getRawImageType(0).getNumBands();
        height=imageReader.getHeight(0);
        width=imageReader.getWidth(0);
    }

    @Override
    public void addLineTo(int yy, MutablePoints points) throws Throwable {
        int size=points.size();
        ImageReadParam readParam=new ImageReadParam();
        readParam.setSourceRegion(new Rectangle(0, yy, width, 1));
        BufferedImage lineImage;
        synchronized (lock) {
            lineImage=imageReader.read(0, readParam);
        }
        points.clear(size+width);
        int[] buffer=new int[width];
        for (int dd=0; dimensions>dd; ++dd) {
            buffer=lineImage.getRaster().getSamples(0, 0, width, 1, dd, buffer);
            for (int xx=0; width>xx; ++xx) {
                set(points, dd, size+xx, buffer[xx]);
            }
        }
    }

    @Override
    public int dimensions() {
        return dimensions;
    }

    @Override
    public int height() {
        return height;
    }

    protected abstract void set(MutablePoints points, int dimension, int index, int value);

    @Override
    public int width() {
        return width;
    }
}
