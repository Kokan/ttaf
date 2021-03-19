package dog.giraffe.kmeans;

import dog.giraffe.Context;
import dog.giraffe.Distance;
import dog.giraffe.VectorMean;
import dog.giraffe.points.Points;
import dog.giraffe.threads.Continuation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@FunctionalInterface
public interface InitialCenters<D extends Distance<T>, M extends VectorMean<M, T>, P extends Points<D, M, P, T>, T> {
    void initialCenters(
            int clusters, Context context, int maxIterations, P points, List<P> points2,
            Continuation<List<T>> continuation) throws Throwable;

    static <D extends Distance<T>, M extends VectorMean<M, T>, P extends Points<D, M, P, T>, T>
    InitialCenters<D, M, P, T> random() {
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
