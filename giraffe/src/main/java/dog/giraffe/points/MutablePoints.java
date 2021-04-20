package dog.giraffe.points;

import dog.giraffe.QuickSort;

public abstract class MutablePoints extends Points implements QuickSort.Swap  {
    public interface Factory {
        MutablePoints create(int expectedSize);
    }

    public MutablePoints(int dimensions) {
        super(dimensions);
    }

    public abstract void add(Vector vector);

    public void addFrom(FloatArrayPoints points, int from, int to) {
        addFrom((Points)points, from, to);
    }

    public void addFrom(Points points, int from, int to) {
        Vector vector=new Vector(dimensions);
        for (; to>from; ++from) {
            for (int dd=0; dimensions>dd; ++dd) {
                vector.coordinate(dd, points.get(dd, from));
            }
            add(vector);
        }
    }

    public void addFrom(UnsignedByteArrayPoints points, int from, int to) {
        addFrom((Points)points, from, to);
    }

    public void addFrom(UnsignedShortArrayPoints points, int from, int to) {
        addFrom((Points)points, from, to);
    }

    public abstract void addNormalized(Vector vector);

    public abstract void addTo(MutablePoints points, int from, int to);

    public void clear() {
        clear(0);
    }

    public abstract void clear(int size);

    public void set(int dimension, int index, byte value) {
        set(dimension, index, value&0xff);
    }

    public void set(int dimension, int index, short value) {
        set(dimension, index, value&0xffff);
    }

    public abstract void set(int dimension, int index, double value);

    public abstract void set(int index, Vector vector);

    public abstract void setNormalized(int dimension, int index, double value);

    public abstract void size(int size);

    /**
     * The only mutator method sub-points have to support is swap().
     */
    public abstract MutablePoints subPoints(int fromIndex, int toIndex);
}
