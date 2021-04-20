package dog.giraffe.points;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Sub-points are only mutable as far as swap goes.
 */
public class UnsignedByteArrayPoints extends MutablePoints {
    private byte[] data;
    private final int offset;
    private int size;

    public UnsignedByteArrayPoints(byte[] data, int dimensions, int offset, int size) {
        super(dimensions);
        this.data=data;
        this.offset=offset;
        this.size=size;
    }

    public UnsignedByteArrayPoints(byte[] data, int dimensions) {
        this(data, dimensions, 0, data.length/dimensions);
    }

    public UnsignedByteArrayPoints(int dimensions, int expectedSize) {
        this(new byte[dimensions*expectedSize], dimensions, 0, 0);
    }

    public void add(byte coordinate0) {
        ensureSize(size+1);
        data[dimensions*size]=coordinate0;
        ++size;
    }

    public void add(byte coordinate0, byte coordinate1) {
        ensureSize(size+1);
        data[dimensions*size]=coordinate0;
        data[dimensions*size+1]=coordinate1;
        ++size;
    }

    public void add(byte coordinate0, byte coordinate1, byte coordinate2) {
        ensureSize(size+1);
        data[dimensions*size]=coordinate0;
        data[dimensions*size+1]=coordinate1;
        data[dimensions*size+2]=coordinate2;
        ++size;
    }

    public void add(byte[] vector) {
        ensureSize(size+1);
        System.arraycopy(vector, 0, data, dimensions*size, dimensions);
        ++size;
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
    public void addFrom(UnsignedByteArrayPoints points, int from, int to) {
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
        ensureSize(size+1);
        for (int dd=0; dimensions>dd; ++dd) {
            data[dimensions*size+dd]=denormalize(vector.coordinate(dd));
        }
        ++size;
    }

    @Override
    public void addTo(MutablePoints points, int from, int to) {
        points.addFrom(this, from, to);
    }

    @Override
    public void clear(int size) {
        ensureSize(size);
        this.size=size;
        Arrays.fill(data, 0, dimensions*size, (byte)0);
    }

    public static byte denormalize(double value) {
        return (byte)Math.max(0L, Math.min(255L, Math.round(255.0*value)));
    }

    private void ensureSize(int newSize) {
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
    public void set(int index, Vector vector) {
        for (int dd=0, ii=dimensions*(offset+index); dimensions>dd; ++dd, ++ii) {
            data[ii]=(byte)vector.coordinate(dd);
        }
    }

    @Override
    public void setNormalized(int dimension, int index, double value) {
        data[dimension+dimensions*index]=denormalize(value);
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void size(int size) {
        ensureSize(size);
        this.size=size;
    }

    @Override
    public List<Points> split(int parts) {
        if ((2>parts)
                || (2>size())) {
            return Collections.singletonList(this);
        }
        parts=Math.min(parts, size());
        List<UnsignedByteArrayPoints> result=new ArrayList<>(parts);
        for (int ii=0; parts>ii; ++ii) {
            result.add(subPoints(ii*size/parts, (ii+1)*size/parts));
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public UnsignedByteArrayPoints subPoints(int fromIndex, int toIndex) {
        return new UnsignedByteArrayPoints(data, dimensions, offset+fromIndex, toIndex-fromIndex);
    }

    @Override
    public void swap(int index0, int index1) {
        index0=dimensions*(offset+index0);
        index1=dimensions*(offset+index1);
        for (int dd=dimensions; 0<dd; --dd, ++index0, ++index1) {
            byte temp=data[index0];
            data[index0]=data[index1];
            data[index1]=temp;
        }
    }
}
