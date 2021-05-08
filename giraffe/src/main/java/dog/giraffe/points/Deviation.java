package dog.giraffe.points;

import dog.giraffe.util.Doubles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Instances of Deviation represents the statistical deviation of a mutable list of vectors.
 */
public class Deviation {
    /**
     * Factory for Deviations.
     */
    public static class Factory {
        private final int dimensions;

        /**
         * Creates a new Factory for vector deviations.
         *
         * @param dimensions dimensionality of the vectors
         */
        public Factory(int dimensions) {
            this.dimensions = dimensions;
        }

        /**
         * Creates a new empty Deviation.
         *
         * @param expectedAddends the expected number of vectors the new Deviation will contain
         * @param mean the mean of all the vectors that will be added to this Deviation
         * @param sumFactory factory for the Sums to use for each dimension
         */
        public Deviation create(int expectedAddends, Vector mean, Sum.Factory sumFactory) {
            List<Sum> sums=new ArrayList<>(dimensions);
            for (int ii=0; ii<dimensions; ++ii) {
                sums.add(sumFactory.create(expectedAddends));
            }
            return new Deviation(mean, Collections.unmodifiableList(sums));
        }
    }

    private int addends;
    private final Vector mean;
    private final List<Sum> sums;

    private Deviation(Vector mean, List<Sum> sums) {
        this.mean=mean;
        this.sums=sums;
    }

    /**
     * Adds the vector addend to the list of vectors.
     */
    public void add(Vector addend) {
        ++addends;
        for (int ii=0;ii<addend.dimensions(); ++ii) {
            sums.get(ii).add(Doubles.square(addend.coordinate(ii)-mean.coordinate(ii)));
        }
    }

    /**
     * Returns the deviation of all of the stored vectors.
     */
    public Vector deviation() {
        if (addends==0) {
            throw new RuntimeException("division by zero");
        }
        Vector dev=new Vector(sums.size());
        for (int ii=0;ii<sums.size(); ++ii) {
            dev.coordinate(ii, Math.sqrt(sums.get(ii).sum()/addends));
        }
        return dev;
    }
}
