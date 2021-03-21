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
    private int clusters;
    private final int max_clusters;
    private final int clusterMinSize;
    private final double lumping;
    private final double std_deviation;
    private final Context context;
    private final Distance<T> distance;
    private final MaxComponent<Double,T> max;
    private final double errorLimit;
    private final int maxIterations;
    private final VectorMean.Factory<T> meanFactory;
    private final VectorStdDeviation.Factory<T> devFactory;
    private final List<T> points;
    private final Sum.Factory sumFactory;

    private KMeans(
            int clusters, int max_clusters, Context context, Distance<T> distance, MaxComponent<Double,T> max, double errorLimit, int clusterMinSize, double lumping, double std_deviation, int maxIterations,
            VectorMean.Factory<T> meanFactory, VectorStdDeviation.Factory<T> devFactory, List<T> points, Sum.Factory sumFactory) {
        this.clusters=clusters;
        this.max_clusters=max_clusters;
        this.clusterMinSize=clusterMinSize;
        this.lumping=lumping;
        this.std_deviation=std_deviation;
        this.context=context;
        this.distance=distance;
        this.max=max;
        this.errorLimit=errorLimit;
        this.maxIterations=maxIterations;
        this.meanFactory=meanFactory;
        this.devFactory=devFactory;
        this.points=points;
        this.sumFactory=sumFactory;
    }

    public static <T> void cluster(
            int clusters, int max_clusters, Context context, Continuation<List<T>> continuation, Distance<T> distance, MaxComponent<Double,T> max, double errorLimit,
            int maxIterations, VectorMean.Factory<T> meanFactory, VectorStdDeviation.Factory<T> devFactory, Sum.Factory sumFactory, Iterable<T> values)
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
        new KMeans<>(clusters, max_clusters, context, distance, max, errorLimit, (int)(points.size()*0.005), 0.5, 1, maxIterations, meanFactory, devFactory, points, sumFactory)
                .start(centers, continuation, error, 0);
    }

    public void start(Set<T> centers, Continuation<List<T>> continuation, double error, int iteration) throws Throwable {
                distribute(centers, Continuations.map((res,cont)->{ 
                               cont.completed(new ArrayList<>(res.keySet()));
                           },continuation), error, 0);
    }

    public void distribute(Set<T> centers, Continuation<Map<T,List<T>>> continuation, double error, int iteration) throws Throwable {
        context.checkStopped();
        if (maxIterations<=iteration) {
            Map<T,List<T>> a=new HashMap<>();
            for (T center : centers) {
                a.put(center,new ArrayList<>());
            }
            continuation.completed(a);
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
                                continuation2.completed(voronoi);
                                return;
                            }
                            update_centers(continuation2, error, iteration, voronoi);
                        },
                        continuation),
                context.executor());
    }


    public void lumping(Continuation<Map<T,List<T>>> continuation, double error, int iteration, Map<T, List<T>> voronoi)
            throws Throwable {
         distribute(voronoi.keySet(), continuation, error, iteration+1);
    }

    //goto 8 ~ split
    //goto 11 ~ no split
    //if last iteration goto 8
    //if Nc < K/2 goto 8
    //if iteration is even or Nc >= 2K goto 11
    //else goto 8
    public boolean maySplitCluster(int iteration) {
        if (iteration % 2 == 0) {
           return false;
        }

        if (clusters >= 2*max_clusters) {
           return false;
        }

        return true;
    }

    public void update_centers( Continuation<Map<T,List<T>>> continuation, double error, int iteration, Map<T, List<T>> voronoi) throws Throwable {
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
                            Set<T> centers=new HashSet<>();
                            centers2.forEach(centers::add);
                            distribute(centers, continuation2, error, iteration+1);
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
