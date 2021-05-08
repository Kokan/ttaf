package dog.giraffe.points;

import dog.giraffe.util.Function;
import java.util.List;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;

/**
 * A list of vectors.
 */
public abstract class Points {
    /**
     * A callback interface with the result of the nearest center calculation.
     */
    public interface Classification<C> {
        /**
         * The nearest center of all vectors in points are center.
         */
        void nearestCenter(C center, Points points);

        /**
         * The nearest center of the vector in points at the index index is center.
         */
        void nearestCenter(C center, Points points, int index);
    }

    /**
     * A callback interface for the enumeration of all of the vectors in a {@link Points}.
     */
    public interface ForEach {
        /**
         * There is a vector in points indexed by index.
         */
        void point(Points points, int index);
    }

    protected final int dimensions;
    protected final Mean.Factory mean;
    protected final Deviation.Factory variance;

    /**
     * Creates a new Points instance with dimensionality dimensions.
     */
    protected Points(int dimensions) {
        if (0>dimensions) {
            throw new IllegalArgumentException(Integer.toString(dimensions));
        }
        this.dimensions=dimensions;
        this.mean=new Mean.Factory(dimensions);
        this.variance=new Deviation.Factory(dimensions);
    }

    /**
     * Adds all of the distances between all vector of this and center to sum.
     */
    public void addAllDistanceTo(Vector center, Sum sum) {
        for (int ii=0; size()>ii; ++ii) {
            addDistanceTo(center, ii, sum);
        }
    }

    /**
     * Adds all vectors of this to mean.
     */
    public void addAllTo(Mean mean) {
        for (int ii=0; size()>ii; ++ii) {
            addTo(ii, mean);
        }
    }

    /**
     * Adds the distance between center and the point indexed by index to sum.
     */
    public void addDistanceTo(Vector center, int index, Sum sum) {
        for (int dd=0; dimensions>dd; ++dd) {
            double di=center.coordinate(dd)-get(dd, index);
            sum.add(di*di);
        }
    }

    /**
     * Adds the vector indexed by index to mean.
     */
    public void addTo(int index, Mean mean) {
        ++mean.addends;
        for (int dd=0; dimensions>dd; ++dd) {
            mean.sums.get(dd).add(get(dd, index));
        }
    }

    /**
     * Determines the nearest center for all the vectors of this.
     * The generic type-parameter C can be used to pass on metadata alongside the vectors of the centers.
     */
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

    /**
     * Return the dimensionality of this.
     */
    public int dimensions() {
        return dimensions;
    }

    /**
     * Returns the distance between center and the vector indexed by index.
     */
    public double distance(Vector center, int index) {
        double distance=0.0;
        for (int dd=0; dimensions>dd; ++dd) {
            double di=center.coordinate(dd)-get(dd, index);
            distance+=di*di;
        }
        return distance;
    }

    /**
     * Returns the vector indexed by index.
     */
    public Vector get(int index) {
        Vector point=new Vector(dimensions);
        for (int dd=0; dimensions()>dd; ++dd) {
            point.coordinate(dd, get(dd, index));
        }
        return point;
    }

    /**
     * Returns the dimension-th coordinate of the vector indexed by index.
     */
    public abstract double get(int dimension, int index);

    /**
     * Returns the dimension-th coordinate of the vector indexed by index.
     * All values are considered to be between 0 and 1 regardless of the internal representation.
     */
    public abstract double getNormalized(int dimension, int index);

    /**
     * Enumerates all the vectors of this.
     */
    public void forEach(ForEach forEach) {
        for (int ii=0; size()>ii; ++ii) {
            forEach.point(this, ii);
        }
    }

    /**
     * Returns the maximum possible value that can be represented by this implementation.
     */
    public abstract double maxValue();

    /**
     * Creates a new factory for Means which is compatible with this.
     */
    public Mean.Factory mean() {
        return mean;
    }

    /**
     * Returns the minimum possible value that can be represented by this implementation.
     */

    public abstract double minValue();

    /**
     * Performs the pointwise operator on all the vectors of this and returns the result.
     */
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

    /**
     * Returns the number of vectors in this.
     */
    public abstract int size();

    /**
     * Splits this into approximately parts disjunct parts.
     */
    public abstract List<Points> split(int parts);

    /**
     * Performs the pointwise operator on all the vectors of this and returns the sum of all of the results.
     */
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

    /**
     * Creates a new factory for Deviations which is compatible with this.
     */
    public Deviation.Factory variance() {
        return variance;
    }

    /**
     * Returns the index of the dimension
     * which has the largest difference between its maximal and minimal coordinate values.
     */
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
