package dog.giraffe;

import dog.giraffe.image.BufferedImageReader;
import dog.giraffe.image.BufferedImageWriter;
import dog.giraffe.image.Cluster1Transform;
import dog.giraffe.image.FileImageReader;
import dog.giraffe.image.FileImageWriter;
import dog.giraffe.image.ImageTransform;
import dog.giraffe.image.Projection1;
import dog.giraffe.points.KDTree;
import dog.giraffe.points.MutablePoints;
import dog.giraffe.threads.Continuation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

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
        Path inputPath=Paths.get("/stuff/unistuff/ttaf2/LC08_L1TP_188027_20200420_20200508_01_T1.tif");
        //Path inputPath=Paths.get("/stuff/unistuff/ttaf/P.t.altaica_Tomak_Male.jpg");
        //Path inputPath=Paths.get("/stuff/unistuff/ttaf2/a/misc/4.2.06.tiff");
        String outputFormat="tiff";
        Path outputPath=Paths.get("/stuff/unistuff/ttaf2/plap.tif");
        Files.deleteIfExists(outputPath);
        try (Context context=new StandardContext()) {
            AsyncJoin<Void> join=new AsyncJoin<>();
            ImageTransform.run(
                    context,
                    bufferedInput
                            ?new BufferedImageReader.Factory(inputPath)
                            :new FileImageReader.Factory(inputPath),
                    bufferedOutput
                            ?new BufferedImageWriter.FileFactory(outputFormat, outputPath)
                            :new FileImageWriter.Factory(outputFormat, outputPath),
                    Arrays.asList(
                            new Cluster1Transform(
                                    //ClusterColors.Gray.falseColor(1),
                                    ClusterColors.RGB.falseColor(0, 1, 2),
                                    //Projection1.multidimensionalHue(0, 1, 2),
                                    //Projection1.multidimensionalHue(1, 2, 3),
                                    //Projection1.multidimensionalHue(3, 4),
                                    //Projection1.multidimensionalHueNormalized(0.001, 0, 1, 2),
                                    Projection1.multidimensionalHueNormalized(0.001, 1, 2, 3),
                                    //Projection1.multidimensionalHueNormalized(0.001, 3, 4),
                                    //Projection1.multidimensionalHueNormalized(0.001, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10),
                                    //Projection1.select(0, 1, 2),
                                    //Projection1.select(1, 2, 3),
                                    //Projection1.select(3, 4),
                                    //Projection1.select(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10),
                                    new Cluster1Transform.Strategy() {
                                        @Override
                                        public <P extends MutablePoints> void cluster(
                                                Context context, KDTree points, Continuation<Clusters> continuation)
                                                throws Throwable {
                                            double errorLimit=0.95;
                                            ClusteringStrategy.<KDTree>elbow(
                                                    errorLimit,
                                                    10,
                                                    2,
                                                    (clusters)->ClusteringStrategy.kMeans(
                                                            clusters,
                                                            errorLimit,
                                                            //KDTree.initialCenters(false),
                                                            InitialCenters.meanAndFarthest(false),
                                                            1000,
                                                            ReplaceEmptyCluster.farthest(false)),
                                                    1)
                                                    .cluster(context, points, continuation);
                                        }
                                    }),
                            //PixelTransform.select(4, 3),
                            //PixelTransform.constNormalizedOutput(1, 0.0),
                            //PixelTransform.intensity(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10),
                            //PixelTransform.normalizedDifferenceVegetationIndex(4, 3),
                            //PixelTransform.intensity(0),
                            //PixelTransform.normalizedDifferenceVegetationIndex(1, 2),
                            ImageTransform.noop()),
                    join);
            join.join();
        }
    }
}
