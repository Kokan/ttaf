package dog.giraffe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

public class KMeans {
    public interface Distance<T> {
        double distance(T center, T point);
    }

    public static <T> List<T> cluster(
            int clusters, Distance<T> distance, double errorLimit, int maxIterations,
            VectorMean.Factory<T> meanFactory, Random random, Sum.Factory sumFactory, Iterable<T> values) {
        if (2>clusters) {
            throw new IllegalStateException(Integer.toString(clusters));
        }
        Objects.requireNonNull(values);
        List<T> points=new ArrayList<>();
        for (T value: values) {
            points.add(value);
        }
        if (points.size()<=clusters) {
            throw new RuntimeException("too few data points");
        }
        Set<T> centers=new HashSet<>(clusters);
        for (int ii=maxIterations*clusters; clusters>centers.size(); --ii) {
            if (0>=ii) {
                throw new RuntimeException("cannot select initial cluster centers");
            }
            centers.add(points.get(random.nextInt(points.size())));
        }
        double error=Double.POSITIVE_INFINITY;
        VectorMean<T> mean=meanFactory.create(points.size());
        Sum errorSum=sumFactory.create(points.size());
        for (int ii=maxIterations; 0<ii; --ii) {
            Map<T, List<T>> voronoi=new HashMap<>(centers.size());
            for (T center: centers) {
                voronoi.put(center, new ArrayList<>(points.size()));
            }
            errorSum.clear();
            for (T point: points) {
                T center=nearestCenter(centers, distance, point);
                errorSum.add(distance.distance(center, point));
                voronoi.get(center).add(point);
            }
            double error2=errorSum.sum();
            if (Double.isFinite(error)
                    && (error2>=error*errorLimit)) {
                break;
            }
            error=error2;
            centers.clear();
            for (List<T> cluster: voronoi.values()) {
                mean.clear();
                for (T point: cluster) {
                    mean.add(point);
                }
                centers.add(mean.mean());
            }
            if (centers.size()<clusters) {
                throw new RuntimeException("cluster collapse");
            }
        }
        return new ArrayList<>(centers);
    }

    public static <T> T nearestCenter(Iterable<T> centers, Distance<T> distance, T point) {
        T bestCenter=null;
        double bestDistance=Double.POSITIVE_INFINITY;
        for (T cc: centers) {
            double dd=distance.distance(cc, point);
            if (dd<bestDistance) {
                bestCenter=cc;
                bestDistance=dd;
            }
        }
        if (null==bestCenter) {
            throw new RuntimeException("cannot select nearest center");
        }
        return bestCenter;
    }
}
