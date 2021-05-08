package dog.giraffe.points;

import dog.giraffe.util.Doubles;

/**
 * Instances of Sum represents the sum of a mutable list of double values.
 * As this is a reference type, instances can be used in generic containers, and they are mutable unlike Doubles.
 * The default implementation only stores a single running sum, trading numerical stability for speed.
 */
public interface Sum {
    /**
     * Factory for Sums.
     */
    interface Factory {
        /**
         * Creates a new empty Sum instance.
         *
         * @param expectedAddends the expected number of values the new Sum will contain
         */
        Sum create(int expectedAddends);
    }

    class SingleVariable implements Sum {
        private double sum;

        @Override
        public void add(double addend) {
            Doubles.checkFinite(addend);
            sum=Doubles.checkFinite(sum+addend);
        }

        @Override
        public void addTo(Sum sum) {
            sum.add(this.sum);
        }

        @Override
        public void clear() {
            sum=0.0;
        }

        @Override
        public double sum() {
            return Doubles.checkFinite(sum);
        }
    }

    /**
     * The default implementation of Sum.
     */
    Factory SINGLE_VARIABLE=(expectedAddends)->new SingleVariable();

    /**
     * Adds the value addend to the list of values.
     */
    void add(double addend);

    /**
     * Adds all of the values stored by this Sum instance to sum.
     */
    void addTo(Sum sum);

    /**
     * Removes all the stored values.
     */
    void clear();

    /**
     * Returns the sum of all of the stored values.
     */
    double sum();
}
