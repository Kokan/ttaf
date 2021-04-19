package dog.giraffe.image;

import dog.giraffe.Context;
import dog.giraffe.points.MutablePoints;
import dog.giraffe.points.UnsignedByteArrayPoints;
import dog.giraffe.points.UnsignedShortArrayPoints;
import dog.giraffe.threads.AsyncSupplier;
import dog.giraffe.threads.Continuation;
import dog.giraffe.threads.Continuations;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.nio.file.Path;
import javax.imageio.ImageIO;

public abstract class BufferedImageReader<P extends MutablePoints> implements ImageReader<P> {
    public static class Factory implements ImageReader.Factory {
        private final AsyncSupplier<BufferedImage> supplier;

        public Factory(AsyncSupplier<BufferedImage> supplier) {
            this.supplier=supplier;
        }

        public Factory(Path path) {
            this((continuation)->continuation.completed(ImageIO.read(path.toFile())));
        }

        @Override
        public <T> void run(
                Context context, ReadProcess<T> readProcess, Continuation<T> continuation) throws Throwable {
            supplier.get(Continuations.map(
                    (image, continuation2)->{
                        switch (image.getSampleModel().getDataType()) {
                            case DataBuffer.TYPE_BYTE:
                                runUnsignedByte(context, image, readProcess, continuation2);
                                break;
                            case DataBuffer.TYPE_USHORT:
                                runUnsignedShort(context, image, readProcess, continuation2);
                                break;
                            default:
                                throw new RuntimeException("unsupported sample model "+image.getSampleModel());
                        }
                    },
                    continuation));
        }

        public <T> void runUnsignedByte(
                Context context, BufferedImage image, ReadProcess<T> readProcess, Continuation<T> continuation)
                throws Throwable {
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
            readProcess.run(
                    context,
                    new BufferedImageReader<>(height, new UnsignedByteArrayPoints(data, dimensions), width) {
                        @Override
                        public UnsignedByteArrayPoints createPoints(int dimensions, int expectedSize) {
                            return new UnsignedByteArrayPoints(dimensions, expectedSize);
                        }
                    },
                    continuation);
        }

        public <T> void runUnsignedShort(
                Context context, BufferedImage image, ReadProcess<T> readProcess, Continuation<T> continuation)
                throws Throwable {
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
            readProcess.run(
                    context,
                    new BufferedImageReader<>(height, new UnsignedShortArrayPoints(data, dimensions), width) {
                        @Override
                        public UnsignedShortArrayPoints createPoints(int dimensions, int expectedSize) {
                            return new UnsignedShortArrayPoints(dimensions, expectedSize);
                        }
                    },
                    continuation);
        }
    }

    private final int height;
    private final P points;
    private final int width;

    private BufferedImageReader(int height, P points, int width) {
        this.height=height;
        this.points=points;
        this.width=width;
    }

    @Override
    public void addLineTo(int yy, P points) {
        this.points.addTo(points, yy*width, (yy+1)*width);
    }

    @Override
    public int dimensions() {
        return points.dimensions();
    }

    @Override
    public int height() {
        return height;
    }

    @Override
    public int width() {
        return width;
    }
}
