package dog.giraffe;

public class MeanDouble {
   private int addends;
   private final Sum sum;

    public MeanDouble(int expectedAddends, Sum.Factory sumFactory) {
       this.sum = sumFactory.create(expectedAddends);
    }

    public MeanDouble add(Double addend) {
         ++addends;
         sum.add(addend);
         return this;
    }

    public Double mean() {
         return (sum.sum() / addends);
    }
}
