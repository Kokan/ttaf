package dog.giraffe;

import dog.giraffe.kmeans.EmptyClusterException;
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
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;

@FunctionalInterface
public interface ClusteringStrategy
        <D extends Distance<T>, M extends VectorMean<M, T>, P extends Points<D, M, P, T>, T> {
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
            class ClustersOrEmpty {
                public final Clusters<T> clusters;
                public final EmptyClusterException empty;

                public ClustersOrEmpty(Clusters<T> clusters) {
                    this.clusters=Objects.requireNonNull(clusters, "clusters");
                    empty=null;
                }

                public ClustersOrEmpty(EmptyClusterException empty) {
                    this.empty=Objects.requireNonNull(empty, "empty");
                    clusters=null;
                }
            }
            ParallelSearch.search(
                    (clusters, continuation2)->strategy.apply(clusters).cluster(
                            context,
                            points,
                            new Continuation<Clusters<T>>() {
                                @Override
                                public void completed(Clusters<T> result) throws Throwable {
                                    continuation2.completed(new ClustersOrEmpty(result));
                                }

                                @Override
                                public void failed(Throwable throwable) throws Throwable {
                                    if (throwable instanceof EmptyClusterException) {
                                        continuation2.completed(new ClustersOrEmpty((EmptyClusterException)throwable));
                                    }
                                    else {
                                        continuation2.failed(throwable);
                                    }
                                }
                            }),
                    minClusters,
                    maxClusters+1,
                    new ParallelSearch<ClustersOrEmpty, Clusters<T>>() {
                        private final NavigableMap<Integer, ClustersOrEmpty> clusters=new TreeMap<>();
                        private int index;
                        private Clusters<T> selected;

                        @Override
                        public void search(
                                Map<Integer, ClustersOrEmpty> newElements, Block continueSearch,
                                Continuation<Clusters<T>> continuation) throws Throwable {
                            clusters.putAll(newElements);
                            while (true) {
                                if (null==selected) {
                                    ClustersOrEmpty next=clusters.remove(minClusters);
                                    if (null==next) {
                                        continueSearch.run();
                                        return;
                                    }
                                    else if (null==next.clusters) {
                                        continuation.failed(new EmptyClusterException(next.empty));
                                        return;
                                    }
                                    else {
                                        selected=next.clusters;
                                        index=minClusters;
                                    }
                                }
                                else if (maxClusters<=index) {
                                    continuation.completed(selected);
                                    return;
                                }
                                else {
                                    ClustersOrEmpty next=clusters.remove(index+1);
                                    if (null==next) {
                                        continueSearch.run();
                                        return;
                                    }
                                    else if (null==next.clusters) {
                                        continuation.completed(selected);
                                        return;
                                    }
                                    else if (selected.error*errorLimit>next.clusters.error) {
                                        selected=next.clusters;
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
