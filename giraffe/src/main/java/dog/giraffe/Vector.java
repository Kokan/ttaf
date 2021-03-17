package dog.giraffe;

import java.util.Arrays;
import java.util.Objects;

public class Vector {
    private final double[] coordinates;

    public Vector(double[] coordinates) {
        this.coordinates=Objects.requireNonNull(coordinates, "coordinates");
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
