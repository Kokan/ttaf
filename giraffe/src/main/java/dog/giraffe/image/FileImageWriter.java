package dog.giraffe.image;

import dog.giraffe.points.UnsignedByteArrayPoints;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;

public class FileImageWriter implements ImageWriter {
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
            data[dimensions*xx+dimension]=UnsignedByteArrayPoints.denormalize(value)&0xff;
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
    private final ImageOutputStream imageOutputStream;
    private final javax.imageio.ImageWriter imageWriter;
    protected final Object lock=new Object();
    private final Path path;
    private final int width;

    public FileImageWriter(
            int dimensions, ImageOutputStream imageOutputStream, javax.imageio.ImageWriter imageWriter,
            Path path, int width) {
        this.dimensions=dimensions;
        this.imageOutputStream=imageOutputStream;
        this.imageWriter=imageWriter;
        this.path=path;
        this.width=width;
    }

    @Override
    public void close() throws IOException {
        try {
            try {
                imageWriter.endReplacePixels();
            }
            finally {
                imageWriter.dispose();
            }
        }
        finally {
            imageOutputStream.close();
        }
    }

    public static FileImageWriter create(
            int width, int height, int dimensions, String format, Path path) throws Throwable {
        boolean error=true;
        ImageOutputStream ios=new FileImageOutputStream(path.toFile());
        try {
            javax.imageio.ImageWriter iw=Objects.requireNonNull(
                    ImageIO.getImageWritersByFormatName(format).next(), "imageWriter");
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
                FileImageWriter result=new FileImageWriter(dimensions, ios, iw, path, width);
                error=false;
                return result;
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
    }

    public static Factory factory(String format, Path path) {
        return (width, height, dimension)->create(width, height, dimension, format, path);
    }

    @Override
    public Line getLine(int yy) {
        return new LineImpl(yy);
    }

    @Override
    public void log(Map<String, Object> log) {
        log.put("buffered", false);
        log.put("file", path);
    }
}
