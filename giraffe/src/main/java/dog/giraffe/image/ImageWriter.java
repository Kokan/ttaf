package dog.giraffe.image;

import dog.giraffe.Context;
import dog.giraffe.Log;
import dog.giraffe.points.MutablePoints;
import dog.giraffe.threads.Continuation;
import dog.giraffe.threads.Continuations;
import dog.giraffe.util.Consumer;
import dog.giraffe.util.Function;
import dog.giraffe.util.Supplier;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Class to create images as {@link java.awt.image.BufferedImage BufferedImages} and to write to disk.
 */
public interface ImageWriter extends AutoCloseable, Log {
    /**
     * Factory for ImageWriters.
     */
    interface Factory {
        /**
         * Creates a new {@link ImageWriter} for an image with the specified width, height, and dimensions.
         */
        ImageWriter create(int width, int height, int dimension) throws Throwable;
    }

    /**
     * Contains all the information to write one line of the image.
     */
    interface Line {
        /**
         * Sets the component dimension of the pixel xx to value, possibly buffering it.
         */
        void setNormalized(int dimension, int xx, double value);

        /**
         * Finalizes the line and ensures it's written the backing store.
         */
        void write() throws Throwable;
    }

    @Override
    void close() throws IOException;

    /**
     * Creates an object which can be used to write a line component-wise.
     *
     * @param yy the index if the line to be written
     */
    Line getLine(int yy);

    /**
     * Guesses the java format name of a file by the extension of the filename.
     */
    static String outputFormat(String outputFormat, Path outputPath) {
        if (null!=outputFormat) {
            return outputFormat;
        }
        String filename=outputPath.getFileName().toString();
        int ii=filename.lastIndexOf('.');
        if (0<=ii) {
            switch (filename.substring(ii+1).toLowerCase()) {
                case "bmp":
                    return "bmp";
                case "gif":
                    return "gif";
                case "jpeg":
                case "jpg":
                    return "jpeg";
                case "png":
                    return "png";
                case "tif":
                case "tiff":
                    return "tiff";
            }
        }
        return "tiff";
    }

    /**
     * Generates an image and writes it.
     *
     * @param imageMap the transformation to be applied to the input image
     * @param imageReader the input image
     * @param imageWriter the place to write the result
     * @param logger store for the metadata
     */
    static void write(
            Context context, Function<Image, Image> imageMap, Supplier<ImageReader> imageReader,
            ImageWriter.Factory imageWriter, Consumer<Map<String, Object>> logger, Continuation<Void> continuation)
            throws Throwable {
        Instant start=Instant.now();
        ImageReader imageReader2=imageReader.get();
        Continuation<Void> continuation2=Continuations.finallyBlock(imageReader2::close, continuation);
        try {
            write(context, imageMap, imageReader2, imageWriter, logger, start, continuation2);
        }
        catch (Throwable throwable) {
            continuation2.failed(throwable);
        }
    }

    private static void write(
            Context context, Function<Image, Image> imageMap, ImageReader imageReader,
            ImageWriter.Factory imageWriter, Consumer<Map<String, Object>> logger, Instant start,
            Continuation<Void> continuation) throws Throwable {
        Image image=imageMap.apply(imageReader);
        PrepareImages.prepareImages(
                context,
                List.of(image),
                Continuations.map(
                        (input, continuation2)->write(context, image, imageWriter, logger, start, continuation2),
                        continuation));
    }

    private static void write(
            Context context, Image image, ImageWriter.Factory imageWriter, Consumer<Map<String, Object>> logger,
            Instant start, Continuation<Void> continuation) throws Throwable {
        ImageWriter imageWriter2=imageWriter.create(image.width(), image.height(), image.dimensions());
        Continuation<Void> continuation2=Continuations.finallyBlock(imageWriter2::close, continuation);
        try {
            ImageWriter.write(
                    context,
                    image,
                    imageWriter2,
                    Continuations.map(
                            (result, continuation3)->{
                                if (null!=logger) {
                                    Instant end=Instant.now();
                                    Map<String, Object> log=new TreeMap<>();
                                    Log.logElapsedTime(start, end, log);
                                    Log.logImages(image, log);
                                    Log.logWriter(imageWriter2, log);
                                    logger.accept(log);
                                }
                                continuation3.completed(result);
                            },
                            continuation2));
        }
        catch (Throwable throwable) {
            continuation2.failed(throwable);
        }
    }

    private static void write(
            Context context, Image image, ImageWriter imageWriter, Continuation<Void> continuation) throws Throwable {
        Continuations.forkJoin(
                (from, to)->(continuation2)->{
                    Image.Reader reader=image.reader();
                    MutablePoints points=image.createPoints(image.dimensions(), image.width());
                    for (int yy=from; to>yy; ++yy) {
                        reader.setNormalizedLineTo(yy, points, 0);
                        ImageWriter.Line line=imageWriter.getLine(yy);
                        for (int xx=0; image.width()>xx; ++xx) {
                            for (int dd=0; image.dimensions()>dd; ++dd) {
                                line.setNormalized(dd, xx, points.getNormalized(dd, xx));
                            }
                        }
                        line.write();
                    }
                    continuation2.completed(null);
                },
                0,
                image.height(),
                Continuations.map(
                        (input, continuation2)->continuation2.completed(null),
                        continuation),
                context.executor());
    }
}
