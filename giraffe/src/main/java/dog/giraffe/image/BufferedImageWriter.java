package dog.giraffe.image;

import dog.giraffe.Context;
import dog.giraffe.Pair;
import dog.giraffe.points.UnsignedByteArrayL2Points;
import dog.giraffe.threads.Continuation;
import dog.giraffe.threads.Continuations;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import javax.imageio.ImageIO;

public class BufferedImageWriter implements ImageWriter {
    public static class FileFactory implements ImageWriter.Factory<Void> {
        private final String format;
        private final Path path;

        public FileFactory(String format, Path path) {
            this.format=format;
            this.path=path;
        }

        @Override
        public <U> void run(
                Context context, int width, int height, int dimensions, WriteProcess<U> writeProcess,
                Continuation<Pair<Void, U>> continuation) throws Throwable {
            BufferedImageWriter imageWriter=new BufferedImageWriter(dimensions, height, width);
            writeProcess.run(
                    context,
                    imageWriter,
                    Continuations.map(
                            (result, continuation2)->{
                                if (!ImageIO.write(imageWriter.createImage(), format, path.toFile())) {
                                    throw new RuntimeException("no image writer for "+format);
                                }
                                continuation2.completed(new Pair<>(null, result));
                            },
                            continuation));
        }
    }

    private class LineImpl implements Line {
        private final int yy;

        public LineImpl(int yy) {
            this.yy=yy;
        }

        @Override
        public void set(int dimension, int xx, double value) {
            data[dimensions*(yy*width+xx)+dimension]=(byte)value;
        }

        @Override
        public void setNormalized(int dimension, int xx, double value) {
            data[dimensions*(yy*width+xx)+dimension]=UnsignedByteArrayL2Points.denormalize(value);
        }

        @Override
        public void write() {
        }
    }

    public static class MemoryFactory implements ImageWriter.Factory<BufferedImage> {
        @Override
        public <U> void run(
                Context context, int width, int height, int dimensions, WriteProcess<U> writeProcess,
                Continuation<Pair<BufferedImage, U>> continuation) throws Throwable {
            BufferedImageWriter imageWriter=new BufferedImageWriter(dimensions, height, width);
            writeProcess.run(
                    context,
                    imageWriter,
                    Continuations.map(
                            (result, continuation2)
                                    ->continuation2.completed(new Pair<>(imageWriter.createImage(), result)),
                            continuation));
        }
    }

    private final byte[] data;
    private final int dimensions;
    private final int height;
    private final int width;

    private BufferedImageWriter(int dimensions, int height, int width) {
        this.dimensions=dimensions;
        this.height=height;
        this.width=width;
        data=new byte[dimensions*height*width];
    }

    private BufferedImage createImage() {
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
}
