package dog.giraffe;

import dog.giraffe.threads.AsyncSupplier;
import dog.giraffe.threads.Continuation;
import dog.giraffe.threads.Continuations;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class KMeans<T> {
    public interface Distance<T> {
        double distance(T center, T point);
    }

    private final int clusters;
    private final Context context;
    private final Distance<T> distance;
    private final double errorLimit;
    private final int maxIterations;
    private final VectorMean.Factory<T> meanFactory;
    private final List<T> points;
    private final Sum.Factory sumFactory;

    private KMeans(
            int clusters, Context context, Distance<T> distance, double errorLimit, int maxIterations,
            VectorMean.Factory<T> meanFactory, List<T> points, Sum.Factory sumFactory) {
        this.clusters=clusters;
        this.context=context;
        this.distance=distance;
        this.errorLimit=errorLimit;
        this.maxIterations=maxIterations;
        this.meanFactory=meanFactory;
        this.points=points;
        this.sumFactory=sumFactory;
    }

    public static <T> void cluster(
            int clusters, Context context, Continuation<List<T>> continuation, Distance<T> distance, double errorLimit,
            int maxIterations, VectorMean.Factory<T> meanFactory, Sum.Factory sumFactory, Iterable<T> values)
            throws Throwable {
        if (2>clusters) {
            throw new IllegalStateException(Integer.toString(clusters));
        }
        Objects.requireNonNull(values);
        List<T> points=new ArrayList<>();
        for (T value: values) {
            points.add(value);
        }
        if (points.size()<=clusters) {
            throw new RuntimeException("too few data points");
        }
        Set<T> centers=new HashSet<>(clusters);
        for (int ii=maxIterations*clusters; clusters>centers.size(); --ii) {
            if (0>=ii) {
                throw new RuntimeException("cannot select initial cluster centers");
            }
            centers.add(points.get(context.random().nextInt(points.size())));
        }
        double error=Double.POSITIVE_INFINITY;
        new KMeans<>(clusters, context, distance, errorLimit, maxIterations, meanFactory, points, sumFactory)
                .cluster(centers, continuation, error, 0);
    }

    public void cluster(
            Set<T> centers, Continuation<List<T>> continuation, double error, int iteration) throws Throwable {
        context.checkStopped();
        if (maxIterations<=iteration) {
            continuation.completed(new ArrayList<>(centers));
            return;
        }
        int threads=Math.max(1, Math.min(points.size(), context.executor().threads()));
        List<AsyncSupplier<Pair<Sum, Map<T, List<T>>>>> forks=new ArrayList<>(threads);
        for (int tt=0; threads>tt; ++tt) {
            int start=tt*points.size()/threads;
            int end=(tt+1)*points.size()/threads;
            forks.add((continuation2)->{
                Map<T, List<T>> voronoi=new HashMap<>(centers.size());
                for (T center: centers) {
                    voronoi.put(center, new ArrayList<>(end-start));
                }
                Sum errorSum=sumFactory.create(end-start);
                for (int ii=start; end>ii; ++ii) {
                    T point=points.get(ii);
                    T center=nearestCenter(centers, distance, point);
                    errorSum=errorSum.add(distance.distance(center, point));
                    voronoi.get(center).add(point);
                }
                continuation2.completed(new Pair<>(errorSum, voronoi));
            });
        }
        Continuations.forkJoin(
                forks,
                Continuations.map(
                        (results, continuation2)->{
                            Sum errorSum=sumFactory.create(points.size());
                            Map<T, List<T>> voronoi=new HashMap<>(centers.size());
                            for (T center: centers) {
                                List<T> cluster=new ArrayList<>(points.size());
                                voronoi.put(center, cluster);
                            }
                            for (Pair<Sum, Map<T, List<T>>> result: results) {
                                errorSum=result.first.addTo(errorSum);
                                for (Map.Entry<T, List<T>> entry: result.second.entrySet()) {
                                    voronoi.get(entry.getKey()).addAll(entry.getValue());
                                }
                            }
                            double error2=errorSum.sum();
                            if (Double.isFinite(error2)
                                    && (error2>=error*errorLimit)) {
                                continuation2.completed(new ArrayList<>(centers));
                                return;
                            }
                            cluster2(continuation2, error2, iteration, voronoi);
                        },
                        continuation),
                context.executor());
    }

    public void cluster2(
            Continuation<List<T>> continuation, double error, int iteration, Map<T, List<T>> voronoi)
            throws Throwable {
        List<AsyncSupplier<T>> forks=new ArrayList<>(voronoi.size());
        for (List<T> cluster: voronoi.values()) {
            forks.add((continuation2)->{
                VectorMean<T> mean=meanFactory.create(cluster.size());
                for (T point: cluster) {
                    mean=mean.add(point);
                }
                continuation2.completed(mean.mean());
            });
        }
        Continuations.forkJoin(
                forks,
                Continuations.map(
                        (centers2, continuation2)->{
                            if (centers2.size()<clusters) {
                                throw new RuntimeException("cluster collapse");
                            }
                            cluster(new HashSet<>(centers2), continuation2, error, iteration+1);
                        },
                        continuation),
                context.executor());
    }

    public static <T> T nearestCenter(Iterable<T> centers, Distance<T> distance, T point) {
        T bestCenter=null;
        double bestDistance=Double.POSITIVE_INFINITY;
        for (T cc: centers) {
            double dd=distance.distance(cc, point);
            if (dd<bestDistance) {
                bestCenter=cc;
                bestDistance=dd;
            }
        }
        if (null==bestCenter) {
            throw new RuntimeException("cannot select nearest center");
        }
        return bestCenter;
    }
}
