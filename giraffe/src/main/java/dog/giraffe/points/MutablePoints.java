package dog.giraffe.points;

import dog.giraffe.QuickSort;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public abstract class MutablePoints extends Points implements QuickSort.Swap  {
    public static class Interval {
        public int from;
        public int to;

        public Interval(int from, int to) {
            this.from=from;
            this.to=to;
        }

        public Interval copy() {
            return new Interval(from, to);
        }

        public boolean isEmpty() {
            return from>=to;
        }

        public int length() {
            return to-from;
        }
    }

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

    public void compact(List<Interval> intervals) {
        ArrayDeque<Interval> queue=new ArrayDeque<>(intervals.size());
        queue.addAll(intervals.stream()
                .filter((ii)->!ii.isEmpty())
                .sorted(Comparator.comparingInt((ii)->ii.from))
                .map(Interval::copy)
                .collect(Collectors.toList()));
        int size=0;
        while (!queue.isEmpty()) {
            Interval ii=queue.removeFirst();
            if (size==ii.from) {
                size=ii.to;
                continue;
            }
            int free=ii.from-size;
            queue.addFirst(ii);
            ii=queue.removeLast();
            if (queue.isEmpty()) {
                if (ii.length()<=free) {
                    copy(ii.from, size, ii.length());
                }
                else {
                    copy(ii.to-free, size, free);
                }
                size+=ii.length();
            }
            else {
                if (ii.length()<=free) {
                    copy(ii.from, size, ii.length());
                    size+=ii.length();
                }
                else {
                    copy(ii.to-free, size, free);
                    size+=free;
                    ii.to-=free;
                    queue.addLast(ii);
                }
            }
        }
        size(size);
    }

    public void copy(int from, int to, int length) {
        for (; 0<length; --length, ++from, ++to) {
            for (int dd=0; dimensions>dd; ++dd) {
                set(dd, to, get(dd, from));
            }
        }
    }

    public void set(int dimension, int index, byte value) {
        set(dimension, index, value&0xff);
    }

    public void set(int dimension, int index, short value) {
        set(dimension, index, value&0xffff);
    }

    public abstract void set(int dimension, int index, double value);

    public abstract void set(int index, Vector vector);

    public abstract void setNormalized(int dimension, int index, double value);

    public void setNormalizedFrom(FloatArrayPoints from, int fromOffset, int length, int toOffset) {
        setNormalizedFrom((MutablePoints)from, fromOffset, length, toOffset);
    }

    public void setNormalizedFrom(MutablePoints from, int fromOffset, int length, int toOffset) {
        for (; 0<length; ++fromOffset, --length, ++toOffset) {
            for (int dd=0; dimensions>dd; ++dd) {
                setNormalized(dd, toOffset, from.getNormalized(dd, fromOffset));
            }
        }
    }

    public void setNormalizedFrom(UnsignedByteArrayPoints from, int fromOffset, int length, int toOffset) {
        setNormalizedFrom((MutablePoints)from, fromOffset, length, toOffset);
    }

    public void setNormalizedFrom(UnsignedShortArrayPoints from, int fromOffset, int length, int toOffset) {
        setNormalizedFrom((MutablePoints)from, fromOffset, length, toOffset);
    }

    public void setNormalizedTo(int fromOffset, int length, MutablePoints to, int toOffset) {
        for (; 0<length; ++fromOffset, --length, ++toOffset) {
            for (int dd=0; dimensions>dd; ++dd) {
                to.setNormalized(dd, toOffset, getNormalized(dd, fromOffset));
            }
        }
    }

    public abstract void size(int size);

    /**
     * The only mutator method sub-points have to support is swap().
     */
    public abstract MutablePoints subPoints(int fromIndex, int toIndex);
}
