package dog.giraffe.points;

import dog.giraffe.QuickSort;
import dog.giraffe.Vector;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VectorList extends L2Points<VectorList> implements QuickSort.Swap {
    private final List<Vector> points;

    public VectorList(List<Vector> points) {
        super(points.get(0).dimensions());
        this.points=points;
    }

    @Override
    public Vector get(int index) {
        return points.get(index);
    }

    @Override
    public double get(int dimension, int index) {
        return points.get(index).coordinate(dimension);
    }

    @Override
    public VectorList self() {
        return this;
    }

    @Override
    public int size() {
        return points.size();
    }

    @Override
    public List<VectorList> split(int parts) {
        if ((2>parts)
                || (2>points.size())) {
            return Collections.singletonList(this);
        }
        parts=Math.min(parts, points.size());
        List<VectorList> result=new ArrayList<>(parts);
        for (int ii=0; parts>ii; ++ii) {
            result.add(new VectorList(
                    points.subList(ii*points.size()/parts, (ii+1)*points.size()/parts)));
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public void swap(int index0, int index1) {
        points.set(index0, points.set(index1, points.get(index0)));
    }
}
