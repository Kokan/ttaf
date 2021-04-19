package dog.giraffe.points;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Sub-points are only mutable as far as swap goes.
 */
public class FloatArrayPoints extends MutablePoints<FloatArrayPoints> {
    private float[] data;
    private final int offset;
    private int size;

    public FloatArrayPoints(float[] data, int dimensions, int offset, int size) {
        super(dimensions);
        this.data=data;
        this.offset=offset;
        this.size=size;
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
    public void addNormalized(Vector vector) {
        add(vector);
    }

    @Override
    public void addTo(FloatArrayPoints points, int from, int to) {
        int length=to-from;
        points.ensureSize(points.size+length);
        System.arraycopy(
                data, dimensions*(offset+from),
                points.data, dimensions*points.size,
                dimensions*length);
        points.size+=length;
    }

    @Override
    public void clear(int size) {
        ensureSize(size);
        this.size=size;
        Arrays.fill(data, 0, dimensions*size, 0.0f);
    }

    private void ensureSize(int newSize) {
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
    public FloatArrayPoints self() {
        return this;
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
    public int size() {
        return size;
    }

    @Override
    public List<FloatArrayPoints> split(int parts) {
        if ((2>parts)
                || (2>size())) {
            return Collections.singletonList(this);
        }
        parts=Math.min(parts, size());
        List<FloatArrayPoints> result=new ArrayList<>(parts);
        for (int ii=0; parts>ii; ++ii) {
            result.add(subPoints(ii*size/parts, (ii+1)*size/parts));
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public FloatArrayPoints subPoints(int fromIndex, int toIndex) {
        return new FloatArrayPoints(data, dimensions, offset+fromIndex, toIndex-fromIndex);
    }

    @Override
    public void swap(int index0, int index1) {
        index0=dimensions*(offset+index0);
        index1=dimensions*(offset+index1);
        for (int dd=dimensions; 0<dd; --dd, ++index0, ++index1) {
            float temp=data[index0];
            data[index0]=data[index1];
            data[index1]=temp;
        }
    }
}
