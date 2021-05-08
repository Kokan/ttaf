package dog.giraffe.util;

import java.util.Objects;

/**
 * Immutable container of two values. Equality and hash is based on the contained values.
 */
public class Pair<T, U> {
    public final T first;
    public final U second;

    public Pair(T first, U second) {
        this.first=first;
        this.second=second;
    }

    @Override
    public boolean equals(Object obj) {
        if (this==obj) {
            return true;
        }
        if ((null==obj)
                || (!getClass().equals(obj.getClass()))) {
            return false;
        }
        Pair<?, ?> pair=(Pair<?, ?>)obj;
        return Objects.equals(first, pair.first)
                && Objects.equals(second, pair.second);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(first)+19*Objects.hashCode(second);
    }

    @Override
    public String toString() {
        return "("+first+", "+second+")";
    }
}
