package dog.giraffe.util;

import java.util.Comparator;

/**
 * Abstract description of a lexicographic order for list-like objects.
 */
public abstract class LexicographicComparator<T> implements Comparator<T> {
    @Override
    public int compare(T o1, T o2) {
        int length1=length(o1);
        int length2=length(o2);
        int length=Math.min(length1, length2);
        for (int ii=0; length>ii; ++ii) {
            int cc=compare(o1, ii, o2, ii);
            if (0!=cc) {
                return cc;
            }
        }
        return Integer.compare(length1, length2);
    }

    /**
     * Compares elements of two lists. The first element is the one indexed by index1 in object1,
     * the second element is the one indexed by index2 in object2.
     *
     * @return 0 if the elements are equal, less than 0 if the first element comes before the second element,
     *         and greater than zero if the first element comes after the second element
     */
    protected abstract int compare(T object1, int index1, T object2, int index2);

    /**
     * Returns the number of elements in the object.
     */
    protected abstract int length(T object);
}
