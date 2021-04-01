package dog.giraffe.points;

import dog.giraffe.QuickSort;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ByteArrayL2Points
        extends L2Points<ByteArrayL2Points> implements QuickSort.Swap, SubPoints<ByteArrayL2Points> {
    private final byte[] data;
    private final int offset;
    private final int size;

    public static class Builder {
        private byte[] data;
        private final int dimensions;
        private int size;

        public Builder(int dimensions, int expectedSize) {
            this.dimensions=dimensions;
            data=new byte[dimensions*Math.max(0, expectedSize)];
        }

        public void add(byte coordinate0) {
            ensureSize();
            data[dimensions*size]=coordinate0;
            size+=dimensions;
        }

        public void add(byte coordinate0, byte coordinate1) {
            ensureSize();
            data[dimensions*size]=coordinate0;
            data[dimensions*size+1]=coordinate1;
            size+=dimensions;
        }

        public void add(byte coordinate0, byte coordinate1, byte coordinate2) {
            ensureSize();
            data[dimensions*size]=coordinate0;
            data[dimensions*size+1]=coordinate1;
            data[dimensions*size+2]=coordinate2;
            size+=dimensions;
        }

        public void add(byte[] vector) {
            ensureSize();
            System.arraycopy(vector, 0, data, dimensions*size, dimensions);
            size+=dimensions;
        }

        public ByteArrayL2Points create() {
            return new ByteArrayL2Points(data, dimensions, 0, size);
        }

        private void ensureSize() {
            if (dimensions*(size+1)>data.length) {
                data=Arrays.copyOf(data, Math.max(dimensions*(size+1), 2*data.length));
            }
        }
    }

    public ByteArrayL2Points(byte[] data, int dimensions, int offset, int size) {
        super(dimensions);
        this.data=data;
        this.offset=offset;
        this.size=size;
    }

    public ByteArrayL2Points(byte[] data, int dimensions) {
        this(data, dimensions, 0, data.length/dimensions);
    }

    @Override
    public double get(int dimension, int index) {
        return data[dimensions*(offset+index)+dimension]&0xff;
    }

    @Override
    public ByteArrayL2Points self() {
        return this;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public List<ByteArrayL2Points> split(int parts) {
        if ((2>parts)
                || (2>size())) {
            return Collections.singletonList(this);
        }
        parts=Math.min(parts, size());
        List<ByteArrayL2Points> result=new ArrayList<>(parts);
        for (int ii=0; parts>ii; ++ii) {
            int from=ii*size/parts;
            int to=(ii+1)*size/parts;
            result.add(new ByteArrayL2Points(
                    data,
                    dimensions,
                    offset+from,
                    to-from));
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public ByteArrayL2Points subPoints(int fromIndex, int toIndex) {
        return new ByteArrayL2Points(data, dimensions, offset+fromIndex, toIndex-fromIndex);
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
