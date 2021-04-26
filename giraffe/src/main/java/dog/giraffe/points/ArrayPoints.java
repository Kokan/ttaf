package dog.giraffe.points;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class ArrayPoints<T> extends MutablePoints {
    T data;
    final int offset;
    int size;

    public ArrayPoints(T data, int dimensions, int offset, int size) {
        super(dimensions);
        this.data=data;
        this.offset=offset;
        this.size=size;
    }

    protected abstract void ensureSize(int newSize);

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
        List<Points> result=new ArrayList<>(parts);
        for (int ii=0; parts>ii; ++ii) {
            result.add(subPoints(ii*size/parts, (ii+1)*size/parts));
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public void swap(int index0, int index1) {
        index0=dimensions*(offset+index0);
        index1=dimensions*(offset+index1);
        for (int dd=dimensions; 0<dd; --dd, ++index0, ++index1) {
            swapImpl(index0, index1);
        }
    }

    protected abstract void swapImpl(int index0, int index1);
}
