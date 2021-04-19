package dog.giraffe.points;

import dog.giraffe.Distance;
import dog.giraffe.Sum;
import dog.giraffe.VectorMean;
import dog.giraffe.VectorStdDeviation;
import java.util.List;
import java.util.function.Function;

public interface Points<D extends Distance<T>,
                        M extends VectorMean<M, T>,
                        S extends VectorStdDeviation<S, T>,
                        P extends Points<D, M, S, P, T>,
                        T> {
    interface Classification<C, D extends Distance<T>, M extends VectorMean<M, T>, S extends VectorStdDeviation<S,T>, P extends Points<D, M, S, P, T>, T> {
        void nearestCenter(C center, P points);

        void nearestCenter(C center, P points, int index );
    }

    interface ForEach<D extends Distance<T>, M extends VectorMean<M, T>, S extends VectorStdDeviation<S,T>, P extends Points<D, M, S, P, T>, T> {
        void point(P points, int index );
    }

    default void addAllDistanceTo(T center, Sum sum) {
        for (int ii=0; size()>ii; ++ii) {
            addDistanceTo(center, ii, sum);
        }
    }

    default void addAllTo(M mean) {
        for (int ii=0; size()>ii; ++ii) {
            addTo(ii, mean);
        }
    }

    default void addDistanceTo(T center, int index, Sum sum) {
        distance().addDistanceTo(center, get(index), sum);
    }

    default void addTo(int index, M mean) {
        mean.add(get(index));
    }

    default <C> void classify(
            Function<C, T> centerPoint, List<C> centers, Classification<C, D, M, S, P, T> classification) {
        if (centers.isEmpty()) {
            throw new IllegalArgumentException();
        }
        for (int ii=0; size()>ii; ++ii) {
            C nc=null;
            double nd=Double.POSITIVE_INFINITY;
            for (C center: centers) {
                double dd=distance(centerPoint.apply(center), ii);
                if (nd>dd) {
                    nc=center;
                    nd=dd;
                }
            }
            classification.nearestCenter(nc, self(), ii);
        }
    }

    D distance();
    
    default double distance(T center, int index) {
        return distance().distance(center, get(index));
    }

    T get(int index);

    default void forEach(ForEach<D, M, S, P, T> forEach) {
        for (int ii=0; size()>ii; ++ii) {
            forEach.point(self(), ii);
        }
    }

    VectorMean.Factory<M, T> mean();

    VectorStdDeviation.Factory<S, T> dev();
    
    P self();

    int size();

    List<P> split(int parts);
}
