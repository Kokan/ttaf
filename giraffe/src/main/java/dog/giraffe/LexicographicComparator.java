package dog.giraffe;

import java.util.Comparator;

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

    protected abstract int compare(T object1, int index1, T object2, int index2);

    protected abstract int length(T object);
}
