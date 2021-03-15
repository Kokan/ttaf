package dog.giraffe.points;

import dog.giraffe.Sum;
import dog.giraffe.VectorMean;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.DoubleBinaryOperator;

public abstract class L2Points<P extends L2Points<P>>
        implements Points<L2Points.Distance, L2Points.Mean, P, double[]> {
    public static class Distance implements dog.giraffe.Distance<double[]> {
        private final int dimensions;

        public Distance(int dimensions) {
            this.dimensions=dimensions;
        }

        @Override
        public void addDistanceTo(double[] center, double[] point, Sum sum) {
            for (int dd=0; dimensions>dd; ++dd) {
                double di=center[dd]-point[dd];
                sum.add(di*di);
            }
        }

        @Override
        public double distance(double[] center, double[] point) {
            double distance=0.0;
            for (int dd=0; dimensions>dd; ++dd) {
                double di=center[dd]-point[dd];
                distance+=di*di;
            }
            return distance;
        }
    }

    public static class Mean implements VectorMean<Mean, double[]> {
        public static class Factory implements VectorMean.Factory<Mean, double[]> {
            private final int dimensions;

            public Factory(int dimensions) {
                this.dimensions=dimensions;
            }

            @Override
            public Mean create(int expectedAddends, Sum.Factory sumFactory) {
                List<Sum> sums=new ArrayList<>(dimensions);
                for (int dd=dimensions; 0<dd; --dd) {
                    sums.add(sumFactory.create(expectedAddends));
                }
                return new Mean(Collections.unmodifiableList(sums));
            }
        }

        private int addends;
        private final List<Sum> sums;

        private Mean(List<Sum> sums) {
            this.sums=sums;
        }

        @Override
        public void add(double[] addend) {
            ++addends;
            for (int dd=0; sums.size()>dd; ++dd) {
                sums.get(dd).add(addend[dd]);
            }
        }

        public void addAll(int addends, double[] sum) {
            this.addends+=addends;
            for (int dd=0; sums.size()>dd; ++dd) {
                sums.get(dd).add(sum[dd]);
            }
        }

        @Override
        public void addTo(Mean mean) {
            mean.addends+=addends;
            for (int dd=0; sums.size()>dd; ++dd) {
                sums.get(dd).addTo(mean.sums.get(dd));
            }
        }

        @Override
        public void clear() {
            addends=0;
            for (Sum sum: sums) {
                sum.clear();
            }
        }

        @Override
        public double[] mean() {
            double[] mean=new double[sums.size()];
            for (int dd=0; sums.size()>dd; ++dd) {
                mean[dd]=sums.get(dd).sum()/addends;
            }
            return mean;
        }
    }

    protected final int dimensions;
    protected final Distance distance;
    protected final Mean.Factory mean;

    public L2Points(int dimensions) {
        if (0>dimensions) {
            throw new IllegalArgumentException(Integer.toString(dimensions));
        }
        this.dimensions=dimensions;
        distance=new Distance(dimensions);
        mean=new Mean.Factory(dimensions);
    }

    @Override
    public void addDistanceTo(double[] center, int index, Sum sum) {
        for (int dd=0; dimensions>dd; ++dd) {
            double di=center[dd]-get(dd, index);
            sum.add(di*di);
        }
    }

    @Override
    public void addTo(int index, Mean mean) {
        ++mean.addends;
        for (int dd=0; dimensions>dd; ++dd) {
            mean.sums.get(dd).add(get(dd, index));
        }
    }

    public int dimensions() {
        return dimensions;
    }

    @Override
    public Distance distance() {
        return distance;
    }

    @Override
    public double distance(double[] center, int index) {
        double distance=0.0;
        for (int dd=0; dimensions>dd; ++dd) {
            double di=center[dd]-get(dd, index);
            distance+=di*di;
        }
        return distance;
    }

    @Override
    public double[] get(int index) {
        double[] point=new double[dimensions];
        for (int dd=0; dimensions()>dd; ++dd) {
            point[dd]=get(dd, index);
        }
        return point;
    }

    public abstract double get(int dimension, int index);

    @Override
    public VectorMean.Factory<Mean, double[]> mean() {
        return mean;
    }

    public double[] perform(double defaultValue, int offset, DoubleBinaryOperator operator, int size) {
        double[] result=new double[dimensions];
        for (int dd=0; dimensions>dd; ++dd) {
            result[dd]=defaultValue;
        }
        for (; 0<size; ++offset, --size) {
            for (int dd=0; dimensions>dd; ++dd) {
                result[dd]=operator.applyAsDouble(result[dd], get(dd, offset));
            }
        }
        return result;
    }

    public double[] sum(int offset, int size, List<Sum> sums) {
        for (int dd=0; dimensions>dd; ++dd) {
            sums.get(dd).clear();
        }
        double[] result=new double[dimensions];
        for (; 0<size; ++offset, --size) {
            for (int dd=0; dimensions>dd; ++dd) {
                sums.get(dd).add(get(dd, offset));
            }
        }
        for (int dd=0; dimensions>dd; ++dd) {
            result[dd]=sums.get(dd).sum();
        }
        return result;
    }

    public int widestDimension(int from, int to) {
        double widestDifference=Double.NEGATIVE_INFINITY;
        int widestDimension=0;
        for (int dd=0; dimensions>dd; ++dd) {
            double max=Double.NEGATIVE_INFINITY;
            double min=Double.POSITIVE_INFINITY;
            for (int ii=from; to>ii; ++ii) {
                double value=get(dd, ii);
                max=Math.max(max, value);
                min=Math.min(min, value);
            }
            double di=max-min;
            if (di>widestDifference) {
                widestDifference=di;
                widestDimension=dd;
            }
        }
        return widestDimension;
    }
}
