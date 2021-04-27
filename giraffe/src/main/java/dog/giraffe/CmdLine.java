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
import dog.giraffe.image.transform.Select;
import dog.giraffe.image.transform.NormalizedDifferenceVegetationIndex;
import dog.giraffe.points.Points;
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

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.MissingParameterException;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

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


    private static class Config {
        @Option(names = { "-i", "--input" }, required = true, paramLabel = "IMGPATH", description = "Input image path")
        Path inputPath;

        @Option(names = { "-o", "--output" }, required = true, paramLabel = "IMGPATH", description = "Output image path")
        Path outputPath;

        @Option(names = { "-a", "--algorithm" }, paramLabel = "CLUSTER", description = "Specify the clustering algorithm (default: kMeans with elbow)")
        String clusteringAlgorithm = "elbow";

        @Option(names = { "--min" }, paramLabel = "CLUSTERNUMBER", description = "Minimum number of clusters")
        int minCluster = 2;

        @Option(names = { "--max" }, paramLabel = "CLUSTERNUMBER", description = "Maximum number of clusters")
        int maxCluster = 20;

        @Option(names = { "-h", "--help" }, usageHelp = true, description = "display a help message")
        boolean helpRequested = false;
    }

    private static <P extends Points> ClusteringStrategy<P>  createClustering(Config config) {
       if (config.clusteringAlgorithm.equals("otsu")) {
          return ClusteringStrategy.otsu(32, config.maxCluster);
       }
       if (config.clusteringAlgorithm.equals("isodata")) {
          return ClusteringStrategy.isodata(config.minCluster, config.maxCluster);
       }

       if (config.clusteringAlgorithm.equals("kMeans")) {
          return ClusteringStrategy.kMeans(
                        config.maxCluster,
                        0.95,
                        InitialCenters.meanAndFarthest(false),
                        10000,
                        ReplaceEmptyCluster.farthest(false));
       }

       return ClusteringStrategy.elbow(
                      0.95,
                      config.maxCluster,
                      config.minCluster,
                      (clusters)->ClusteringStrategy.kMeans(
                              clusters,
                              0.95,
                              InitialCenters.meanAndFarthest(false),
                              10000,
                              ReplaceEmptyCluster.farthest(false)),
                      1);
    }

    private static Function<Image, Image> createImageMap(Config config, Mask mask) {
        return (image)->
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
                        createClustering(config)
                );
    }


    public static void main(String[] args) throws Throwable {
       
        Config config = new Config();
        CommandLine cmd = new CommandLine(config);
        try {
        cmd.parseArgs(args);
        }
        catch (MissingParameterException e) {
           config.helpRequested = true;
        }

        if (config.helpRequested) {
           cmd.usage(System.out);
           return;
        }


        boolean bufferedInput=true;
        boolean bufferedOutput=true;
        //Mask mask=Mask.all();
        Mask mask=Mask.and(
                Mask.halfPlane(1851, 4, 14, 6256),
                Mask.halfPlane(14, 6256, 6119, 8066),
                Mask.halfPlane(6119, 8066, 7964,1786),
                Mask.halfPlane(7964,1786, 1851, 4));

        String outputFormat="tiff";

        Files.deleteIfExists(config.outputPath);

        try (Context context=new StandardContext()) {
            AsyncJoin<Void> join=new AsyncJoin<>();
            write(
                    context,
                    createImageMap(config, mask),
                    bufferedInput
                            ?BufferedImageReader.factory(config.inputPath)
                            :FileImageReader.factory(config.inputPath),
                    bufferedOutput
                            ?BufferedImageWriter.factory(outputFormat, config.outputPath)
                            :FileImageWriter.factory(outputFormat, config.outputPath),
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
