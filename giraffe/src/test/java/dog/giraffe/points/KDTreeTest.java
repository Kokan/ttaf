package dog.giraffe.points;

import dog.giraffe.Sum;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.fail;

public class KDTreeTest {
    private static List<Vector> create(int dimensions, Random random, int size) {
        List<Vector> list=new ArrayList<>();
        for (; 0<size; --size) {
            Vector vector=new Vector(dimensions);
            for (int dd=0; dimensions>dd; ++dd) {
                vector.coordinate(dd, random.nextDouble());
            }
            list.add(vector);
        }
        return list;
    }

    @Test
    public void test() {
        for (long seed=1L; 1000L>seed; ++seed) {
            Random random=new Random(seed);
            final int dimensions=random.nextInt(4)+1;
            List<Vector> centers=Collections.unmodifiableList(create(dimensions, random, 10));
            VectorList points=new VectorList(create(dimensions, random, 1000));
            KDTree tree=KDTree.create(random.nextInt(10)+1, points, Sum.HEAP);
            tree.classify(
                    Function.identity(),
                    centers,
                    new Points.Classification<>() {
                        @Override
                        public void nearestCenter(Vector center, Points points) {
                            for (int ii=0; points.size()>ii; ++ii) {
                                nearestCenter(center, points, ii);
                            }
                        }

                        @Override
                        public void nearestCenter(Vector center, Points points, int index) {
                            Vector point=points.get(index);
                            Vector center2=Distance.nearestCenter(centers, point);
                            if (!center.equals(center2)) {
                                fail();
                            }
                        }
                    });
        }
    }
}
