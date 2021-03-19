package dog.giraffe;

import dog.giraffe.kmeans.InitialCenters;
import dog.giraffe.kmeans.KMeans;
import dog.giraffe.kmeans.ReplaceEmptyCluster;
import dog.giraffe.points.Points;
import dog.giraffe.threads.AsyncSupplier;
import dog.giraffe.threads.Continuation;
import dog.giraffe.threads.Continuations;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@FunctionalInterface
public interface ClusteringStrategy<D extends Distance<T>, M extends VectorMean<M, T>, P extends Points<D, M, P, T>, T> {
    static <D extends Distance<T>, M extends VectorMean<M, T>, P extends Points<D, M, P, T>, T>
    ClusteringStrategy<D, M, P, T> best(List<ClusteringStrategy<D, M, P, T>> strategies) {
        if (strategies.isEmpty()) {
            throw new IllegalArgumentException("empty strategies");
        }
        return (context, points, continuation)->{
            List<AsyncSupplier<Clusters<T>>> forks=new ArrayList<>(strategies.size());
            for (ClusteringStrategy<D, M, P, T> strategy: strategies) {
                forks.add((continuation2)->strategy.cluster(context, points, continuation2));
            }
            Continuation<List<Clusters<T>>> join=Continuations.map(
                    (clustersList, continuation2)->{
                        Clusters<T> best=null;
                        for (Clusters<T> clusters: clustersList) {
                            if ((null==best)
                                    || (best.error>clusters.error)) {
                                best=clusters;
                            }
                        }
                        continuation.completed(best);
                    },
                    continuation);
            Continuations.forkJoin(forks, join, context.executor());
        };
    }

    static <D extends Distance<T>, M extends VectorMean<M, T>, P extends Points<D, M, P, T>, T>
    ClusteringStrategy<D, M, P, T> best(int iterations, ClusteringStrategy<D, M, P, T> strategy) {
        List<ClusteringStrategy<D, M, P, T>> strategies=new ArrayList<>(iterations);
        for (; 0<iterations; --iterations) {
            strategies.add(strategy);
        }
        return best(strategies);
    }

    void cluster(Context context, P points, Continuation<Clusters<T>> continuation) throws Throwable;

    static <D extends Distance<T>, M extends VectorMean<M, T>, P extends Points<D, M, P, T>, T>
    ClusteringStrategy<D, M, P, T> elbow(
            double errorLimit, int maxClusters, int minClusters,
            Function<Integer, ClusteringStrategy<D, M, P, T>> strategy) {
        return (context, points, continuation)->{
            class Elbow {
                public void cluster(
                        Clusters<T> lastClusters, Continuation<Clusters<T>> continuation) throws Throwable {
                    if ((null!=lastClusters)
                            && (lastClusters.centers.size()>=maxClusters)) {
                        continuation.completed(lastClusters);
                        return;
                    }
                    strategy.apply((null==lastClusters)?minClusters:(lastClusters.centers.size()+1))
                            .cluster(
                                    context,
                                    points,
                                    Continuations.map(
                                            (clusters, continuation2)->{
                                                if ((null==lastClusters)
                                                        || (lastClusters.error*errorLimit>clusters.error)) {
                                                    cluster(clusters, continuation2);
                                                }
                                                else {
                                                    continuation2.completed(lastClusters);
                                                }
                                            },
                                            continuation));
                }
            }
            new Elbow().cluster(null, continuation);
        };
    }

    static <D extends Distance<T>, M extends VectorMean<M, T>, P extends Points<D, M, P, T>, T>
    ClusteringStrategy<D, M, P, T> kMeans(
            int clusters, double errorLimit, InitialCenters<D, M, P, T> initialCenters, int maxIterations,
            ReplaceEmptyCluster<D, M, P, T> replaceEmptyCluster) {
        return (context, points, continuation)->KMeans.cluster(
                clusters, context, continuation, errorLimit,
                initialCenters, maxIterations, points, replaceEmptyCluster);
    }
}
