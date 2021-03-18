package dog.giraffe;

import java.util.function.Function;

public interface Distance<T> {
    default void addDistanceTo(T center, T point, Sum sum) {
        sum.add(distance(center, point));
    }

    double distance(T center, T point);

    static <T> Function<T, T> nearestCenter(Iterable<T> centers, Distance<T> distance) {
        return (point)->nearestCenter(centers, distance, point);
    }

    static <T> T nearestCenter(Iterable<T> centers, Distance<T> distance, T point) {
        T bestCenter=null;
        double bestDistance=Double.POSITIVE_INFINITY;
        for (T center: centers) {
            double dd=distance.distance(center, point);
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
