package dog.giraffe.points;

import dog.giraffe.QuickSort;

public abstract class MutablePoints<P extends MutablePoints<P>> extends Points<P> implements QuickSort.Swap  {
    public interface Factory<P extends MutablePoints<P>> {
        P create(int expectedSize);
    }

    public MutablePoints(int dimensions) {
        super(dimensions);
    }

    public abstract void add(Vector vector);

    public abstract void addNormalized(Vector vector);

    public abstract void addTo(P points, int from, int to);

    public void clear() {
        clear(0);
    }

    public abstract void clear(int size);

    public abstract void set(int dimension, int index, double value);

    public abstract void setNormalized(int dimension, int index, double value);

    /**
     * The only mutator method sub-points have to support is swap().
     */
    public abstract P subPoints(int fromIndex, int toIndex);
}
