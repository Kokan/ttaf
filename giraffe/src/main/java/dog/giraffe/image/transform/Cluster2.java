package dog.giraffe.image.transform;

import dog.giraffe.ClusterColors;
import dog.giraffe.ClusteringStrategy;
import dog.giraffe.Clusters;
import dog.giraffe.ColorConverter;
import dog.giraffe.Context;
import dog.giraffe.Doubles;
import dog.giraffe.Lists;
import dog.giraffe.Pair;
import dog.giraffe.Sum;
import dog.giraffe.image.Image;
import dog.giraffe.points.FloatArrayPoints;
import dog.giraffe.points.KDTree;
import dog.giraffe.points.MutablePoints;
import dog.giraffe.points.Vector;
import dog.giraffe.threads.AsyncFunction;
import dog.giraffe.threads.AsyncSupplier;
import dog.giraffe.threads.Continuation;
import dog.giraffe.threads.Continuations;
import dog.giraffe.threads.Function;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        protected MutablePoints createPoints1(int dimensions, int expectedSize) {
            return new FloatArrayPoints(dimensions, expectedSize);
        }

        @Override
        protected MutablePoints createPoints2(int dimensions, int expectedSize) {
            return new FloatArrayPoints(dimensions, expectedSize);
        }

        @Override
        protected int dimensions1() {
            return 1;
        }

        @Override
        protected int dimensions2() {
            return 1;
        }

        @Override
        protected Projection projection() {
            return new Projection() {
                private final ColorConverter colorConverter=new ColorConverter();

                @Override
                public boolean project(
                        MutablePoints input, int inputOffset, MutablePoints output1, int outputOffset1,
                        MutablePoints output2, int outputOffset2) {
                    colorConverter.rgbToHslv(
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
        protected MutablePoints createPoints1(int dimensions, int expectedSize) {
            return new FloatArrayPoints(dimensions, expectedSize);
        }

        @Override
        protected MutablePoints createPoints2(int dimensions, int expectedSize) {
            return new FloatArrayPoints(dimensions, expectedSize);
        }

        @Override
        protected int dimensions1() {
            return 1;
        }

        @Override
        protected int dimensions2() throws Throwable {
            return image.dimensions();
        }

        @Override
        protected Projection projection() throws Throwable {
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
                    for (int dd=0; point.length>dd; ++dd) {
                        dotProduct+=point[dd];
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

    protected interface Projection {
        boolean project(
                MutablePoints input, int inputOffset,
                MutablePoints output1, int outputOffset1, MutablePoints output2, int outputOffset2);
    }

    private final Data data1=new Data(ClusterColors.Gray.falseColor(3));
    private final Data data2=new Data(ClusterColors.RGB.falseColor(0, 1, 2));
    private final Mask mask;
    private final ClusteringStrategy<? super KDTree> strategy;
    private Sum.Factory sumFactory;

    private Cluster2(Image image, Mask mask, ClusteringStrategy<? super KDTree> strategy) {
        super(image);
        this.mask=mask;
        this.strategy=strategy;
    }

    protected abstract void checkImageDimensions(int dimensions);

    protected abstract MutablePoints createPoints1(int dimensions, int expectedSize);

    protected abstract MutablePoints createPoints2(int dimensions, int expectedSize);

    public static Image createHue(Image image, Mask mask, ClusteringStrategy<? super KDTree> strategy) {
        return new Hue(image, mask, strategy);
    }

    public static Image createHyperHue(Image image, Mask mask, ClusteringStrategy<? super KDTree> strategy) {
        return new HyperHue(image, mask, strategy);
    }

    protected abstract int dimensions1();

    protected abstract int dimensions2() throws Throwable;

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
        MutablePoints points1=createPoints1(dimensions1(), image.height()*image.width());
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

    protected abstract Projection projection() throws Throwable;

    @Override
    public Reader reader() throws Throwable {
        return new TransformReader() {
            private final Function<Vector, Vector> nearestCenter1=KDTree.nearestCenter2(data1.centers, sumFactory);
            private final Function<Vector, Vector> nearestCenter2=KDTree.nearestCenter2(data2.centers, sumFactory);
            private final Vector point1=new Vector(dimensions1());
            private final Vector point2=new Vector(dimensions2());
            private final MutablePoints points1=createPoints1(dimensions1(), 1);
            private final MutablePoints points2=createPoints2(dimensions2(), 1);
            private final Projection projection=projection();

            {
                points1.size(1);
                points2.size(1);
            }

            @Override
            protected void setNormalizedLineToTransform(int yy, MutablePoints points, int offset) throws Throwable {
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
