package dog.giraffe.kmeans;

import dog.giraffe.Context;
import dog.giraffe.Distance;
import dog.giraffe.VectorMean;
import dog.giraffe.points.Points;
import dog.giraffe.threads.AsyncSupplier;
import dog.giraffe.threads.Continuation;
import dog.giraffe.threads.Continuations;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@FunctionalInterface
public interface ReplaceEmptyCluster
        <D extends Distance<T>, M extends VectorMean<M, T>, P extends Points<D, M, P, T>, T> {
    static <D extends Distance<T>, M extends VectorMean<M, T>, P extends Points<D, M, P, T>, T>
    ReplaceEmptyCluster<D, M, P, T> error() {
        return (centers, context, maxIterations, points, points2, continuation)->
                continuation.failed(new EmptyClusterException());
    }

    void newCenter(
            List<T> centers, Context context, int maxIterations, P points, List<P> points2,
            Continuation<T> continuation) throws Throwable;

    static <D extends Distance<T>, M extends VectorMean<M, T>, P extends Points<D, M, P, T>, T>
    ReplaceEmptyCluster<D, M, P, T> notNear() {
        return (centers, context, maxIterations, points, points2, continuation)->{
            class Candidate {
                public final double distance;
                public final T point;

                public Candidate(double distance, T point) {
                    this.distance=distance;
                    this.point=point;
                }
            }
            List<AsyncSupplier<Candidate>> forks=new ArrayList<>(points2.size());
            for (P points3: points2) {
                forks.add(new AsyncSupplier<Candidate>() {
                    private double bestDistance;
                    private T bestPoint;

                    @Override
                    public void get(Continuation<? super Candidate> continuation) throws Throwable {
                        points3.classify(
                                Function.identity(),
                                centers,
                                new Points.Classification<T, D, M, P, T>() {
                                    @Override
                                    public void nearestCenter(T center, P points) {
                                        for (int ii=0; points.size()>ii; ++ii) {
                                            nearestCenter(center, points, ii);
                                        }
                                    }

                                    @Override
                                    public void nearestCenter(T center, P points, int index) {
                                        double dd=points.distance(center, index);
                                        if (dd>bestDistance) {
                                            bestDistance=dd;
                                            bestPoint=points.get(index);
                                        }
                                    }
                                });
                        continuation.completed(
                                (0.0>=bestDistance)
                                        ?null
                                        :new Candidate(bestDistance, bestPoint));
                    }
                });
            }
            Continuation<List<Candidate>> join=Continuations.map(
                    (candidates, continuation2)->{
                        T bc=null;
                        double bd=0.0;
                        for (Candidate candidate: candidates) {
                            if (bd<candidate.distance) {
                                bc=candidate.point;
                                bd=candidate.distance;
                            }
                        }
                        if (0.0>=bd) {
                            continuation.failed(new EmptyClusterException());
                        }
                        else {
                            continuation.completed(bc);
                        }
                    },
                    continuation);
            Continuations.forkJoin(forks, join, context.executor());
        };
    }

    static <D extends Distance<T>, M extends VectorMean<M, T>, P extends Points<D, M, P, T>, T>
    ReplaceEmptyCluster<D, M, P, T> random() {
        return (centers, context, maxIterations, points, points2, continuation)->{
            for (int ii=maxIterations; 0<ii; --ii) {
                T point=points.get(context.random().nextInt(points.size()));
                if (!centers.contains(point)) {
                    continuation.completed(point);
                    return;
                }
            }
            continuation.failed(new EmptyClusterException());
        };
    }
}
