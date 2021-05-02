package dog.giraffe;

import dog.giraffe.points.Vector;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Lists {
    private Lists() {
    }

    public static <T> List<T> flatten(List<List<T>> list) {
        List<T> result=new ArrayList<>(list.size());
        list.forEach(result::addAll);
        return result;
    }

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

    public static List<Double> toList(double[] values) {
        List<Double> list=new ArrayList<>(values.length);
        for (double value: values) {
            list.add(value);
        }
        return Collections.unmodifiableList(list);
    }

    public static List<Integer> toList(int[] values) {
        List<Integer> list=new ArrayList<>(values.length);
        for (int value: values) {
            list.add(value);
        }
        return Collections.unmodifiableList(list);
    }

    public static List<Double> toList(Vector vector) {
        List<Double> list=new ArrayList<>(vector.dimensions());
        for (int dd=0; vector.dimensions()>dd; ++dd) {
            list.add(vector.coordinate(dd));
        }
        return Collections.unmodifiableList(list);
    }
}
