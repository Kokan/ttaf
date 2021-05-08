package dog.giraffe.points;

import dog.giraffe.util.QuickSort;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A mutable list of vectors.
 */
public abstract class MutablePoints extends Points implements QuickSort.Swap  {
    /**
     * An interval of indices.
     */
    public static class Interval {
        public final int from;
        public final int to;

        /**
         * Creates a new instance for the interval [from, to).
         */
        public Interval(int from, int to) {
            this.from=from;
            this.to=to;
        }

        /**
         * Returns whether this interval is empty.
         */
        public boolean isEmpty() {
            return from>=to;
        }

        /**
         * Returns the number of indices in this interval.
         */
        public int length() {
            return to-from;
        }
    }

    /**
     * Creates a new MutablePoints instance with dimensionality dimensions.
     */
    public MutablePoints(int dimensions) {
        super(dimensions);
    }

    /**
     * Adds vector to this.
     */
    public abstract void add(Vector vector);

    /**
     * Removes all the vectors from this which are not contained in any of the intervals.
     * This is done by moving all the intervals to the start of this then setting the new size of this to
     * to the sum of the length of all of the intervals.
     *
     * @param intervals disjunct intervals of this containing valid vectors
     */
    public void compact(List<Interval> intervals) {
        ArrayDeque<Interval> queue=new ArrayDeque<>(intervals.size());
        queue.addAll(intervals.stream()
                .filter((ii)->!ii.isEmpty())
                .sorted(Comparator.comparingInt((ii)->ii.from))
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
                    queue.addLast(new Interval(ii.from, ii.to-free));
                }
            }
        }
        size(size);
    }

    /**
     * Copies the interval of vectors [from, from+length) to [to, to+length).
     * The intervals must not overlap.
     */
    protected void copy(int from, int to, int length) {
        for (; 0<length; --length, ++from, ++to) {
            for (int dd=0; dimensions>dd; ++dd) {
                set(dd, to, get(dd, from));
            }
        }
    }

    /**
     * Sets the dimension-th coordinate of the vector indexed by index to value.
     */
    public void set(int dimension, int index, byte value) {
        set(dimension, index, value&0xff);
    }

    /**
     * Sets the dimension-th coordinate of the vector indexed by index to value.
     */
    public void set(int dimension, int index, short value) {
        set(dimension, index, value&0xffff);
    }

    /**
     * Sets the dimension-th coordinate of the vector indexed by index to value.
     */
    public abstract void set(int dimension, int index, double value);

    /**
     * Sets the dimension-th coordinate of the vector indexed by index to value.
     * All values are considered to be between 0 and 1 regardless of the internal representation.
     */
    public abstract void setNormalized(int dimension, int index, double value);

    /**
     * Copies the vectors [fromOffset, fromOffset+length) of from
     * to [toOffset, toOffset+length) of this.
     */
    public void setNormalizedFrom(FloatArrayPoints from, int fromOffset, int length, int toOffset) {
        setNormalizedFrom((MutablePoints)from, fromOffset, length, toOffset);
    }

    /**
     * Copies the vectors [fromOffset, fromOffset+length) of from
     * to [toOffset, toOffset+length) of this.
     */
    public void setNormalizedFrom(MutablePoints from, int fromOffset, int length, int toOffset) {
        for (; 0<length; ++fromOffset, --length, ++toOffset) {
            for (int dd=0; dimensions>dd; ++dd) {
                setNormalized(dd, toOffset, from.getNormalized(dd, fromOffset));
            }
        }
    }

    /**
     * Copies the vectors [fromOffset, fromOffset+length) of from
     * to [toOffset, toOffset+length) of this.
     */
    public void setNormalizedFrom(UnsignedByteArrayPoints from, int fromOffset, int length, int toOffset) {
        setNormalizedFrom((MutablePoints)from, fromOffset, length, toOffset);
    }

    /**
     * Copies the vectors [fromOffset, fromOffset+length) of from
     * to [toOffset, toOffset+length) of this.
     */
    public void setNormalizedFrom(UnsignedShortArrayPoints from, int fromOffset, int length, int toOffset) {
        setNormalizedFrom((MutablePoints)from, fromOffset, length, toOffset);
    }

    /**
     * Copies the vectors [fromOffset, fromOffset+length) of this
     * to [toOffset, toOffset+length) of to.
     */
    public void setNormalizedTo(int fromOffset, int length, MutablePoints to, int toOffset) {
        for (; 0<length; ++fromOffset, --length, ++toOffset) {
            for (int dd=0; dimensions>dd; ++dd) {
                to.setNormalized(dd, toOffset, getNormalized(dd, fromOffset));
            }
        }
    }

    /**
     * Sets the size of this to size.
     * If the new size is smaller than the old size then vectors are deleted from this.
     * If the new size is larger than the old size then vectors are created to this with undefined coordinate values.
     */
    public abstract void size(int size);

    /**
     * Creates a MutablePoints that has the same vectors as this in the interval [fromIndex, toIndex).
     * The only mutator method sub-points have to support is swap().
     */
    public abstract MutablePoints subPoints(int fromIndex, int toIndex);
}
