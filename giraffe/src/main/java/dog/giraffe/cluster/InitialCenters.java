package dog.giraffe.cluster;

import dog.giraffe.Context;
import dog.giraffe.Log;
import dog.giraffe.points.Mean;
import dog.giraffe.points.Points;
import dog.giraffe.points.Vector;
import dog.giraffe.threads.AsyncSupplier;
import dog.giraffe.threads.Continuation;
import dog.giraffe.threads.Continuations;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Selects vectors for the initial cluster centers.
 */
public interface InitialCenters<P extends Points> extends Log {
    /**
     * Selects vectors for the initial cluster centers.
     *
     * @param clusters the number of distinct vector to select
     * @param maxIterations the maximum number of iterations to try to find a centers
     * @param points the input data points
     * @param points2 the result of points.split()
     */
    void initialCenters(
            int clusters, Context context, int maxIterations, P points, List<Points> points2,
            Continuation<List<Vector>> continuation) throws Throwable;

    /**
     * Select the mean of the points for the first center.
     * The rest of the centers are selected by {@link ReplaceEmptyCluster#farthest(boolean)}.
     *
     * @param notNear parameter for {@link ReplaceEmptyCluster#farthest(boolean)}
     */
    static <P extends Points> InitialCenters<P> meanAndFarthest(boolean notNear) {
        ReplaceEmptyCluster<P> replaceEmptyClustersFirst=new ReplaceEmptyCluster<>() {
            @Override
            public void newCenter(
                    List<Vector> centers, Context context, int maxIterations, P points, List<Points> points2,
                    Continuation<Vector> continuation) throws Throwable {
                List<AsyncSupplier<Mean>> forks=new ArrayList<>(points2.size());
                for (int pp=0; points2.size()>pp; ++pp) {
                    int expected=((0==pp)?points:(points2.get(pp))).size();
                    Points points3=points2.get(pp);
                    forks.add((continuation2)->{
                        Mean mean=points3.mean().create(expected, context.sum());
                        points3.addAllTo(mean);
                        continuation2.completed(mean);
                    });
                }
                Continuation<List<Mean>> join=Continuations.map(
                        (means, continuation2)->{
                            means=new ArrayList<>(means);
                            Mean mean=means.get(0);
                            for (int ii=means.size()-1; 0<ii; --ii) {
                                means.remove(ii).addTo(mean);
                            }
                            continuation2.completed(mean.mean());
                        },
                        continuation);
                Continuations.forkJoin(forks, join, context.executor());
            }

            @Override
            public void log(Map<String, Object> log) {
                log.put("type", "mean-and-farthest-1st");
                log.put("not-near", notNear);
            }
        };
        ReplaceEmptyCluster<P> replaceEmptyClustersRest=ReplaceEmptyCluster.farthest(notNear);
        return new InitialCenters<>() {
            @Override
            public void initialCenters(
                    int clusters, Context context, int maxIterations, P points, List<Points> points2,
                    Continuation<List<Vector>> continuation) throws Throwable {
                newCenters(
                        new HashSet<>(0),
                        clusters,
                        context,
                        continuation,
                        maxIterations,
                        points,
                        points2,
                        replaceEmptyClustersFirst,
                        replaceEmptyClustersRest);
            }

            @Override
            public void log(Map<String, Object> log) {
                log.put("type", "mean-and-farthest");
                log.put("not-near", notNear);
            }
        };
    }

    /**
     * Select the first center with replaceEmptyClusterFirst. The rest of the points will be selected by
     * replaceEmptyClusterRest.
     */
    static <P extends Points> void newCenters(
            Set<Vector> centers, int clusters, Context context, Continuation<List<Vector>> continuation,
            int maxIterations, P points, List<Points> points2, ReplaceEmptyCluster<P> replaceEmptyClusterFirst,
            ReplaceEmptyCluster<P> replaceEmptyClusterRest) throws Throwable {
        List<Vector> centers2=List.copyOf(centers);
        if (centers2.size()>=clusters) {
            continuation.completed(centers2);
            return;
        }
        replaceEmptyClusterFirst.newCenter(
                centers2,
                context,
                maxIterations,
                points,
                points2,
                Continuations.async(
                        Continuations.map(
                                (center, continuation2)->{
                                    Set<Vector> centers3=new HashSet<>(clusters);
                                    centers3.addAll(centers);
                                    if (centers3.add(center)) {
                                        newCenters(
                                                centers3, clusters, context, continuation2, maxIterations,
                                                points, points2, replaceEmptyClusterRest, replaceEmptyClusterRest);
                                    }
                                    else {
                                        continuation.failed(new EmptyClusterException());
                                    }
                                },
                                continuation),
                        context.executor()));
    }

    /**
     * Select data points randomly for initial centers.
     * If the selected vectors are not distinct it will try again.
     */
    static <P extends Points> InitialCenters<P> random() {
        return new InitialCenters<>() {
            @Override
            public void initialCenters(
                    int clusters, Context context, int maxIterations, P points, List<Points> points2,
                    Continuation<List<Vector>> continuation) throws Throwable {
                Set<Vector> centers=new HashSet<>(clusters);
                for (int ii=maxIterations*clusters; clusters>centers.size(); --ii) {
                    if (0>=ii) {
                        continuation.failed(new CannotSelectInitialCentersException());
                        return;
                    }
                    centers.add(points.get(context.random().nextInt(points.size())));
                }
                continuation.completed(List.copyOf(centers));
            }

            @Override
            public void log(Map<String, Object> log) {
                log.put("type", "random");
            }
        };
    }
}
