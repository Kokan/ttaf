package dog.giraffe.points;

import java.util.Arrays;

/**
 * A {@link MutablePoints} backed by a float array.
 * Coordinate values are considered to be between 0 and 1.
 * Sub-points are only mutable as far as swap goes.
 */
public class FloatArrayPoints extends ArrayPoints<float[]> {
    /**
     * Creates a new instance with the array data, dimensionality dimensions, size size, starting at offset.
     */
    public FloatArrayPoints(float[] data, int dimensions, int offset, int size) {
        super(data, dimensions, offset, size);
    }

    /**
     * Creates a new instance with dimensionality dimensions and pre-allocated size of expectedSize.
     */
    public FloatArrayPoints(int dimensions, int expectedSize) {
        this(new float[dimensions*expectedSize], dimensions, 0, 0);
    }

    @Override
    public void add(Vector vector) {
        ensureSize(size+1);
        for (int dd=0; dimensions>dd; ++dd) {
            data[dimensions*size+dd]=(float)vector.coordinate(dd);
        }
        ++size;
    }

    @Override
    protected void copy(int from, int to, int length) {
        System.arraycopy(
                data, dimensions*(offset+from),
                data, dimensions*(offset+to),
                dimensions*length);
    }

    @Override
    protected void ensureSize(int newSize) {
        if (dimensions*newSize>data.length) {
            data=Arrays.copyOf(data, Math.max(dimensions*newSize, 2*data.length));
        }
    }

    @Override
    public double get(int dimension, int index) {
        return data[dimensions*(offset+index)+dimension];
    }

    @Override
    public double getNormalized(int dimension, int index) {
        return get(dimension, index);
    }

    @Override
    public double maxValue() {
        return 1.0;
    }

    @Override
    public double minValue() {
        return 0.0;
    }

    @Override
    public void set(int dimension, int index, double value) {
        data[dimension+dimensions*(offset+index)]=(float)value;
    }

    @Override
    public void setNormalized(int dimension, int index, double value) {
        set(dimension, index, value);
    }

    @Override
    public void setNormalizedFrom(FloatArrayPoints from, int fromOffset, int length, int toOffset) {
        System.arraycopy(
                from.data, from.dimensions*(from.offset+fromOffset),
                data, dimensions*(offset+toOffset),
                dimensions*length);
    }

    @Override
    public void setNormalizedTo(int fromOffset, int length, MutablePoints to, int toOffset) {
        to.setNormalizedFrom(this, fromOffset, length, toOffset);
    }

    @Override
    public FloatArrayPoints subPoints(int fromIndex, int toIndex) {
        return new FloatArrayPoints(data, dimensions, offset+fromIndex, toIndex-fromIndex);
    }

    @Override
    protected void swapImpl(int index0, int index1) {
        float temp=data[index0];
        data[index0]=data[index1];
        data[index1]=temp;
    }
}
