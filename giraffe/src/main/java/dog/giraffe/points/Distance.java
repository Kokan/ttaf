package dog.giraffe.points;

import dog.giraffe.util.Function;

/**
 * Helpers methods related to the standard Euclidean distance function.
 */
public class Distance {
    private Distance() {
    }

    /**
     * Return the Euclidean distance between center and point.
     */
    public static double distance(Vector center, Vector point) {
        int dimensions=center.dimensions();
        double distance=0.0;
        for (int dd=0; dimensions>dd; ++dd) {
            double di=center.coordinate(dd)-point.coordinate(dd);
            distance+=di*di;
        }
        return distance;
    }

    /**
     * Returns a {@link Function} which maps points to its nearest vector from centers.
     */
    public static Function<Vector, Vector> nearestCenter(Iterable<Vector> centers) {
        return (point)->nearestCenter(centers, point);
    }

    /**
     * Returns the vector from centers that is nearest to point.
     */
    public static Vector nearestCenter(Iterable<Vector> centers, Vector point) {
        Vector bestCenter=null;
        double bestDistance=Double.POSITIVE_INFINITY;
        for (Vector center: centers) {
            double dd=distance(center, point);
            if (dd<bestDistance) {
                bestCenter=center;
                bestDistance=dd;
            }
        }
        if (null==bestCenter) {
            throw new RuntimeException("cannot select nearest center");
        }
        return bestCenter;
    }
}
