package dog.giraffe.points;

import java.util.Arrays;

/**
 * Sub-points are only mutable as far as swap goes.
 */
public class FloatArrayPoints extends ArrayPoints<float[]> {
    public FloatArrayPoints(float[] data, int dimensions, int offset, int size) {
        super(data, dimensions, offset, size);
    }

    public FloatArrayPoints(float[] data, int dimensions) {
        this(data, dimensions, 0, data.length/dimensions);
    }

    public FloatArrayPoints(int dimensions, int expectedSize) {
        this(new float[dimensions*expectedSize], dimensions, 0, 0);
    }

    public void add(float coordinate0) {
        ensureSize(size+1);
        data[dimensions*size]=coordinate0;
        ++size;
    }

    public void add(float coordinate0, float coordinate1) {
        ensureSize(size+1);
        data[dimensions*size]=coordinate0;
        data[dimensions*size+1]=coordinate1;
        ++size;
    }

    public void add(float coordinate0, float coordinate1, float coordinate2) {
        ensureSize(size+1);
        data[dimensions*size]=coordinate0;
        data[dimensions*size+1]=coordinate1;
        data[dimensions*size+2]=coordinate2;
        ++size;
    }

    public void add(float[] vector) {
        ensureSize(size+1);
        System.arraycopy(vector, 0, data, dimensions*size, dimensions);
        ++size;
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
    public void addFrom(FloatArrayPoints points, int from, int to) {
        int length=to-from;
        ensureSize(size+length);
        System.arraycopy(
                points.data, points.dimensions*(points.offset+from),
                data, dimensions*size,
                dimensions*length);
        size+=length;
    }

    @Override
    public void addNormalized(Vector vector) {
        add(vector);
    }

    @Override
    public void addTo(MutablePoints points, int from, int to) {
        points.addFrom(this, from, to);
    }

    @Override
    public void clear(int size) {
        ensureSize(size);
        this.size=size;
        Arrays.fill(data, 0, dimensions*size, 0.0f);
    }

    @Override
    public void copy(int from, int to, int length) {
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
    public void set(int index, Vector vector) {
        for (int dd=0, ii=dimensions*(offset+index); dimensions>dd; ++dd, ++ii) {
            data[ii]=(float)vector.coordinate(dd);
        }
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
