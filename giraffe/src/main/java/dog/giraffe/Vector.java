package dog.giraffe;

import java.util.Arrays;
import java.util.Objects;

public class Vector implements Arith<Vector> {
    private final double[] coordinates;

    public Vector(double[] coordinates) {
        this.coordinates=Objects.requireNonNull(coordinates, "coordinates");
    }

   public Vector(double blue, double green, double red) {
        this(new double[3]);
        coordinates[0]=blue;
        coordinates[1]=green;
        coordinates[2]=red;
   }

    public Vector(int dimensions) {
        this(new double[dimensions]);
    }

    public double coordinate(int dimension) {
        return coordinates[dimension];
    }

    public void coordinate(int dimension, double coordinate) {
        coordinates[dimension]=coordinate;
    }

    public Vector copy() {
        return new Vector(Arrays.copyOf(coordinates, coordinates.length));
    }

    public int dimensions() {
        return coordinates.length;
    }

    @Override
    public Vector add(Vector other) {
        final int dim = dimensions();
        double d[] = new double[dim];
        for (int i=0;i<dim;++i) d[i] = this.coordinates[i] + other.coordinates[i];
        return new Vector(d);
    }

    @Override
    public Vector sub(Vector other) {
        final int dim = dimensions();
        double d[] = new double[dim];
        for (int i=0;i<dim;++i) d[i] = this.coordinates[i] - other.coordinates[i];
        return new Vector(d);
    }

    @Override
    public Vector pow() {
        final int dim = dimensions();
        double d[] = new double[dim];
        for (int i=0;i<dim;++i) d[i] = Math.pow(this.coordinates[i], 2);
        return new Vector(d);
    }

    @Override
    public Vector sqrt() {
        final int dim = dimensions();
        double d[] = new double[dim];
        for (int i=0;i<dim;++i) d[i] = Math.sqrt(this.coordinates[i]);
        return new Vector(d);
    }

    @Override
    public Vector mul(double multiplier) {
        final int dim = dimensions();
        double d[] = new double[dim];
        for (int i=0;i<dim;++i) d[i] = multiplier * this.coordinates[i];
        return new Vector(d);
    }

    @Override
    public Vector div(double divisor) {
        final int dim = dimensions();
        double d[] = new double[dim];
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

    @Override
    public String toString() {
        return Arrays.toString(coordinates);
    }
}
