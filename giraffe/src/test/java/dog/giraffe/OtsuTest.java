package dog.giraffe;

import dog.giraffe.points.FloatArrayPoints;
import dog.giraffe.points.MutablePoints;
import dog.giraffe.points.Points;
import dog.giraffe.points.Vector;
import dog.giraffe.threads.batch.SingleThreadedExecutor;
import dog.giraffe.threads.batch.SingleThreadedJoin;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.DoubleUnaryOperator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OtsuTest {
    private final TestContext context;
    private final SingleThreadedExecutor executor;

    public OtsuTest() {
        executor=new SingleThreadedExecutor();
        context=new TestContext(executor);
    }

    private static void assertClusters(List<List<Double>> expected, List<List<Vector>> actual) {
        assertEquals(expected.size(), actual.size());
        actual=new ArrayList<>(actual);
        for (int ii=0; actual.size()>ii; ++ii) {
            List<Vector> center=actual.get(ii);
            assertFalse(center.isEmpty());
            center.forEach((vector)->assertEquals(1, vector.dimensions()));
            center=new ArrayList<>(center);
            center.sort(null);
            actual.set(ii, center);
        }
        actual.sort(Comparator.comparing((center)->center.get(0)));
        for (int ii=0; expected.size()>ii; ++ii) {
            List<Double> ec=expected.get(ii);
            List<Vector> ac=actual.get(ii);
            assertEquals(ec.size(), ac.size());
            for (int jj=0; ec.size()>jj; ++jj) {
                assertEquals(ec.get(jj), ac.get(jj).coordinate(0), 0.01);
            }
        }
    }

    private Clusters cluster(
            ClusteringStrategy<Points> clusteringStrategy, DoubleUnaryOperator density) throws Throwable {
        MutablePoints points=new FloatArrayPoints(1, 16);
        for (int xx=0; 100>=xx; ++xx) {
            double x2=xx/100.0;
            for (int yy=(int)(100.0*density.applyAsDouble(x2)); 0<yy; --yy) {
                points.add(new Vector(new double[]{x2}));
            }
        }
        SingleThreadedJoin<Clusters> join=new SingleThreadedJoin<>();
        clusteringStrategy.cluster(context, points, join);
        Clusters clusters=executor.runJoin(context, join);
        assertTrue(executor.isEmpty());
        return clusters;
    }

    private static DoubleUnaryOperator density(double... minimums) {
        return (xx)->{
            double min=Double.POSITIVE_INFINITY;
            for (double min2: minimums) {
                min=Math.min(min, Math.abs(xx-min2));
            }
            return min;
        };
    }

    @Test
    public void testCircular() throws Throwable {
        assertClusters(
                Arrays.asList(
                        Arrays.asList(0.0, 0.5),
                        Arrays.asList(0.5, 1.0)),
                cluster(
                        ClusteringStrategy.otsuCircular(10, 2),
                        density(0.0, 0.5, 1.0))
                        .centers);
        assertClusters(
                Arrays.asList(
                        Arrays.asList(0.0, 0.2, 0.8, 1.0),
                        Arrays.asList(0.2, 0.5),
                        Arrays.asList(0.5, 0.8)),
                cluster(
                        ClusteringStrategy.otsuCircular(10, 3),
                        density(0.2, 0.8))
                        .centers);
        assertClusters(
                Arrays.asList(
                        Arrays.asList(0.0, 0.2, 0.8, 1.0),
                        Arrays.asList(0.2, 0.8)),
                cluster(
                        ClusteringStrategy.otsuCircular(10, 2),
                        density(0.2, 0.8))
                        .centers);
    }

    @Test
    public void testLinear() throws Throwable {
        assertClusters(
                Arrays.asList(
                        Arrays.asList(0.0, 0.2),
                        Arrays.asList(0.2, 0.8),
                        Arrays.asList(0.8, 1.0)),
                cluster(
                        ClusteringStrategy.otsuLinear(10, 3),
                        density(0.2, 0.8))
                        .centers);
    }
}
