package dog.giraffe;

import dog.giraffe.kmeans.InitialCenters;
import dog.giraffe.kmeans.KMeans;
import dog.giraffe.kmeans.ReplaceEmptyCluster;
import dog.giraffe.points.Points;
import dog.giraffe.threads.AsyncSupplier;
import dog.giraffe.threads.Block;
import dog.giraffe.threads.Continuation;
import dog.giraffe.threads.Continuations;
import dog.giraffe.threads.ParallelSearch;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.Function;

@FunctionalInterface
public interface ClusteringStrategy<D extends Distance<T>, M extends VectorMean<M, T>, P extends Points<D, M, P, T>, T> {
    static <D extends Distance<T>, M extends VectorMean<M, T>, P extends Points<D, M, P, T>, T>
    ClusteringStrategy<D, M, P, T> best(List<ClusteringStrategy<D, M, P, T>> strategies) {
        if (strategies.isEmpty()) {
            throw new IllegalArgumentException("empty strategies");
        }
        if (1==strategies.size()) {
            return strategies.get(0);
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
            Function<Integer, ClusteringStrategy<D, M, P, T>> strategy, int threads) {
        return (context, points, continuation)->{
            ParallelSearch.search(
                    (clusters, continuation2)->strategy.apply(clusters).cluster(context, points, continuation2),
                    minClusters,
                    maxClusters+1,
                    new ParallelSearch<Clusters<T>, Clusters<T>>() {
                        private final NavigableMap<Integer, Clusters<T>> clusters=new TreeMap<>();
                        private int index;
                        private Clusters<T> selected;

                        @Override
                        public void search(
                                Map<Integer, Clusters<T>> newElements, Block continueSearch,
                                Continuation<Clusters<T>> continuation) throws Throwable {
                            clusters.putAll(newElements);
                            while (true) {
                                if (null==selected) {
                                    selected=clusters.remove(minClusters);
                                    if (null==selected) {
                                        continueSearch.run();
                                        return;
                                    }
                                    else {
                                        index=minClusters;
                                    }
                                }
                                else if (maxClusters<=index) {
                                    continuation.completed(selected);
                                    return;
                                }
                                else {
                                    Clusters<T> selected2=clusters.remove(index+1);
                                    if (null==selected2) {
                                        continueSearch.run();
                                        return;
                                    }
                                    else if (selected.error*errorLimit>selected2.error) {
                                        selected=selected2;
                                        ++index;
                                    }
                                    else {
                                        continuation.completed(selected);
                                        return;
                                    }
                                }
                            }
                        }
                    },
                    context.executor(),
                    threads,
                    continuation);
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
