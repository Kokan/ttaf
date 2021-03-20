package dog.giraffe;

public class MeanDouble implements VectorMean<Double> {
    public class Factory implements VectorMean.Factory<Double> {
        private final Sum.Factory sumFactory;

        public Factory(Sum.Factory sumFactory) {
           this.sumFactory = sumFactory;
        }

        @Override
        public VectorMean<Double> create(int expectedAddends) {
            return new MeanDouble(expectedAddends, sumFactory);
        }
    }

   private int addends;
   private final Sum sum;

    public MeanDouble(int expectedAddends, Sum.Factory sumFactory) {
       this.sum = sumFactory.create(expectedAddends);
   }

    @Override
    public VectorMean<Double> add(Double addend) {
         ++addends;
         sum.add(addend);
         return this;
    }

    @Override
    public VectorMean<Double> clear() {
         addends = 0;
         sum.clear();
         return this;
    }

    @Override
    public Double mean() {
         return (sum.sum() / addends);
    }
}
