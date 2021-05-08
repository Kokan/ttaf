package dog.giraffe.points;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Instances of Mean represents the statistical mean of a mutable list of vectors.
 */
public class Mean {
    /**
     * Factory for Means.
     */
    public static class Factory {
        private final int dimensions;

        /**
         * Creates a new Factory for vector means.
         *
         * @param dimensions dimensionality of the vectors
         */
        public Factory(int dimensions) {
            this.dimensions=dimensions;
        }

        /**
         * Creates a new empty Mean.
         *
         * @param expectedAddends the expected number of vectors the new Mean will contain
         * @param sumFactory factory for the Sums to use for each dimension
         */
        public Mean create(int expectedAddends, Sum.Factory sumFactory) {
            List<Sum> sums=new ArrayList<>(dimensions);
            for (int dd=dimensions; 0<dd; --dd) {
                sums.add(sumFactory.create(expectedAddends));
            }
            return new Mean(Collections.unmodifiableList(sums));
        }
    }

    int addends;
    final List<Sum> sums;

    private Mean(List<Sum> sums) {
        this.sums=sums;
    }

    /**
     * Adds the vector addend to the list of vectors.
     */
    public void add(Vector addend) {
        ++addends;
        for (int dd=0; sums.size()>dd; ++dd) {
            sums.get(dd).add(addend.coordinate(dd));
        }
    }

    /**
     * Adds the weighted vector sum to the list of vectors.
     *
     * @param addends the weight of sum
     * @param sum the vector to be added to this Mean
     */
    public void addAll(int addends, Vector sum) {
        this.addends+=addends;
        for (int dd=0; sums.size()>dd; ++dd) {
            sums.get(dd).add(sum.coordinate(dd));
        }
    }

    /**
     * Adds all of the vectors stored by this Mean instance to mean.
     */
    public void addTo(Mean mean) {
        mean.addends+=addends;
        for (int dd=0; sums.size()>dd; ++dd) {
            sums.get(dd).addTo(mean.sums.get(dd));
        }
    }

    /**
     * Removes all the stored vectors.
     */
    public void clear() {
        addends=0;
        for (Sum sum: sums) {
            sum.clear();
        }
    }

    /**
     * Returns the mean of all of the stored vectors.
     */
    public Vector mean() {
        if (0>=addends) {
            throw new EmptySetException();
        }
        Vector mean=new Vector(sums.size());
        for (int dd=0; sums.size()>dd; ++dd) {
            mean.coordinate(dd, sums.get(dd).sum()/addends);
        }
        return mean;
    }
}
