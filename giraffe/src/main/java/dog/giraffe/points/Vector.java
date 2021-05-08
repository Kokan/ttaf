package dog.giraffe.points;

import dog.giraffe.util.LexicographicComparator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

/**
 * Mutable representation of a vector in Euclidean space.
 */
public class Vector implements Comparable<Vector> {
    /**
     * A lexicographic order on vectors.
     */
    public static final Comparator<Vector> COMPARATOR=new LexicographicComparator<>() {
        @Override
        protected int compare(Vector object1, int index1, Vector object2, int index2) {
            return Double.compare(object1.coordinate(index1), object2.coordinate(index2));
        }

        @Override
        protected int length(Vector object) {
            return object.dimensions();
        }
    };

    private final double[] coordinates;

    /**
     * Creates a new {@link Vector} with the given coordinates. The coordinates will be shared with the array.
     */
    public Vector(double[] coordinates) {
        this.coordinates=Objects.requireNonNull(coordinates, "coordinates");
    }

    /**
     * Creates a new zero-vector with the given dimensions.
     */
    public Vector(int dimensions) {
        this(new double[dimensions]);
    }

    /**
     * Creates a new {@link Vector} whose coordinates are the pointwise sum of the coordinates of this and other.
     */
    public Vector add(Vector other) {
        final int dim = dimensions();
        double[] d = new double[dim];
        for (int i=0;i<dim;++i) d[i] = this.coordinates[i] + other.coordinates[i];
        return new Vector(d);
    }

    /**
     * Compares this and vector by the lexicographic order.
     */
    @Override
    public int compareTo(Vector vector) {
        return COMPARATOR.compare(this, vector);
    }

    /**
     * Returns the coordinate of this in the dimension dimension.
     */
    public double coordinate(int dimension) {
        return coordinates[dimension];
    }

    /**
     * Replaces the coordinate of this in the dimension dimension by coordinate.
     */
    public void coordinate(int dimension, double coordinate) {
        coordinates[dimension]=coordinate;
    }

    /**
     * Creates a new {@link Vector} with identical coordinates to this.
     */
    public Vector copy() {
        return new Vector(Arrays.copyOf(coordinates, coordinates.length));
    }

    /**
     * Returns the dimensionality of this.
     */
    public int dimensions() {
        return coordinates.length;
    }

    /**
     * Return a new {@link Vector} which is the scalar division of this and divisor.
     */
    public Vector div(double divisor) {
        final int dim = dimensions();
        double[] d = new double[dim];
        for (int i=0;i<dim;++i) d[i] = this.coordinates[i] / divisor;
        return new Vector(d);
    }

    @Override
    public boolean equals(Object obj) {
        if (this==obj) {
            return true;
        }
        if ((null==obj)
                || (!getClass().equals(obj.getClass()))) {
            return false;
        }
        return Arrays.equals(coordinates, ((Vector)obj).coordinates);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(coordinates);
    }

    /**
     * Return a new {@link Vector} which is the scalar multiply of this and multiplier.
     */
    public Vector mul(double multiplier) {
        final int dim = dimensions();
        double[] d = new double[dim];
        for (int i=0;i<dim;++i) d[i] = multiplier * this.coordinates[i];
        return new Vector(d);
    }

    /**
     * Creates a new {@link Vector} whose coordinates are
     * the pointwise difference of the coordinates of this and other.
     */
    public Vector sub(Vector other) {
        final int dim = dimensions();
        double[] d = new double[dim];
        for (int i=0;i<dim;++i) d[i] = this.coordinates[i] - other.coordinates[i];
        return new Vector(d);
    }

    @Override
    public String toString() {
        return Arrays.toString(coordinates);
    }
}
