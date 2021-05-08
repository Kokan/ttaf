package dog.giraffe.image.transform;

import dog.giraffe.Context;
import dog.giraffe.Log;
import dog.giraffe.cluster.ClusterColors;
import dog.giraffe.cluster.ClusteringStrategy;
import dog.giraffe.cluster.Clusters;
import dog.giraffe.image.Image;
import dog.giraffe.points.FloatArrayPoints;
import dog.giraffe.points.KDTree;
import dog.giraffe.points.MutablePoints;
import dog.giraffe.points.Sum;
import dog.giraffe.points.Vector;
import dog.giraffe.threads.AsyncFunction;
import dog.giraffe.threads.AsyncSupplier;
import dog.giraffe.threads.Continuation;
import dog.giraffe.threads.Continuations;
import dog.giraffe.util.ColorConverter;
import dog.giraffe.util.Doubles;
import dog.giraffe.util.Function;
import dog.giraffe.util.Lists;
import dog.giraffe.util.Pair;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Clusters the pixel of an image and replaces every pixel with a color assigned to pixel's nearest center.
 * This transform treats a pixel differently whether it is gray-like or colorful.
 * The two class of pixels are clustered separately and assigned different colors.
 * Centers of grayish pixel will be assigned gray colors.
 * Centers of colorful pixels will be assigned fully saturated colors.
 */
public abstract class Cluster2 extends Image.Transform {
    private static class Data {
        private List<Vector> centers;
        private Clusters clusters;
        private Map<Vector, Vector> colorMap;
        private final ClusterColors colors;

        public Data(ClusterColors colors) {
            this.colors=colors;
        }
    }

    private static class Hue extends Cluster2 {
        public Hue(Image image, Mask mask, ClusteringStrategy<? super KDTree> strategy) {
            super(image, mask, strategy);
        }

        @Override
        protected void checkImageDimensions(int dimensions) {
            if (3>dimensions) {
                throw new RuntimeException(String.format(
                        "not enough dimensions. image: %1$d, selected: 3",
                        dimensions));
            }
        }

        @Override
        protected MutablePoints createPoints1(int expectedSize) {
            return new FloatArrayPoints(1, expectedSize);
        }

        @Override
        protected MutablePoints createPoints2(int dimensions, int expectedSize) {
            return new FloatArrayPoints(dimensions, expectedSize);
        }

        @Override
        protected int dimensions2() {
            return 1;
        }

        @Override
        protected String logType() {
            return "hue";
        }

        @Override
        protected Projection projection() {
            return new Projection() {
                private final ColorConverter colorConverter=new ColorConverter();

                @Override
                public boolean project(
                        MutablePoints input, int inputOffset, MutablePoints output1, int outputOffset1,
                        MutablePoints output2, int outputOffset2) {
                    colorConverter.rgbToHsvAndHsl(
                            input.getNormalized(0, inputOffset),
                            input.getNormalized(1, inputOffset),
                            input.getNormalized(2, inputOffset));
                    if (1.0-0.8*colorConverter.value>=colorConverter.saturationValue) {
                        output1.setNormalized(0, outputOffset1, colorConverter.value);
                        return true;
                    }
                    else {
                        output2.setNormalized(0, outputOffset2, colorConverter.hue/(2.0*Math.PI));
                        return false;
                    }
                }
            };
        }
    }

    private static class HyperHue extends Cluster2 {
        public HyperHue(Image image, Mask mask, ClusteringStrategy<? super KDTree> strategy) {
            super(image, mask, strategy);
        }

        @Override
        protected void checkImageDimensions(int dimensions) {
            if (2>dimensions) {
                throw new RuntimeException(String.format(
                        "not enough dimensions. image: %1$d, selected: 2",
                        dimensions));
            }
        }

        @Override
        protected MutablePoints createPoints1(int expectedSize) {
            return new FloatArrayPoints(1, expectedSize);
        }

        @Override
        protected MutablePoints createPoints2(int dimensions, int expectedSize) {
            return new FloatArrayPoints(dimensions, expectedSize);
        }

        @Override
        protected int dimensions2() {
            return image.dimensions();
        }

        @Override
        protected String logType() {
            return "hyper-hue";
        }

        @Override
        protected Projection projection() {
            return new Projection() {
                private final double[] hue=new double[image.dimensions()];
                private final double[] point=new double[image.dimensions()];

                @Override
                public boolean project(
                        MutablePoints input, int inputOffset, MutablePoints output1, int outputOffset1,
                        MutablePoints output2, int outputOffset2) {
                    for (int dd=0; point.length>dd; ++dd) {
                        point[dd]=input.getNormalized(dd, inputOffset);
                    }
                    double dotProduct=0.0;
                    for (double cc: point) {
                        dotProduct+=cc;
                    }
                    dotProduct/=point.length;
                    double grayLength=dotProduct;
                    for (int dd=0; point.length>dd; ++dd) {
                        hue[dd]=point[dd]-dotProduct;
                    }
                    double hueLength=0.0;
                    for (int dd=0; point.length>dd; ++dd) {
                        hueLength+=Doubles.square(hue[dd]);
                    }
                    hueLength=Math.sqrt(hueLength/hue.length);
                    if (Math.min(0.01, 0.125*grayLength)>hueLength) {
                        output1.setNormalized(0, outputOffset1, grayLength);
                        return true;
                    }
                    else {
                        for (int dd=0; hue.length>dd; ++dd) {
                            output2.setNormalized(dd, outputOffset2, 0.5+0.5*hue[dd]);
                        }
                        return false;
                    }
                }
            };
        }
    }

    /**
     * Contains all the information to classify and transform pixels.
     * Implementations doesn't have to be thread-safe.
     */
    protected interface Projection {
        /**
         * Selects the class of the pixel at inputOffset in input.
         * Returns true if it is gray-like and puts its transformation into output1 at outputOffset1.
         * Returns false if it is colorful and puts its transformation into output2 at outputOffset2.
         */
        boolean project(
                MutablePoints input, int inputOffset,
                MutablePoints output1, int outputOffset1,
                MutablePoints output2, int outputOffset2);
    }

    private final Data data1=new Data(ClusterColors.falseGrays(3));
    private final Data data2=new Data(ClusterColors.falseColors(0, 1, 2));
    private final Mask mask;
    private final ClusteringStrategy<? super KDTree> strategy;
    private Sum.Factory sumFactory;

    private Cluster2(Image image, Mask mask, ClusteringStrategy<? super KDTree> strategy) {
        super(image);
        this.mask=mask;
        this.strategy=strategy;
    }

    /**
     * Checks the number of components of the input image.
     */
    protected abstract void checkImageDimensions(int dimensions);

    /**
     * Create {@link MutablePoints} compatible with grayish pixels.
     */
    protected abstract MutablePoints createPoints1(int expectedSize);

    /**
     * Create {@link MutablePoints} compatible with colorful pixels.
     */
    protected abstract MutablePoints createPoints2(int dimensions, int expectedSize);

    /**
     * Creates a new {@link Cluster2} instance which clusters pixels by value or hue depending on saturation.
     *
     * @param mask ignore parts of the input image
     * @param strategy clustering algorithm to be used
     */
    public static Image createHue(Image image, Mask mask, ClusteringStrategy<? super KDTree> strategy) {
        return new Hue(image, mask, strategy);
    }

    /**
     * Creates a new {@link Cluster2} instance which clusters pixels by intensity or hyper-hue depending on saturation.
     *
     * @param mask ignore parts of the input image
     * @param strategy clustering algorithm to be used
     */
    public static Image createHyperHue(Image image, Mask mask, ClusteringStrategy<? super KDTree> strategy) {
        return new HyperHue(image, mask, strategy);
    }

    /**
     * Returns the number of components used for colorful pixels.
     */
    protected abstract int dimensions2();

    @Override
    public void log(Map<String, Object> log) throws Throwable {
        log.put("type", "cluster2-"+logType());
        Log.logField("strategy", strategy, log);
        log.put("mask", mask);
        log.put("error", data1.clusters.error+data2.clusters.error);
        Map<String, Object> temp=new HashMap<>();
        Log.logClusters(data1.clusters, data1.colorMap, temp);
        temp.forEach((key, value)->log.put("gray-"+key, value));
        temp.clear();
        Log.logClusters(data2.clusters, data2.colorMap, temp);
        temp.forEach((key, value)->log.put("color-"+key, value));
    }

    /**
     * Returns the type of the clustering selection.
     */
    protected abstract String logType();

    private AsyncFunction<KDTree, Void> prepareCluster(Context context, Data data, MutablePoints points) {
        return (kdTree, continuation)->
                strategy.cluster(
                        context,
                        kdTree,
                        Continuations.map(
                                (result2, continuation2)->{
                                    data.clusters=result2;
                                    data.centers=Lists.flatten(data.clusters.centers);
                                    data.colorMap=data.colors.colors(data.clusters.centers, points);
                                    continuation2.completed(null);
                                },
                                continuation));
    }

    private AsyncSupplier<Void> prepareKDTree(
            Context context, Data data, List<MutablePoints.Interval> intervals, MutablePoints points) {
        return (continuation)->{
            points.compact(intervals);
            KDTree.create(
                    context,
                    4096,
                    points,
                    Continuations.map(
                            prepareCluster(context, data, points),
                            continuation));
        };
    }

    @Override
    protected void prepareImpl(Context context, Continuation<Dimensions> continuation) throws Throwable {
        checkImageDimensions(image.dimensions());
        sumFactory=context.sum();
        MutablePoints points1=createPoints1(image.height()*image.width());
        MutablePoints points2=createPoints2(dimensions2(), image.height()*image.width());
        points1.size(image.height()*image.width());
        points2.size(image.height()*image.width());
        Continuations.forkJoin(
                prepareLines(points1, points2),
                0,
                image.height(),
                Continuations.map(
                        (intervals, continuation2)->{
                            List<MutablePoints.Interval> intervals1=new ArrayList<>(intervals.size());
                            List<MutablePoints.Interval> intervals2=new ArrayList<>(intervals.size());
                            intervals.forEach((pair)->{
                                intervals1.add(pair.first);
                                intervals2.add(pair.second);
                            });
                            List<AsyncSupplier<Void>> forks=new ArrayList<>(2);
                            forks.add(prepareKDTree(context, data1, intervals1, points1));
                            forks.add(prepareKDTree(context, data2, intervals2, points2));
                            Continuations.forkJoin(
                                    forks,
                                    Continuations.map(
                                            (input, continuation3)->continuation3.completed(new Dimensions(
                                                    3,
                                                    image.height(),
                                                    image.width())),
                                            continuation2),
                                    context.executor());
                        },
                        continuation),
                context.executor());
    }

    private Continuations.IntForks<Pair<MutablePoints.Interval, MutablePoints.Interval>> prepareLines(
            MutablePoints points1, MutablePoints points2) {
        return (from, to)->(continuation)->{
            MutablePoints line=image.createPoints(image.dimensions(), image.width());
            Projection projection=projection();
            line.size(image.width());
            Reader reader=image.reader();
            int offsetFrom=from*image.width();
            int offsetTo1=offsetFrom;
            int offsetTo2=offsetFrom;
            for (int yy=from; to>yy; ++yy) {
                reader.setNormalizedLineTo(yy, line, 0);
                for (int xx=0; image.width()>xx; ++xx) {
                    if (mask.visible(xx, yy)) {
                        if (projection.project(line, xx, points1, offsetTo1, points2, offsetTo2)) {
                            ++offsetTo1;
                        }
                        else {
                            ++offsetTo2;
                        }
                    }
                }
            }
            continuation.completed(new Pair<>(
                    new MutablePoints.Interval(offsetFrom, offsetTo1),
                    new MutablePoints.Interval(offsetFrom, offsetTo2)));
        };
    }

    /**
     * Returns a new instance of {@link Projection} that can be used to classify and transform pixels.
     */
    protected abstract Projection projection();

    @Override
    public Reader reader() throws Throwable {
        return new TransformReader() {
            private final Function<Vector, Vector> nearestCenter1=KDTree.nearestCenter2(data1.centers, sumFactory);
            private final Function<Vector, Vector> nearestCenter2=KDTree.nearestCenter2(data2.centers, sumFactory);
            private final Vector point1=new Vector(1);
            private final Vector point2=new Vector(dimensions2());
            private final MutablePoints points1=createPoints1(1);
            private final MutablePoints points2=createPoints2(dimensions2(), 1);
            private final Projection projection=projection();

            {
                points1.size(1);
                points2.size(1);
            }

            @Override
            protected void setNormalizedLineToTransform(MutablePoints points, int offset) throws Throwable {
                for (int xx=0; line.size()>xx; ++xx, ++offset) {
                    Map<Vector, Vector> colorMap3;
                    Function<Vector, Vector> nearestCenter3;
                    Vector point3;
                    MutablePoints points3;
                    if (projection.project(line, xx, points1, 0, points2, 0)) {
                        colorMap3=data1.colorMap;
                        nearestCenter3=nearestCenter1;
                        point3=point1;
                        points3=points1;
                    }
                    else {
                        colorMap3=data2.colorMap;
                        nearestCenter3=nearestCenter2;
                        point3=point2;
                        points3=points2;
                    }
                    for (int dd=0; point3.dimensions()>dd; ++dd) {
                        point3.coordinate(dd, points3.get(dd, 0));
                    }
                    Vector center=nearestCenter3.apply(point3);
                    Vector color=colorMap3.get(center);
                    for (int dd=0; color.dimensions()>dd; ++dd) {
                        points.setNormalized(dd, offset, color.coordinate(dd));
                    }
                }
            }
        };
    }
}
