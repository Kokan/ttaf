package dog.giraffe.image.transform;

import dog.giraffe.ClusterColors;
import dog.giraffe.ClusteringStrategy;
import dog.giraffe.Clusters;
import dog.giraffe.Context;
import dog.giraffe.Lists;
import dog.giraffe.Sum;
import dog.giraffe.image.Image;
import dog.giraffe.points.KDTree;
import dog.giraffe.points.MutablePoints;
import dog.giraffe.points.UnsignedByteArrayPoints;
import dog.giraffe.points.Vector;
import dog.giraffe.threads.AsyncFunction;
import dog.giraffe.threads.Continuation;
import dog.giraffe.threads.Continuations;
import dog.giraffe.threads.Function;
import java.util.List;
import java.util.Map;

public class Cluster1 extends Image.Transform {
    private List<Vector> centers;
    private Clusters clusters;
    private Map<Vector, Vector> colorMap;
    private final ClusterColors colors;
    private final Mask mask;
    private final ClusteringStrategy<? super KDTree> strategy;
    private Sum.Factory sumFactory;

    private Cluster1(Image image, ClusterColors colors, Mask mask, ClusteringStrategy<? super KDTree> strategy) {
        super(image);
        this.colors=colors;
        this.mask=mask;
        this.strategy=strategy;
    }
    
    public static Image create(
            Image image, ClusterColors colors, Mask mask, ClusteringStrategy<? super KDTree> strategy) {
        return new Cluster1(image, colors, mask, strategy);
    }

    @Override
    public MutablePoints createPoints(int dimensions, int expectedSize) throws Throwable {
        return new UnsignedByteArrayPoints(dimensions, expectedSize);
    }

    private AsyncFunction<KDTree, Dimensions> prepareCluster(Context context, MutablePoints points) {
        return (kdTree, continuation)->
            strategy.cluster(
                    context,
                    kdTree,
                    Continuations.map(
                            (result2, continuation2)->{
                                clusters=result2;
                                centers=Lists.flatten(clusters.centers);
                                colorMap=colors.colors(clusters.centers, points);
                                continuation2.completed(new Dimensions(
                                        colors.dimensions(),
                                        image.height(),
                                        image.width()));
                            },
                            continuation));
    }

    private AsyncFunction<List<MutablePoints.Interval>, Dimensions> prepareKDTree(
            Context context, MutablePoints points) {
        return (intervals, continuation)->{
            points.compact(intervals);
            KDTree.create(
                    context,
                    4096,
                    points,
                    Continuations.map(
                            prepareCluster(context, points),
                            continuation));
        };
    }

    @Override
    protected void prepareImpl(Context context, Continuation<Dimensions> continuation) throws Throwable {
        sumFactory=context.sum();
        MutablePoints points=image.createPoints(image.dimensions(), image.height()*image.width());
        points.size(image.height()*image.width());
        Continuations.forkJoin(
                prepareLines(points),
                0,
                image.height(),
                Continuations.map(
                        prepareKDTree(context, points),
                        continuation),
                context.executor());
    }

    private Continuations.IntForks<MutablePoints.Interval> prepareLines(MutablePoints points) {
        return (from, to)->(continuation)->{
            MutablePoints line=image.createPoints(image.dimensions(), image.width());
            line.size(image.width());
            Reader reader=image.reader();
            int offsetFrom=from*image.width();
            int offsetTo=offsetFrom;
            for (int yy=from; to>yy; ++yy) {
                reader.setNormalizedLineTo(yy, line, 0);
                for (int xx=0; image.width()>xx; ++xx) {
                    if (mask.visible(xx, yy)) {
                        for (int dd=0; image.dimensions()>dd; ++dd) {
                            points.setNormalized(dd, offsetTo, line.getNormalized(dd, xx));
                        }
                        ++offsetTo;
                    }
                }
            }
            continuation.completed(new MutablePoints.Interval(offsetFrom, offsetTo));
        };
    }

    @Override
    public Reader reader() throws Throwable {
        return new TransformReader() {
            private final Function<Vector, Vector> nearestCenter=KDTree.nearestCenter2(centers, sumFactory);
            private final Vector point=new Vector(line.dimensions());

            @Override
            protected void setNormalizedLineToTransform(int yy, MutablePoints points, int offset) throws Throwable {
                for (int xx=0; line.size()>xx; ++xx, ++offset) {
                    for (int dd=0; line.dimensions()>dd; ++dd) {
                        point.coordinate(dd, line.get(dd, xx));
                    }
                    Vector center=nearestCenter.apply(point);
                    Vector color=colorMap.get(center);
                    for (int dd=0; color.dimensions()>dd; ++dd) {
                        points.setNormalized(dd, offset, color.coordinate(dd));
                    }
                }
            }
        };
    }
}
