package dog.giraffe.points;

import dog.giraffe.Distance;
import dog.giraffe.VectorMean;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class PointsList<D extends Distance<T>, M extends VectorMean<M, T>, T>
        implements Points<D, M, PointsList<D, M, T>, T> {
    private final D distance;
    private final VectorMean.Factory<M, T> mean;
    private final List<T> points;

    private PointsList(D distance, VectorMean.Factory<M, T> mean, List<T> points) {
        this.distance=Objects.requireNonNull(distance, "distance");
        this.mean=Objects.requireNonNull(mean, "mean");
        this.points=Objects.requireNonNull(points, "points");
    }

    public PointsList(D distance, VectorMean.Factory<M, T> mean, Collection<T> points) {
        this(distance, mean, Collections.unmodifiableList(new ArrayList<>(points)));
    }

    @Override
    public <C> void classify(
            Function<C, T> centerPoint, List<C> centers,
            Classification<C, D, M, PointsList<D, M, T>, T> classification) {
        if (centers.isEmpty()) {
            throw new IllegalArgumentException();
        }
        for (int ii=0; points.size()>ii; ++ii) {
            C nc=null;
            double nd=Double.POSITIVE_INFINITY;
            for (C center: centers) {
                double dd=distance.distance(centerPoint.apply(center), points.get(ii));
                if (nd>dd) {
                    nc=center;
                    nd=dd;
                }
            }
            classification.nearestCenter(nc, this, ii);
        }
    }

    @Override
    public D distance() {
        return distance;
    }

    @Override
    public T get(int index) {
        return points.get(index);
    }

    @Override
    public VectorMean.Factory<M, T> mean() {
        return mean;
    }

    @Override
    public PointsList<D, M, T> self() {
        return this;
    }

    @Override
    public int size() {
        return points.size();
    }

    @Override
    public List<PointsList<D, M, T>> split(int parts) {
        if ((2>parts)
                || (2>points.size())) {
            return Collections.singletonList(this);
        }
        parts=Math.min(parts, points.size());
        List<PointsList<D, M, T>> result=new ArrayList<>(parts);
        for (int ii=0; parts>ii; ++ii) {
            result.add(new PointsList<>(
                    distance, mean, points.subList(ii*points.size()/parts, (ii+1)*points.size()/parts)));
        }
        return Collections.unmodifiableList(result);
    }
}
