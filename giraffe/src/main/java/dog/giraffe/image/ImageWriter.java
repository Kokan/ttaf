package dog.giraffe.image;

import dog.giraffe.Context;
import dog.giraffe.Log;
import dog.giraffe.points.MutablePoints;
import dog.giraffe.threads.Consumer;
import dog.giraffe.threads.Continuation;
import dog.giraffe.threads.Continuations;
import dog.giraffe.threads.Function;
import dog.giraffe.threads.Supplier;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public interface ImageWriter extends AutoCloseable, Log {
    interface Factory {
        ImageWriter create(int width, int height, int dimension) throws Throwable;
    }

    interface Line {
        void setNormalized(int dimension, int xx, double value);

        void write() throws Throwable;
    }

    void close() throws IOException;

    Line getLine(int yy);

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
