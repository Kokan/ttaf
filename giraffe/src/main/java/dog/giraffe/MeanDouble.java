package dog.giraffe;

public class MeanDouble {
    public class Factory {
        private final Sum.Factory sumFactory;

        public Factory(Sum.Factory sumFactory) {
           this.sumFactory = sumFactory;
        }

        public MeanDouble create(int expectedAddends) {
            return new MeanDouble(expectedAddends, sumFactory);
        }
    }

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

    public MeanDouble clear() {
         addends = 0;
         sum.clear();
         return this;
    }

    public Double mean() {
         return (sum.sum() / addends);
    }
}
