package dog.giraffe;

import dog.giraffe.points.Vector;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Clusters {
    public final List<List<Vector>> centers;
    public final double error;

    public Clusters(List<List<Vector>> centers, double error) {
        this.centers=centers;
        this.error=error;
    }

    public static Clusters create(List<Vector> centers, double error) {
        return new Clusters(
                Collections.unmodifiableList(centers.stream().map(List::of).collect(Collectors.toList())),
                error);
    }
}
