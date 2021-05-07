package dog.giraffe;

import dog.giraffe.points.Vector;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

public class Clusters {
    public final List<List<Vector>> centers;
    public final double error;
    public final Map<String,Object> stats;

    public Clusters(List<List<Vector>> centers, double error, Map<String,Object> stats) {
        this.centers=centers;
        this.error=error;
        this.stats=stats;
    }

    public Clusters(List<List<Vector>> centers, double error) {
        this.centers=centers;
        this.error=error;
        this.stats=new HashMap<>();
    }

    public static Clusters create(List<Vector> centers, double error) {
        return new Clusters(
                Collections.unmodifiableList(centers.stream().map(List::of).collect(Collectors.toList())),
                error, Collections.unmodifiableMap(new HashMap<>()));
    }

    public static Clusters createWithStats(List<Vector> centers, double error, Map<String, Object> stats) {
        return new Clusters(
                Collections.unmodifiableList(centers.stream().map(List::of).collect(Collectors.toList())),
                error,
                Collections.unmodifiableMap(stats));
    }
}
