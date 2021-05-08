package dog.giraffe.cluster;

import dog.giraffe.Context;
import dog.giraffe.Log;
import dog.giraffe.points.Points;
import dog.giraffe.points.Vector;
import dog.giraffe.threads.AsyncSupplier;
import dog.giraffe.threads.Continuation;
import dog.giraffe.threads.Continuations;
import dog.giraffe.util.Function;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Method to replace cluster that became empty.
 */
public interface ReplaceEmptyCluster<P extends Points> extends Log {
    /**
     * Selects a farthest data point from the existing centers as the new center.
     * If notNear is true it will select a point that is farthest from the nearest center to it.
     * If notNear is false
     * it will select a point that has the maximal sum of all the distances to all the existing centers.
     */
    static <P extends Points> ReplaceEmptyCluster<P> farthest(boolean notNear) {
        return new ReplaceEmptyCluster<>() {
            @Override
            public void log(Map<String, Object> log) {
                log.put("type", "farthest");
                log.put("not-near", notNear);
            }

            @Override
            public void newCenter(
                    List<Vector> centers, Context context, int maxIterations, P points, List<Points> points2,
                    Continuation<Vector> continuation) throws Throwable {
                class Candidate {
                    public final double distance;
                    public final int index;
                    public final Points points;

                    public Candidate(double distance, int index, Points points) {
                        this.distance=distance;
                        this.index=index;
                        this.points=points;
                    }
                }
                List<AsyncSupplier<Candidate>> forks=new ArrayList<>(points2.size());
                for (Points points3: points2) {
                    forks.add(new AsyncSupplier<>() {
                        private double bestDistance;
                        private int bestIndex;
                        private Points bestPoints;

                        @Override
                        public void get(Continuation<Candidate> continuation2) throws Throwable {
                            if (notNear) {
                                points3.classify(
                                        Function.identity(),
                                        centers,
                                        new Points.Classification<>() {
                                            @Override
                                            public void nearestCenter(Vector center, Points points) {
                                                for (int ii=0; points.size()>ii; ++ii) {
                                                    nearestCenter(center, points, ii);
                                                }
                                            }

                                            @Override
                                            public void nearestCenter(Vector center, Points points, int index) {
                                                double dd=points.distance(center, index);
                                                if (dd>bestDistance) {
                                                    bestDistance=dd;
                                                    bestIndex=index;
                                                    bestPoints=points;
                                                }
                                            }
                                        });
                            }
                            else {
                                points3.forEach((points, index)->{
                                    double dd=0.0;
                                    for (Vector center: centers) {
                                        double d2=points.distance(center, index);
                                        if (0.0>=d2) {
                                            return;
                                        }
                                        dd+=d2;
                                    }
                                    if (dd>bestDistance) {
                                        bestDistance=dd;
                                        bestIndex=index;
                                        bestPoints=points;
                                    }
                                });
                            }
                            continuation2.completed(
                                    (0.0>=bestDistance)
                                            ?null
                                            :new Candidate(bestDistance, bestIndex, bestPoints));
                        }
                    });
                }
                Continuation<List<Candidate>> join=Continuations.map(
                        (candidates, continuation2)->{
                            Candidate bestCandidate=null;
                            double bestDistance=0.0;
                            for (Candidate candidate: candidates) {
                                if ((null!=candidate)
                                        && (bestDistance<candidate.distance)) {
                                    bestCandidate=candidate;
                                    bestDistance=candidate.distance;
                                }
                            }
                            if (0.0>=bestDistance) {
                                continuation2.failed(new EmptyClusterException());
                            }
                            else {
                                continuation2.completed(bestCandidate.points.get(bestCandidate.index));
                            }
                        },
                        continuation);
                Continuations.forkJoin(forks, join, context.executor());
            }
        };
    }

    /**
     * Selects a vector as a replacement center.
     *
     * @param centers existing centers
     * @param maxIterations the maximum number of iterations to try to find a new center
     * @param points the input data points
     * @param points2 the result of points.split()
     */
    void newCenter(
            List<Vector> centers, Context context, int maxIterations, P points, List<Points> points2,
            Continuation<Vector> continuation) throws Throwable;

    /**
     * Select randomly a data point as a new centers. If the randomly selected vector is already in
     * the existing centers it will try again.
     */
    static <P extends Points> ReplaceEmptyCluster<P> random() {
        return new ReplaceEmptyCluster<>() {
            @Override
            public void log(Map<String, Object> log) {
                log.put("type", "random");
            }

            @Override
            public void newCenter(
                    List<Vector> centers, Context context, int maxIterations, P points, List<Points> points2,
                    Continuation<Vector> continuation) throws Throwable {
                for (int ii=maxIterations; 0<ii; --ii) {
                    Vector point=points.get(context.random().nextInt(points.size()));
                    if (!centers.contains(point)) {
                        continuation.completed(point);
                        return;
                    }
                }
                continuation.failed(new EmptyClusterException());
            }
        };
    }
}
