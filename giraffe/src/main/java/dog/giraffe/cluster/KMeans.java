package dog.giraffe.cluster;

import dog.giraffe.Context;
import dog.giraffe.Log;
import dog.giraffe.points.EmptySetException;
import dog.giraffe.points.Mean;
import dog.giraffe.points.Points;
import dog.giraffe.points.Sum;
import dog.giraffe.points.Vector;
import dog.giraffe.threads.AsyncFunction;
import dog.giraffe.threads.AsyncSupplier;
import dog.giraffe.threads.Continuation;
import dog.giraffe.threads.Continuations;
import dog.giraffe.util.Function;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class KMeans<P extends Points> {
    private static class Center {
        public final int index;
        public final Vector point;

        public Center(int index, Vector point) {
            this.index=index;
            this.point=point;
        }

        @Override
        public boolean equals(Object obj) {
            if (this==obj) {
                return true;
            }
            if ((null==obj)
                    || (!getClass().equals(obj.getClass()))) {
                return false;
            }
            return point.equals(((Center)obj).point);
        }

        @Override
        public int hashCode() {
            return point.hashCode();
        }

        public Vector point() {
            return point;
        }
    }

    private final Function<Center, Vector> centerPoint=Center::point;
    private final int clusters;
    private final Context context;
    private final double errorLimit;
    private final int maxIterations;
    private final List<List<Mean>> means;
    private final P points;
    private final List<Points> points2;
    private final ReplaceEmptyCluster<P> replaceEmptyCluster;
    private final List<Sum> sums;

    private KMeans(
            int clusters, Context context, double errorLimit, int maxIterations, List<List<Mean>> means, P points,
            List<Points> points2, ReplaceEmptyCluster<P> replaceEmptyCluster, List<Sum> sums) {
        this.clusters=clusters;
        this.context=context;
        this.errorLimit=errorLimit;
        this.maxIterations=maxIterations;
        this.means=means;
        this.points=points;
        this.points2=points2;
        this.replaceEmptyCluster=replaceEmptyCluster;
        this.sums=sums;
    }

    /**
     * Assigns data points to centers.
     */
    private AsyncSupplier<Void> classify(List<Center> centers, List<Mean> means, Points points, Sum sum) {
        return (continuation2)->{
            points.classify(
                    centerPoint,
                    centers,
                    new Points.Classification<>() {
                        @Override
                        public void nearestCenter(Center center, Points points) {
                            points.addAllTo(means.get(center.index));
                            points.addAllDistanceTo(center.point, sum);
                        }

                        @Override
                        public void nearestCenter(Center center, Points points, int index) {
                            points.addTo(index, means.get(center.index));
                            points.addDistanceTo(center.point, index, sum);
                        }
                    });
            continuation2.completed(null);
        };
    }

    /**
     * Forks classification.
     */
    private void fork(
            List<Vector> centers, Continuation<Clusters> continuation, double error, int iteration) throws Throwable {
        context.checkStopped();
        if (maxIterations<=iteration) {
            continuation.completed(Clusters.create(centers, error));
            return;
        }
        for (List<Mean> means2: means) {
            for (Mean mean: means2) {
                mean.clear();
            }
        }
        for (Sum sum: sums) {
            sum.clear();
        }
        List<Center> centers2=new ArrayList<>(centers.size());
        for (int ii=0; centers.size()>ii; ++ii) {
            centers2.add(new Center(ii, centers.get(ii)));
        }
        List<AsyncSupplier<Void>> forks=new ArrayList<>(points2.size());
        for (int ii=0; points2.size()>ii; ++ii) {
            forks.add(classify(centers2, means.get(ii), points2.get(ii), sums.get(ii)));
        }
        Continuations.forkJoin(
                forks,
                Continuations.map(
                        join(error, iteration),
                        continuation),
                context.executor());
    }

    /**
     * Calculates new cluster centers.
     */
    private AsyncFunction<List<Void>, Clusters> join(double error, int iteration) {
        return (input, continuation)->{
            List<Mean> means2=means.get(0);
            Sum sum2=sums.get(0);
            for (int ii=1; points2.size()>ii; ++ii) {
                for (int jj=0; clusters>jj; ++jj) {
                    means.get(ii).get(jj).addTo(means2.get(jj));
                }
                sums.get(ii).addTo(sum2);
            }
            Set<Vector> centers=new HashSet<>(clusters);
            for (Mean mean: means2) {
                Vector center;
                try {
                    center=mean.mean();
                }
                catch (EmptySetException ignore) {
                    continue;
                }
                centers.add(center);
            }
            InitialCenters.newCenters(
                    centers,
                    clusters,
                    context,
                    Continuations.map(
                            (newCenters, continuation2)->{
                                double error2=sum2.sum();
                                if (error*errorLimit<=error2) {
                                    continuation2.completed(Clusters.create(newCenters, error2));
                                }
                                else {
                                    fork(newCenters, continuation2, error2, iteration+1);
                                }
                            },
                            continuation),
                    maxIterations,
                    points,
                    points2,
                    replaceEmptyCluster,
                    replaceEmptyCluster);
        };
    }

    /**
     * The k-means algorithm, also known as Lloyd's algorithm.
     * The algorithm uses early stopping when the new clusters error/current cluster error &gt;= errorLimit.
     *
     * @param clusters the number of clusters to use
     * @param maxIterations the maximum number of iterations the algorithm recalculates centers
     */
    public static <P extends Points> ClusteringStrategy<P> kMeans(
            int clusters, double errorLimit, InitialCenters<P> initialCenters, int maxIterations,
            ReplaceEmptyCluster<P> replaceEmptyCluster) {
        return new ClusteringStrategy<>() {
            @Override
            public void cluster(Context context, P points, Continuation<Clusters> continuation) throws Throwable {
                if (0>=clusters) {
                    continuation.failed(new IllegalStateException(Integer.toString(clusters)));
                    return;
                }
                if (points.size()<clusters) {
                    continuation.failed(new CannotSelectInitialCentersException(String.format(
                            "too few data points; clusters: %1$d, data points: %2$d", clusters, points.size())));
                    return;
                }
                List<Points> points2=points.split(context.executor().threads());
                List<List<Mean>> means=new ArrayList<>(points2.size());
                List<Sum> sums=new ArrayList<>(points2.size());
                for (Points points3: points2) {
                    List<Mean> means2=new ArrayList<>(clusters);
                    for (int ii=clusters; 0<ii; --ii) {
                        means2.add(points.mean().create((means.isEmpty()?points:points3).size(), context.sum()));
                    }
                    means.add(Collections.unmodifiableList(means2));
                    sums.add(context.sum().create((sums.isEmpty()?points:points3).size()));
                }
                initialCenters.initialCenters(
                        clusters,
                        context,
                        maxIterations,
                        points,
                        points2,
                        Continuations.map(
                                (centers, continuation2)->{
                                    if (centers.size()!=clusters) {
                                        continuation2.failed(new CannotSelectInitialCentersException());
                                        return;
                                    }
                                    KMeans<P> kMeans=new KMeans<>(
                                            clusters,
                                            context,
                                            errorLimit,
                                            maxIterations,
                                            Collections.unmodifiableList(means),
                                            points,
                                            points2,
                                            replaceEmptyCluster,
                                            Collections.unmodifiableList(sums));
                                    kMeans.fork(centers, continuation2, Double.POSITIVE_INFINITY, 0);
                                },
                                continuation));
            }

            @Override
            public void log(Map<String, Object> log) throws Throwable {
                log.put("type", "k-means");
                log.put("clusters", clusters);
                log.put("error-limit", errorLimit);
                Log.logField("initial-centers", initialCenters, log);
                log.put("max-iterations", maxIterations);
                Log.logField("replace-empty-cluster", replaceEmptyCluster, log);
            }
        };
    }
}
