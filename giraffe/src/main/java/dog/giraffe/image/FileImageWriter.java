package dog.giraffe.image;

import dog.giraffe.Context;
import dog.giraffe.Pair;
import dog.giraffe.points.UnsignedByteArrayL2Points;
import dog.giraffe.threads.Continuation;
import dog.giraffe.threads.Continuations;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;

public class FileImageWriter implements ImageWriter {
    public static class Factory implements ImageWriter.Factory<Void> {
        private final String format;
        private final Path path;

        public Factory(String format, Path path) {
            this.format=format;
            this.path=path;
        }

        @Override
        public <U> void run(
                Context context, int width, int height, int dimensions, WriteProcess<U> writeProcess,
                Continuation<Pair<Void, U>> continuation) throws Throwable {
            Continuation<U> continuation2;
            ImageWriter imageWriter;
            boolean error=true;
            ImageOutputStream ios=new FileImageOutputStream(path.toFile());
            try {
                javax.imageio.ImageWriter iw=Objects.requireNonNull(ImageIO.getImageWritersByFormatName(format).next(), "imageWriter");
                try {
                    iw.setOutput(ios);
                    iw.prepareWriteEmpty(
                            null,
                            ImageTypeSpecifier.createInterleaved(
                                    Images.createColorSpace(dimensions),
                                    Images.createBandOffsets(dimensions),
                                    DataBuffer.TYPE_BYTE,
                                    false,
                                    false),
                            width,
                            height,
                            null,
                            null,
                            null);
                    iw.endWriteEmpty();
                    iw.prepareReplacePixels(0, new Rectangle(0, 0, width, height));
                    imageWriter=new FileImageWriter(dimensions, height, iw, width);
                    continuation2=Continuations.finallyBlock(
                            ()->{
                                try {
                                    try {
                                        iw.endReplacePixels();
                                    }
                                    finally {
                                        iw.dispose();
                                    }
                                }
                                finally {
                                    ios.close();
                                }
                            },
                            Continuations.map(
                                    (result, continuation3)->continuation3.completed(new Pair<>(null, result)),
                                    continuation));
                    error=false;
                }
                finally {
                    if (error) {
                        iw.dispose();
                    }
                }
            }
            finally {
                if (error) {
                    ios.close();
                }
            }
            try {
                writeProcess.run(context, imageWriter, continuation2);
            }
            catch (Throwable throwable) {
                continuation2.failed(throwable);
            }
        }
    }

    private class LineImpl implements Line {
        private final int[] data;
        private final BufferedImage image;
        private final int yy;

        public LineImpl(int yy) {
            this.yy=yy;
            data=new int[dimensions*width];
            image=Images.createUnsignedByte(width, 1, dimensions);
        }

        @Override
        public void set(int dimension, int xx, double value) {
            data[dimensions*xx+dimension]=((byte)value)&0xff;
        }

        @Override
        public void setNormalized(int dimension, int xx, double value) {
            data[dimensions*xx+dimension]=UnsignedByteArrayL2Points.denormalize(value)&0xff;
        }

        @Override
        public void write() throws Throwable {
            image.getRaster().setPixels(0, 0, width, 1, data);
            ImageWriteParam writeParam=new ImageWriteParam(Locale.US);
            writeParam.setDestinationOffset(new Point(0, yy));
            synchronized (lock) {
                imageWriter.replacePixels(image, writeParam);
            }
        }
    }

    private final int dimensions;
    private final int height;
    private final javax.imageio.ImageWriter imageWriter;
    protected final Object lock=new Object();
    private final int width;

    public FileImageWriter(int dimensions, int height, javax.imageio.ImageWriter imageWriter, int width) {
        this.dimensions=dimensions;
        this.height=height;
        this.imageWriter=imageWriter;
        this.width=width;
    }

    @Override
    public Line getLine(int yy) {
        return new LineImpl(yy);
    }
}
