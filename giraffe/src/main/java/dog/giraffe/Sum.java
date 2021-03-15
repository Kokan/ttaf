package dog.giraffe;

import java.util.Arrays;

/**
 * there's a fourth method: grouping partial sums by exponents
 */
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

    abstract class Array extends Abstract {
        protected double[] array;
        protected int size;

        public Array(int expectedAddends) {
            this.array=new double[Math.max(1, expectedAddends)];
        }

        @Override
        protected void addImpl(double addend) {
            if (array.length<=size) {
                array=Arrays.copyOf(array, 2*array.length);
            }
            array[size]=addend;
            ++size;
        }

        @Override
        public void addTo(Sum sum) {
            for (int ii=size-1; 0<=ii; --ii) {
                sum.add(array[ii]);
            }
        }

        @Override
        protected void clearImpl() {
            size=0;
        }

        protected abstract void sumArray();

        @Override
        protected double sumImpl() {
            if (0>=size) {
                return 0.0;
            }
            if (1<size) {
                sumArray();
                size=1;
            }
            return array[0];
        }
    }

    interface Factory {
        Sum create(int expectedAddends);
    }

    class Heap extends Array {
        public Heap(int expectedAddends) {
            super(expectedAddends);
        }

        private void fixDown() {
            for (int pp=0; ; ) {
                int lc=(pp<<1)+1;
                if (size<=lc) {
                    return;
                }
                int rc=lc+1;
                if (size<=rc) {
                    if (magnitude(pp)>magnitude(lc)) {
                        swap(pp, lc);
                    }
                    return;
                }
                if ((magnitude(pp)<=magnitude(lc))
                        && (magnitude(pp)<=magnitude(rc))) {
                    return;
                }
                int cc=(magnitude(lc)<magnitude(rc))?lc:rc;
                swap(pp, cc);
                pp=cc;
            }
        }

        private void fixUp(int index) {
            while (0<index) {
                int pp=(index-1)>>>1;
                if (magnitude(pp)<=magnitude(index)) {
                    return;
                }
                swap(pp, index);
                index=pp;
            }
        }

        private double magnitude(int index) {
            return Math.abs(array[index]);
        }

        @Override
        protected void sumArray() {
            for (int ii=1; size>ii; ++ii) {
                fixUp(ii);
            }
            while (1<size) {
                double value=array[0];
                --size;
                array[0]=array[size];
                fixDown();
                array[0]+=value;
                fixDown();
            }
        }

        private void swap(int index0, int index1) {
            double tt=array[index0];
            array[index0]=array[index1];
            array[index1]=tt;
        }
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

    class Tree extends Array {
        public Tree(int expectedAddends) {
            super(expectedAddends);
        }

        private double sum(int offset, int length) {
            if (1==length) {
                return array[offset];
            }
            int length2=length>>>1;
            return sum(offset, length2)+sum(offset+length2, length-length2);
        }

        @Override
        protected void sumArray() {
            array[0]=sum(0, size);
        }
    }

    Factory HEAP=Heap::new;

    Factory SINGLE_VARIABLE=(expectedAddends)->new SingleVariable();

    Factory TREE=Tree::new;

    Factory PREFERRED=SINGLE_VARIABLE;

    void add(double addend);

    void addTo(Sum sum);

    void clear();

    double sum();
}
