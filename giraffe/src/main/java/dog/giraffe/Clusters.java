package dog.giraffe;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Clusters<T> {
    public final List<List<T>> centers;
    public final double error;

    public Clusters(List<List<T>> centers, double error) {
        this.centers=centers;
        this.error=error;
    }

    public static <T> Clusters<T> create(List<T> centers, double error) {
        return new Clusters<>(
                Collections.unmodifiableList(centers.stream().map(List::of).collect(Collectors.toList())),
                error);
    }
}
