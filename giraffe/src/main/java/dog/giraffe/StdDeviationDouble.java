package dog.giraffe;

public class StdDeviationDouble implements VectorStdDeviation<Double> {
    class Factory implements VectorStdDeviation.Factory<Double> {
        private final VectorMean.Factory<Double> meanFactory;
        private final Sum.Factory sumFactory;

        public Factory(VectorMean.Factory<Double> meanFactory, Sum.Factory sumFactory) {
           this.meanFactory = meanFactory;
           this.sumFactory = sumFactory;
        }
         
        @Override
        public VectorStdDeviation<Double> create(Double mean, int addend) {
            return new StdDeviationDouble(mean, meanFactory, sumFactory);
        }
    }

    private final Double mean;
    private final VectorMean<Double> dev;

   public StdDeviationDouble(Double mean, VectorMean.Factory<Double> meanFactory, Sum.Factory sumFactory) {
       this.mean = mean; 
       this.dev = meanFactory.create();
    }

    @Override
    public VectorStdDeviation<Double> add(Double addend) {
         dev.add(mean-addend);

         return this;
    }

    @Override
    public VectorStdDeviation<Double> clear() {
       dev.clear();
 
       return this;
    }

    @Override
    public Double mean() {
         return mean;
    }

    @Override
    public Double deviation() {
         return dev.mean();
    }
}
