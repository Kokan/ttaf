package dog.giraffe.points;

import java.util.Arrays;

/**
 * A {@link MutablePoints} backed by a byte array.
 * Coordinate values are considered to be between 0 and 255.
 * Sub-points are only mutable as far as swap goes.
 */
public class UnsignedByteArrayPoints extends ArrayPoints<byte[]> {
    /**
     * Creates a new instance with the array data, dimensionality dimensions, size size, starting at offset.
     */
    public UnsignedByteArrayPoints(byte[] data, int dimensions, int offset, int size) {
        super(data, dimensions, offset, size);
    }

    /**
     * Creates a new instance with the array data, dimensionality dimensions, full size, starting at offset 0.
     */
    public UnsignedByteArrayPoints(byte[] data, int dimensions) {
        this(data, dimensions, 0, data.length/dimensions);
    }

    /**
     * Creates a new instance with dimensionality dimensions and pre-allocated size of expectedSize.
     */
    public UnsignedByteArrayPoints(int dimensions, int expectedSize) {
        this(new byte[dimensions*expectedSize], dimensions, 0, 0);
    }

    @Override
    public void add(Vector vector) {
        ensureSize(size+1);
        for (int dd=0; dimensions>dd; ++dd) {
            data[dimensions*size+dd]=(byte)vector.coordinate(dd);
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

    /**
     * Converts the value in the range [0, 1] to the range of a byte.
     */
    public static byte denormalize(double value) {
        return (byte)Math.max(0L, Math.min(255L, Math.round(255.0*value)));
    }

    @Override
    protected void ensureSize(int newSize) {
        if (dimensions*newSize>data.length) {
            data=Arrays.copyOf(data, Math.max(dimensions*newSize, 2*data.length));
        }
    }

    @Override
    public double get(int dimension, int index) {
        return data[dimensions*(offset+index)+dimension]&0xff;
    }

    @Override
    public double getNormalized(int dimension, int index) {
        return get(dimension, index)/255.0;
    }

    @Override
    public double maxValue() {
        return 255.0;
    }

    @Override
    public double minValue() {
        return 0.0;
    }

    @Override
    public void set(int dimension, int index, byte value) {
        data[dimension+dimensions*index]=value;
    }

    @Override
    public void set(int dimension, int index, double value) {
        data[dimension+dimensions*index]=(byte)value;
    }

    @Override
    public void setNormalized(int dimension, int index, double value) {
        data[dimension+dimensions*index]=denormalize(value);
    }

    @Override
    public void setNormalizedFrom(UnsignedByteArrayPoints from, int fromOffset, int length, int toOffset) {
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
    public UnsignedByteArrayPoints subPoints(int fromIndex, int toIndex) {
        return new UnsignedByteArrayPoints(data, dimensions, offset+fromIndex, toIndex-fromIndex);
    }

    @Override
    protected void swapImpl(int index0, int index1) {
        byte temp=data[index0];
        data[index0]=data[index1];
        data[index1]=temp;
    }
}
