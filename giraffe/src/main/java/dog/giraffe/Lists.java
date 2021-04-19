package dog.giraffe;

import java.util.ArrayList;
import java.util.List;

public class Lists {
    private Lists() {
    }

    public static <T> List<T> flatten(List<List<T>> list) {
        List<T> result=new ArrayList<>(list.size());
        list.forEach(result::addAll);
        return result;
    }
}
