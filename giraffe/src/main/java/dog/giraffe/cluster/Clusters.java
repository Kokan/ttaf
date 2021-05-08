package dog.giraffe.cluster;

import dog.giraffe.points.Vector;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The result of a clustering.
 */
public class Clusters {
    /**
     * The cluster centers. A center may have multiple vectors.
     * A point belongs to the center which contains the vector to which the point is nearest to.
     */
    public final List<List<Vector>> centers;
    /**
     * The error of the clustering. Definition differs between clustering methods.
     */
    public final double error;
    /**
     * Metadata of the clustering.
     */
    public final Map<String, Object> stats;

    /**
     * Creates a new instance.
     */
    public Clusters(List<List<Vector>> centers, double error, Map<String,Object> stats) {
        this.centers=centers;
        this.error=error;
        this.stats=stats;
    }

    /**
     * Creates a new instance.
     */
    public Clusters(List<List<Vector>> centers, double error) {
        this.centers=centers;
        this.error=error;
        this.stats=new HashMap<>();
    }

    /**
     * Creates a new instance by repacking centers.
     */
    public static Clusters create(List<Vector> centers, double error) {
        return new Clusters(
                Collections.unmodifiableList(centers.stream().map(List::of).collect(Collectors.toList())),
                error, Collections.unmodifiableMap(new HashMap<>()));
    }

    /**
     * Creates a new instance by repacking centers.
     */
    public static Clusters createWithStats(List<Vector> centers, double error, Map<String, Object> stats) {
        return new Clusters(
                Collections.unmodifiableList(centers.stream().map(List::of).collect(Collectors.toList())),
                error,
                Collections.unmodifiableMap(stats));
    }
}
