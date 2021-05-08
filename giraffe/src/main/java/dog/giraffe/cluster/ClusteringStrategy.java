package dog.giraffe.cluster;

import dog.giraffe.Context;
import dog.giraffe.Log;
import dog.giraffe.points.Points;
import dog.giraffe.threads.AsyncSupplier;
import dog.giraffe.threads.Continuation;
import dog.giraffe.threads.Continuations;
import dog.giraffe.threads.ParallelSearch;
import dog.giraffe.util.Block;
import dog.giraffe.util.Function;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

/**
 * A method for clustering vectors. All possible method specific parameters are captured by instances.
 */
public interface ClusteringStrategy<P extends Points> extends Log {
    /**
     * Executes all the strategies and selects the result with the smallest error.
     * All the strategies will be run in parallel.
     */
    static <P extends Points> ClusteringStrategy<P> best(List<ClusteringStrategy<P>> strategies) {
        if (strategies.isEmpty()) {
            throw new IllegalArgumentException("empty strategies");
        }
        if (1==strategies.size()) {
            return strategies.get(0);
        }
        return new ClusteringStrategy<>() {
            @Override
            public void cluster(Context context, P points, Continuation<Clusters> continuation) throws Throwable {
                List<AsyncSupplier<Clusters>> forks=new ArrayList<>(strategies.size());
                for (ClusteringStrategy<P> strategy: strategies) {
                    forks.add((continuation2)->{
                        context.checkStopped();
                        strategy.cluster(context, points, continuation2);
                    });
                }
                Continuation<List<Clusters>> join=Continuations.map(
                        (clustersList, continuation2)->{
                            Clusters best=null;
                            for (Clusters clusters: clustersList) {
                                if ((null==best)
                                        || (best.error>clusters.error)) {
                                    best=clusters;
                                }
                            }
                            continuation.completed(best);
                        },
                        continuation);
                Continuations.forkJoin(forks, join, context.executor());
            }

            @Override
            public void log(Map<String, Object> log) throws Throwable {
                log.put("type", "best");
                for (int ii=0; strategies.size()>ii; ++ii) {
                    Log.logField(String.format("strategy%1$02d", ii), strategies.get(ii), log);
                }
            }
        };
    }

    /**
     * Cluster the specified points.
     */
    void cluster(Context context, P points, Continuation<Clusters> continuation) throws Throwable;

    /**
     * Runs all the strategies in the interval [minClusters, maxClusters] and selects the one
     * where the error stops to decrease significantly.
     * More specifically it returns the result for which errorLimit &lt;= next.error / result.error.
     *
     * @param threads the maximum number of threads used to run strategies
     */
    static <P extends Points> ClusteringStrategy<P> elbow(
            double errorLimit, int maxClusters, int minClusters,
            Function<Integer, ClusteringStrategy<P>> strategy, int threads) {
        return new ClusteringStrategy<>() {
            @Override
            public void cluster(Context context, P points, Continuation<Clusters> continuation) throws Throwable {
                class ClustersOrEmpty {
                    public final Clusters clusters;
                    public final EmptyClusterException empty;

                    public ClustersOrEmpty(Clusters clusters) {
                        this.clusters=Objects.requireNonNull(clusters, "clusters");
                        empty=null;
                    }

                    public ClustersOrEmpty(EmptyClusterException empty) {
                        this.empty=Objects.requireNonNull(empty, "empty");
                        clusters=null;
                    }
                }
                ParallelSearch.search(
                        (clusters, continuation2)->{
                            Continuation<Clusters> continuation3=new Continuation<>() {
                                @Override
                                public void completed(Clusters result) throws Throwable {
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
                            };
                            try {
                                strategy.apply(clusters).cluster(context, points, continuation3);
                            }
                            catch (Throwable throwable) {
                                continuation3.failed(throwable);
                            }
                        },
                        minClusters,
                        maxClusters+1,
                        new ParallelSearch<ClustersOrEmpty, Clusters>() {
                            private final NavigableMap<Integer, ClustersOrEmpty> clusters=new TreeMap<>();
                            private int index;
                            private Clusters selected;

                            @Override
                            public void search(
                                    Map<Integer, ClustersOrEmpty> newElements, Block continueSearch,
                                    Continuation<Clusters> continuation) throws Throwable {
                                context.checkStopped();
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
            }

            @Override
            public void log(Map<String, Object> log) throws Throwable {
                log.put("type", "elbow");
                log.put("error-limit", errorLimit);
                log.put("max-clusters", maxClusters);
                log.put("min-clusters", minClusters);
                Log.logField("strategy", strategy.apply(minClusters), log);
            }
        };
    }
}
