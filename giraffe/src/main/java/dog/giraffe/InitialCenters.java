package dog.giraffe;

import dog.giraffe.Context;
import dog.giraffe.Distance;
import dog.giraffe.VectorMean;
import dog.giraffe.points.Points;
import dog.giraffe.threads.AsyncSupplier;
import dog.giraffe.threads.Continuation;
import dog.giraffe.threads.Continuations;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@FunctionalInterface
public interface InitialCenters<D extends Distance<T>, M extends VectorMean<M, T>, S extends VectorStdDeviation<S, T>, P extends Points<D, M, S, P, T>, T> {
    void initialCenters(
            int clusters, Context context, int maxIterations, P points, List<P> points2,
            Continuation<List<T>> continuation) throws Throwable;

    static <D extends Distance<T>, M extends VectorMean<M, T>, S extends VectorStdDeviation<S, T>, P extends Points<D, M, S, P, T>, T>
    InitialCenters<D, M, S, P, T> meanAndFarthest(boolean notNear) {
        ReplaceEmptyCluster<D, M, S, P, T> replaceEmptyClustersFirst
                =(centers, context, maxIterations, points, points2, continuation)->{
                    List<AsyncSupplier<M>> forks=new ArrayList<>(points2.size());
                    for (int pp=0; points2.size()>pp; ++pp) {
                        int expected=((0==pp)?points:(points2.get(pp))).size();
                        P points3=points2.get(pp);
                        forks.add((continuation2)->{
                            M mean=points3.mean().create(expected, context.sum());
                            points3.addAllTo(mean);
                            continuation2.completed(mean);
                        });
                    }
                    Continuation<List<M>> join=Continuations.map(
                            (means, continuation2)->{
                                means=new ArrayList<>(means);
                                M mean=means.get(0);
                                for (int ii=means.size()-1; 0<ii; --ii) {
                                    means.remove(ii).addTo(mean);
                                }
                                continuation2.completed(mean.mean());
                            },
                            continuation);
                    Continuations.forkJoin(forks, join, context.executor());
                };
        ReplaceEmptyCluster<D, M, S, P, T> replaceEmptyClustersRest=ReplaceEmptyCluster.farthest(notNear);
        return (clusters, context, maxIterations, points, points2, continuation)->{
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
        };
    }

    static <D extends Distance<T>, M extends VectorMean<M, T>, S extends VectorStdDeviation<S, T>, P extends Points<D, M, S, P, T>, T>
    void newCenters(
            Set<T> centers, int clusters, Context context, Continuation<List<T>> continuation, int maxIterations,
            P points, List<P> points2, ReplaceEmptyCluster<D, M, S, P, T> replaceEmptyClusterFirst,
            ReplaceEmptyCluster<D, M, S, P, T> replaceEmptyClusterRest) throws Throwable {
        List<T> centers2=Collections.unmodifiableList(new ArrayList<>(centers));
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
                                    Set<T> centers3=new HashSet<>(clusters);
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

    static <D extends Distance<T>, M extends VectorMean<M, T>, S extends VectorStdDeviation<S, T>, P extends Points<D, M, S, P, T>, T>
    InitialCenters<D, M, S, P, T> random() {
        return (clusters, context, maxIterations, points, points2, continuation)->{
            Set<T> centers=new HashSet<>(clusters);
            for (int ii=maxIterations*clusters; clusters>centers.size(); --ii) {
                if (0>=ii) {
                    continuation.failed(new CannotSelectInitialCentersException());
                    return;
                }
                centers.add(points.get(context.random().nextInt(points.size())));
            }
            continuation.completed(Collections.unmodifiableList(new ArrayList<>(centers)));
        };
    }
}
