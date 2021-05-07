package dog.giraffe.points;

import dog.giraffe.Sum;
import dog.giraffe.threads.Function;
import java.util.List;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;

public abstract class Points {
    public interface Classification<C> {
        void nearestCenter(C center, Points points);

        void nearestCenter(C center, Points points, int index);
    }

    public interface ForEach {
        void point(Points points, int index );
    }

    protected final int dimensions;
    protected final Mean.Factory mean;
    protected final Variance.Factory variance;

    protected Points(int dimensions) {
        if (0>dimensions) {
            throw new IllegalArgumentException(Integer.toString(dimensions));
        }
        this.dimensions=dimensions;
        this.mean=new Mean.Factory(dimensions);
        this.variance=new Variance.Factory(dimensions);
    }

    public void addAllDistanceTo(Vector center, Sum sum) {
        for (int ii=0; size()>ii; ++ii) {
            addDistanceTo(center, ii, sum);
        }
    }

    public void addAllTo(Mean mean) {
        for (int ii=0; size()>ii; ++ii) {
            addTo(ii, mean);
        }
    }

    public void addDistanceTo(Vector center, int index, Sum sum) {
        for (int dd=0; dimensions>dd; ++dd) {
            double di=center.coordinate(dd)-get(dd, index);
            sum.add(di*di);
        }
    }

    public void addTo(int index, Mean mean) {
        ++mean.addends;
        for (int dd=0; dimensions>dd; ++dd) {
            mean.sums.get(dd).add(get(dd, index));
        }
    }

    public <C> void classify(
            Function<C, Vector> centerPoint, List<C> centers, Classification<C> classification) throws Throwable {
        if (centers.isEmpty()) {
            throw new IllegalArgumentException();
        }
        for (int ii=0; size()>ii; ++ii) {
            C nc=null;
            double nd=Double.POSITIVE_INFINITY;
            for (C center: centers) {
                double dd=distance(centerPoint.apply(center), ii);
                if (nd>dd) {
                    nc=center;
                    nd=dd;
                }
            }
            classification.nearestCenter(nc, this, ii);
        }
    }

    public int dimensions() {
        return dimensions;
    }

    public double distance(Vector center, int index) {
        double distance=0.0;
        for (int dd=0; dimensions>dd; ++dd) {
            double di=center.coordinate(dd)-get(dd, index);
            distance+=di*di;
        }
        return distance;
    }

    public Vector get(int index) {
        Vector point=new Vector(dimensions);
        for (int dd=0; dimensions()>dd; ++dd) {
            point.coordinate(dd, get(dd, index));
        }
        return point;
    }

    public abstract double get(int dimension, int index);

    public abstract double getNormalized(int dimension, int index);

    public void forEach(ForEach forEach) {
        for (int ii=0; size()>ii; ++ii) {
            forEach.point(this, ii);
        }
    }

    public abstract double maxValue();

    public Mean.Factory mean() {
        return mean;
    }

    public abstract double minValue();

    Vector perform(double defaultValue, DoubleBinaryOperator operator) {
        Vector result=new Vector(dimensions);
        for (int dd=0; dimensions>dd; ++dd) {
            result.coordinate(dd, defaultValue);
        }
        for (int offset=0, size=size(); 0<size; ++offset, --size) {
            for (int dd=0; dimensions>dd; ++dd) {
                result.coordinate(dd, operator.applyAsDouble(result.coordinate(dd), get(dd, offset)));
            }
        }
        return result;
    }

    public abstract int size();

    public abstract List<Points> split(int parts);

    Vector sum(DoubleUnaryOperator operator, List<Sum> sums) {
        for (int dd=0; dimensions>dd; ++dd) {
            sums.get(dd).clear();
        }
        Vector result=new Vector(dimensions);
        for (int offset=0, size=size(); 0<size; ++offset, --size) {
            for (int dd=0; dimensions>dd; ++dd) {
                sums.get(dd).add(operator.applyAsDouble(get(dd, offset)));
            }
        }
        for (int dd=0; dimensions>dd; ++dd) {
            result.coordinate(dd, sums.get(dd).sum());
        }
        return result;
    }

    public Variance.Factory variance() {
        return variance;
    }

    int widestDimension() {
        double widestDifference=Double.NEGATIVE_INFINITY;
        int widestDimension=0;
        for (int dd=0; dimensions>dd; ++dd) {
            double max=Double.NEGATIVE_INFINITY;
            double min=Double.POSITIVE_INFINITY;
            for (int ii=0; size()>ii; ++ii) {
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
