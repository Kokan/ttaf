package dog.giraffe.points;

/**
 * Instances of MeanDouble represents the statistical mean of a mutable list of values.
 */
public class MeanDouble {
   private int addends;
   private final Sum sum;

    /**
     * Creates a new MeanDouble.
     *
     * @param expectedAddends the expected number of values the new MeanDouble will contain
     * @param sumFactory factory for the Sum to use
     */
    public MeanDouble(int expectedAddends, Sum.Factory sumFactory) {
       this.sum = sumFactory.create(expectedAddends);
    }

    /**
     * Adds the value addend to the list of values.
     */
    public void add(double addend) {
         ++addends;
         sum.add(addend);
    }

    /**
     * Returns the mean of all of the stored values.
     */
    public double mean() {
         return sum.sum()/addends;
    }
}
