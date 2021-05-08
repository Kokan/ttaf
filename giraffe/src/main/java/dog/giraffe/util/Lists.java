package dog.giraffe.util;

import dog.giraffe.points.Vector;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Collection of helper methods operating on {@link java.util.List Lists}.
 */
public class Lists {
    private Lists() {
    }

    /**
     * Returns the concatenation of all the lists contained in list.
     */
    public static <T> List<T> flatten(List<List<T>> list) {
        List<T> result=new ArrayList<>(list.size());
        list.forEach(result::addAll);
        return result;
    }

    /**
     * Returns a {@link java.util.Comparator Comparator} that sorts {@link java.util.List Lists}
     * according to the lexicographic order.
     */
    public static <T extends Comparable<? super T>> Comparator<List<T>> lexicographicComparator() {
        return new LexicographicComparator<>() {
            @Override
            protected int compare(List<T> object1, int index1, List<T> object2, int index2) {
                return object1.get(index1).compareTo(object2.get(index2));
            }

            @Override
            protected int length(List<T> object) {
                return object.size();
            }
        };
    }

    /**
     * Converts the array of integers to a {@link java.util.List List} of {@link java.lang.Integer Integers}.
     */
    public static List<Integer> toList(int[] values) {
        List<Integer> list=new ArrayList<>(values.length);
        for (int value: values) {
            list.add(value);
        }
        return Collections.unmodifiableList(list);
    }

    /**
     * Returns a {@link java.util.List List} with all of the coordinates of the vector.
     */
    public static List<Double> toList(Vector vector) {
        List<Double> list=new ArrayList<>(vector.dimensions());
        for (int dd=0; vector.dimensions()>dd; ++dd) {
            list.add(vector.coordinate(dd));
        }
        return Collections.unmodifiableList(list);
    }
}
