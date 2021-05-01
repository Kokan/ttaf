package dog.giraffe;

import java.util.ArrayList;
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
}
