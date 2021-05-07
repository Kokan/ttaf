package dog.giraffe;

public interface Sum {
    abstract class Abstract implements Sum {
        @Override
        public void add(double addend) {
            Doubles.checkFinite(addend);
            if (0.0!=addend) {
                addImpl(addend);
            }
        }

        protected abstract void addImpl(double addend);

        @Override
        public void clear() {
            clearImpl();
        }

        protected abstract void clearImpl();

        @Override
        public double sum() {
            return Doubles.checkFinite(sumImpl());
        }

        protected abstract double sumImpl();
    }

    interface Factory {
        Sum create(int expectedAddends);
    }

    class SingleVariable extends Abstract {
        private double sum;

        @Override
        protected void addImpl(double addend) {
            sum+=addend;
        }

        @Override
        public void addTo(Sum sum) {
            sum.add(this.sum);
        }

        @Override
        protected void clearImpl() {
            sum=0.0;
        }

        @Override
        protected double sumImpl() {
            return sum;
        }
    }

    Factory SINGLE_VARIABLE=(expectedAddends)->new SingleVariable();

    Factory PREFERRED=SINGLE_VARIABLE;

    void add(double addend);

    void addTo(Sum sum);

    void clear();

    double sum();
}
