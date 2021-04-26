package dog.giraffe;

import dog.giraffe.image.BufferedImageReader;
import dog.giraffe.image.BufferedImageWriter;
import dog.giraffe.image.FileImageReader;
import dog.giraffe.image.FileImageWriter;
import dog.giraffe.image.Image;
import dog.giraffe.image.ImageReader;
import dog.giraffe.image.ImageWriter;
import dog.giraffe.image.PrepareImages;
import dog.giraffe.image.transform.Cluster1;
import dog.giraffe.image.transform.Intensity;
import dog.giraffe.image.transform.Mask;
import dog.giraffe.points.MutablePoints;
import dog.giraffe.threads.Continuation;
import dog.giraffe.threads.Continuations;
import dog.giraffe.threads.Function;
import dog.giraffe.threads.Supplier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class CmdLine {
    private static class AsyncJoin<T> implements Continuation<T> {
        private Throwable error;
        private boolean hasError;
        private boolean hasResult;
        private final Object lock=new Object();
        private T result;

        @Override
        public void completed(T result) throws Throwable {
            synchronized (lock) {
                if (hasError || hasResult) {
                    throw new RuntimeException("already completed");
                }
                hasResult=true;
                this.result=result;
                lock.notifyAll();
            }
        }

        @Override
        public void failed(Throwable throwable) throws Throwable {
            synchronized (lock) {
                if (hasError || hasResult) {
                    throw new RuntimeException("already completed");
                }
                error=throwable;
                hasError=true;
                lock.notifyAll();
            }
        }

        public T join() throws Throwable {
            synchronized (lock) {
                while (true) {
                    if (hasError) {
                        throw new RuntimeException(error);
                    }
                    if (hasResult) {
                        return result;
                    }
                    lock.wait();
                }
            }
        }
    }

    public static void main(String[] args) throws Throwable {
        boolean bufferedInput=true;
        boolean bufferedOutput=true;
        //Mask mask=Mask.all();
        Mask mask=Mask.and(
                Mask.halfPlane(1851, 4, 14, 6256),
                Mask.halfPlane(14, 6256, 6119, 8066),
                Mask.halfPlane(6119, 8066, 7964,1786),
                Mask.halfPlane(7964,1786, 1851, 4));
        Path inputPath=Paths.get("../../ttaf2/LC08_L1TP_188027_20200420_20200508_01_T1.tif");
        //Path inputPath=Paths.get("../P.t.altaica_Tomak_Male.jpg");
        //Path inputPath=Paths.get("../../ttaf2/a/misc/4.2.06.tiff");
        String outputFormat="tiff";
        Path outputPath=Paths.get("../../ttaf2/plap.tif");
        Files.deleteIfExists(outputPath);
        Function<Image, Image> imageMap=(image)->
                Cluster1.create(
                        //Select.create(image, 1, 2, 3),
                        /*Normalize.createDeviation(
                                Select.create(image, 1, 2, 3),
                                mask,
                                3.0),*/
                        /*HyperHue.create(
                                Normalize.createDeviation(
                                        Select.create(image, 1, 2, 3),
                                        mask,
                                        3.0)),*/
                        /*NormalizedDifferenceVegetationIndex.create(
                                Select.create(image, 4, 3)),*/
                        //Hue.create(image),
                        //HyperHue.create(image),
                        //HyperHue.create(Select.create(image, 1, 2, 3)),
                        //NormalizedHyperHue.create(image, 0.01),
                        //NormalizedHyperHue.create(Select.create(image, 3, 4), 0.01),
                        Intensity.create(image),
                        //ClusterColors.Gray.falseColor(1),
                        ClusterColors.RGB.falseColor(0, 1, 2),
                        mask,
                        ClusteringStrategy.elbow(
                                0.95,
                                20,
                                2,
                                (clusters)->ClusteringStrategy.kMeans(
                                        clusters,
                                        0.95,
                                        InitialCenters.meanAndFarthest(false),
                                        10000,
                                        ReplaceEmptyCluster.farthest(false)),
                                //(clusters)->ClusteringStrategy.otsu(32, clusters),
                                1));
        try (Context context=new StandardContext()) {
            AsyncJoin<Void> join=new AsyncJoin<>();
            write(
                    context,
                    imageMap,
                    bufferedInput
                            ?BufferedImageReader.factory(inputPath)
                            :FileImageReader.factory(inputPath),
                    bufferedOutput
                            ?BufferedImageWriter.factory(outputFormat, outputPath)
                            :FileImageWriter.factory(outputFormat, outputPath),
                    join);
            join.join();
        }
    }

    private static void write(
            Context context, Function<Image, Image> imageMap, Supplier<ImageReader> imageReader,
            ImageWriter.Factory imageWriter, Continuation<Void> continuation) throws Throwable {
        ImageReader imageReader2=imageReader.get();
        Continuation<Void> continuation2=Continuations.finallyBlock(imageReader2::close, continuation);
        try {
            write(context, imageMap, imageReader2, imageWriter, continuation2);
        }
        catch (Throwable throwable) {
            continuation2.failed(throwable);
        }
    }

    private static void write(
            Context context, Function<Image, Image> imageMap, ImageReader imageReader,
            ImageWriter.Factory imageWriter, Continuation<Void> continuation) throws Throwable {
        Image image=imageMap.apply(imageReader);
        PrepareImages.prepareImages(
                context,
                List.of(image),
                Continuations.map(
                        (input, continuation2)->write(context, image, imageWriter, continuation2),
                        continuation));
    }

    private static void write(
            Context context, Image image, ImageWriter.Factory imageWriter, Continuation<Void> continuation)
            throws Throwable {
        ImageWriter imageWriter2=imageWriter.create(image.width(), image.height(), image.dimensions());
        Continuation<Void> continuation2=Continuations.finallyBlock(imageWriter2::close, continuation);
        try {
            write(context, image, imageWriter2, continuation2);
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
