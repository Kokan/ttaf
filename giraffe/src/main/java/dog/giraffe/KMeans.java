package dog.giraffe;

import dog.giraffe.points.Points;
import dog.giraffe.threads.AsyncSupplier;
import dog.giraffe.threads.Continuation;
import dog.giraffe.threads.Continuations;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class KMeans {
    public static class CannotSelectInitialCenters extends RuntimeException {
        private static final long serialVersionUID=0L;
    }

    private static class Center<T> {
        public final int index;
        public final T point;

        public Center(int index, T point) {
            this.index=index;
            this.point=point;
        }
        
        public T point() {
            return point;
        }
    }

    @FunctionalInterface
    public interface EmptyCluster<D extends Distance<T>, M extends VectorMean<M, T>, P extends Points<D, M, P, T>, T> {
        T emptyCluster(Context context, P points);

        static <D extends Distance<T>, M extends VectorMean<M, T>, P extends Points<D, M, P, T>, T>
        EmptyCluster<D, M, P, T> error() {
            return (context, points)->{
                throw new EmptyClusterException();
            };
        }

        static <D extends Distance<T>, M extends VectorMean<M, T>, P extends Points<D, M, P, T>, T>
        EmptyCluster<D, M, P, T> random() {
            return (context, points)->points.get(context.random().nextInt(points.size()));
        }
    }

    public static class EmptyClusterException extends RuntimeException {
        private static final long serialVersionUID=0L;
    }

    private KMeans() {
    }

    public static <D extends Distance<T>, M extends VectorMean<M, T>, P extends Points<D, M, P, T>, T>
    void cluster(
            int clusters, Context context, Continuation<List<T>> continuation, EmptyCluster<D, M, P, T> emptyCluster,
            double errorLimit, int maxIterations, P points, Sum.Factory sumFactory) throws Throwable {
        if (2>clusters) {
            throw new IllegalStateException(Integer.toString(clusters));
        }
        if (points.size()<clusters) {
            throw new RuntimeException("too few data points");
        }
        Set<T> centers=new HashSet<>(clusters);
        for (int ii=maxIterations*clusters; clusters>centers.size(); --ii) {
            if (0>=ii) {
                throw new CannotSelectInitialCenters();
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
        cluster2(
                Collections.unmodifiableList(new ArrayList<>(centers)),
                clusters,
                context,
                continuation,
                emptyCluster,
                Double.POSITIVE_INFINITY,
                errorLimit,
                0,
                maxIterations,
                Collections.unmodifiableList(means),
                points,
                points2,
                Collections.unmodifiableList(sums));
    }

    public static <D extends Distance<T>, M extends VectorMean<M, T>, P extends Points<D, M, P, T>, T>
    void cluster2(
            List<T> centers, int clusters, Context context, Continuation<List<T>> continuation,
            EmptyCluster<D, M, P, T> emptyCluster, double error, double errorLimit, int iteration, int maxIterations,
            List<List<M>> means, P points, List<P> points2, List<Sum> sums) throws Throwable {
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
        Function<Center<T>, T> centerPoint=Center::point;
        List<AsyncSupplier<Void>> forks=new ArrayList<>(points2.size());
        for (int ii=0; points2.size()>ii; ++ii) {
            List<M> means2=means.get(ii);
            P points3=points2.get(ii);
            Sum sum2=sums.get(ii);
            forks.add((continuation2)->{
                points3.classify(
                        centerPoint, centers2,
                        new Points.Classification<Center<T>, D, M, P, T>() {
                            @Override
                            public void nearestCenter(Center<T> center, P points) {
                                points.addAllTo(means2.get(center.index));
                                points.addAllDistanceTo(center.point, sum2);
                            }

                            @Override
                            public void nearestCenter(Center<T> center, P points, int index) {
                                points.addTo(index, means2.get(center.index));
                                points.addDistanceTo(center.point, index, sum2);
                            }
                        });
                continuation2.completed(null);
            });
        }
        Continuation<List<Void>> join=Continuations.map(
                (input, continuation2)->{
                    List<M> means2=means.get(0);
                    Sum sum2=sums.get(0);
                    for (int ii=1; points2.size()>ii; ++ii) {
                        for (int jj=0; centers.size()>jj; ++jj) {
                            means.get(ii).get(jj).addTo(means2.get(jj));
                        }
                        sums.get(ii).addTo(sum2);
                    }
                    Set<T> centers3=new HashSet<>(centers.size());
                    for (M mean: means2) {
                        T center;
                        try {
                            center=mean.mean();
                        }
                        catch (VectorMean.EmptySetException ignore) {
                            continue;
                        }
                        centers3.add(center);
                    }
                    for (int ii=maxIterations; centers3.size()<clusters; --ii) {
                        if (0>=ii) {
                            throw new EmptyClusterException();
                        }
                        centers3.add(emptyCluster.emptyCluster(context, points));
                    }
                    List<T> centers4=Collections.unmodifiableList(new ArrayList<>(centers3));
                    double error2=sum2.sum();
                    if (error*errorLimit<=error2) {
                        continuation2.completed(centers4);
                        return;
                    }
                    cluster2(
                            centers4,
                            clusters,
                            context,
                            continuation2,
                            emptyCluster,
                            error2,
                            errorLimit,
                            iteration+1,
                            maxIterations,
                            means,
                            points,
                            points2,
                            sums);
                },
                continuation);
        Continuations.forkJoin(forks, join, context.executor());
    }

    public static <T> T nearestCenter(Iterable<T> centers, Distance<T> distance, T point) {
        T bestCenter=null;
        double bestDistance=Double.POSITIVE_INFINITY;
        for (T center: centers) {
            double dd=distance.distance(center, point);
            if (dd<bestDistance) {
                bestCenter=center;
                bestDistance=dd;
            }
        }
        if (null==bestCenter) {
            throw new RuntimeException("cannot select nearest center");
        }
        return bestCenter;
    }
}
