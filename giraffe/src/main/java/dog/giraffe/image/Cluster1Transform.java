package dog.giraffe.image;

import dog.giraffe.ClusterColors;
import dog.giraffe.ClusteringStrategy;
import dog.giraffe.Clusters;
import dog.giraffe.Context;
import dog.giraffe.Lists;
import dog.giraffe.Sum;
import dog.giraffe.points.Distance;
import dog.giraffe.points.KDTree;
import dog.giraffe.points.MutablePoints;
import dog.giraffe.points.Vector;
import dog.giraffe.threads.Continuation;
import dog.giraffe.threads.Continuations;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class Cluster1Transform implements ImageTransform {
    private List<Vector> centers;
    private Map<Vector, Vector> colorMap;
    private final ClusterColors colors;
    private Clusters clusters;
    private final Projection1.Factory projection;
    private final ClusteringStrategy<? super KDTree> strategy;

    public Cluster1Transform(
            ClusterColors colors, Projection1.Factory projection, ClusteringStrategy<? super KDTree> strategy) {
        this.colors=colors;
        this.projection=projection;
        this.strategy=strategy;
    }

    @Override
    public int dimensions() {
        return colors.dimensions();
    }

    @Override
    public void prepare(ImageReader imageReader) {
    }

    @Override
    public void prepare(Context context, ImageReader imageReader, Continuation<Void> continuation) throws Throwable {
        Projection1 projection2=projection.create(imageReader);
        MutablePoints points=projection2.createPoints(imageReader.width()*imageReader.height());
        MutablePoints line=imageReader.createPoints(imageReader.width());
        for (int yy=0; imageReader.height()>yy; ++yy) {
            line.clear();
            imageReader.addLineTo(yy, line);
            projection2.project(line, points);
        }
        strategy.cluster(
                context,
                KDTree.create(4096, points, context.sum()),
                Continuations.map(
                        (result, continuation2)->{
                            clusters=result;
                            centers=Lists.flatten(clusters.centers);
                            colorMap=colors.colors(clusters.centers, points);
                            continuation2.completed(null);
                        },
                        continuation));
    }

    @Override
    public PrepareLine prepareLine(Context context, ImageReader imageReader) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean prepareLines() {
        return false;
    }

    @Override
    public Write write(Context context, ImageReader imageReader, ImageWriter imageWriter, int dimension) {
        return new Write() {
            private final Function<Vector, Vector> nearestCenter=(16>centers.size())
                    ?Distance.nearestCenter(centers)
                    :KDTree.nearestCenter(centers, Sum.PREFERRED);
            private final Vector point;
            private final Projection1 projection2=projection.create(imageReader);

            {
                point=new Vector(projection2.dimensions());
            }

            @Override
            public void close() {
            }

            @Override
            public void write(Context context, MutablePoints inputLine, ImageWriter.Line outputLine, int dimension) {
                for (int xx=0; inputLine.size()>xx; ++xx) {
                    projection2.project(inputLine, xx, point);
                    Vector center=nearestCenter.apply(point);
                    Vector color=colorMap.get(center);
                    for (int dd=0; color.dimensions()>dd; ++dd) {
                        outputLine.setNormalized(dimension+dd, xx, color.coordinate(dd));
                    }
                }
            }
        };
    }
}
