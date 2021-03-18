package dog.giraffe.kmeans;

import dog.giraffe.Context;
import dog.giraffe.Distance;
import dog.giraffe.Sum;
import dog.giraffe.VectorMean;
import dog.giraffe.points.Points;
import dog.giraffe.threads.AsyncFunction;
import dog.giraffe.threads.AsyncSupplier;
import dog.giraffe.threads.Continuation;
import dog.giraffe.threads.Continuations;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class KMeans<D extends Distance<T>, M extends VectorMean<M, T>, P extends Points<D, M, P, T>, T> {
    private static class Center<T> {
        public final int index;
        public final T point;

        public Center(int index, T point) {
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
            return point.equals(((Center<?>)obj).point);
        }

        @Override
        public int hashCode() {
            return point.hashCode();
        }

        public T point() {
            return point;
        }
    }

    private final Function<Center<T>, T> centerPoint=Center::point;
    private final int clusters;
    private final Context context;
    private final double errorLimit;
    private final int maxIterations;
    private final List<List<M>> means;
    private final P points;
    private final List<P> points2;
    private final ReplaceEmptyCluster<D, M, P, T> replaceEmptyCluster;
    private final List<Sum> sums;

    private KMeans(
            int clusters, Context context, double errorLimit, int maxIterations, List<List<M>> means, P points,
            List<P> points2, ReplaceEmptyCluster<D, M, P, T> replaceEmptyCluster, List<Sum> sums) {
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

    public static <D extends Distance<T>, M extends VectorMean<M, T>, P extends Points<D, M, P, T>, T>
    void cluster(
            int clusters, Context context, Continuation<List<T>> continuation,
            double errorLimit, int maxIterations, P points, ReplaceEmptyCluster<D, M, P, T> replaceEmptyCluster,
            Sum.Factory sumFactory) throws Throwable {
        if (2>clusters) {
            continuation.failed(new IllegalStateException(Integer.toString(clusters)));
            return;
        }
        if (points.size()<clusters) {
            continuation.failed(new RuntimeException("too few data points"));
            return;
        }
        Set<T> centers=new HashSet<>(clusters);
        for (int ii=maxIterations*clusters; clusters>centers.size(); --ii) {
            if (0>=ii) {
                continuation.failed(new CannotSelectInitialCentersException());
                return;
            }
            centers.add(points.get(context.random().nextInt(points.size())));
        }
        List<P> points2=points.split(context.executor().threads());
        List<List<M>> means=new ArrayList<>(points2.size());
        List<Sum> sums=new ArrayList<>(points2.size());
        for (P points3: points2) {
            List<M> means2=new ArrayList<>(centers.size());
            for (int ii=centers.size(); 0<ii; --ii) {
                means2.add(points.mean().create((means.isEmpty()?points:points3).size(), sumFactory));
            }
            means.add(Collections.unmodifiableList(means2));
            sums.add(sumFactory.create((sums.isEmpty()?points:points3).size()));
        }
        new KMeans<D, M, P, T>(
                clusters, context, errorLimit, maxIterations, Collections.unmodifiableList(means), points,
                points2, replaceEmptyCluster, Collections.unmodifiableList(sums))
                .fork(
                        Collections.unmodifiableList(new ArrayList<>(centers)),
                        continuation,
                        Double.POSITIVE_INFINITY,
                        0);
    }

    private AsyncSupplier<Void> classify(List<Center<T>> centers, List<M> means, P points, Sum sum) {
        return (continuation2)->{
            points.classify(
                    centerPoint,
                    centers,
                    new Points.Classification<Center<T>, D, M, P, T>() {
                        @Override
                        public void nearestCenter(Center<T> center, P points) {
                            points.addAllTo(means.get(center.index));
                            points.addAllDistanceTo(center.point, sum);
                        }

                        @Override
                        public void nearestCenter(Center<T> center, P points, int index) {
                            points.addTo(index, means.get(center.index));
                            points.addDistanceTo(center.point, index, sum);
                        }
                    });
            continuation2.completed(null);
        };
    }

    private void fork(
            List<T> centers, Continuation<List<T>> continuation, double error, int iteration) throws Throwable {
        context.checkStopped();
        if (maxIterations<=iteration) {
            continuation.completed(new ArrayList<>(centers));
            return;
        }
        for (List<M> means2: means) {
            for (M mean: means2) {
                mean.clear();
            }
        }
        for (Sum sum: sums) {
            sum.clear();
        }
        List<Center<T>> centers2=new ArrayList<>(centers.size());
        for (int ii=0; centers.size()>ii; ++ii) {
            centers2.add(new Center<>(ii, centers.get(ii)));
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

    private AsyncFunction<List<Void>, List<T>> join(double error, int iteration) {
        return (input, continuation)->{
            List<M> means2=means.get(0);
            Sum sum2=sums.get(0);
            for (int ii=1; points2.size()>ii; ++ii) {
                for (int jj=0; clusters>jj; ++jj) {
                    means.get(ii).get(jj).addTo(means2.get(jj));
                }
                sums.get(ii).addTo(sum2);
            }
            Set<T> centers=new HashSet<>(clusters);
            for (M mean: means2) {
                T center;
                try {
                    center=mean.mean();
                }
                catch (VectorMean.EmptySetException ignore) {
                    continue;
                }
                centers.add(center);
            }
            newCenters(centers, continuation, error, iteration, sum2);
        };
    }

    private void newCenters(
            Set<T> centers, Continuation<List<T>> continuation, double error, int iteration, Sum sum)
            throws Throwable {
        List<T> centers2=Collections.unmodifiableList(new ArrayList<>(centers));
        if (centers2.size()>=clusters) {
            double error2=sum.sum();
            if (error*errorLimit<=error2) {
                continuation.completed(centers2);
            }
            else {
                fork(centers2, continuation, error2, iteration+1);
            }
            return;
        }
        replaceEmptyCluster.newCenter(
                centers2,
                context,
                maxIterations,
                points,
                points2,
                Continuations.async(
                        Continuations.map(
                            (center, continuation2)->{
                                Set<T> centers3=new HashSet<>(clusters);
                                centers3.addAll(centers);
                                if (centers3.add(center)) {
                                    newCenters(centers3, continuation2, error, iteration, sum);
                                }
                                else {
                                    System.out.println(centers+" - "+center);
                                    continuation.failed(new EmptyClusterException());
                                }
                            },
                            continuation),
                        context.executor()));
    }
}