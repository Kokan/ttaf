package dog.giraffe.util;

import java.util.function.DoubleUnaryOperator;

/**
 * Collection of helper methods operating on doubles.
 */
public class Doubles {
    /**
     * The identity function for doubles.
     */
    public static final DoubleUnaryOperator IDENTITY=DoubleUnaryOperator.identity();
    /**
     * The squaring function for doubles.
     */
    public static final DoubleUnaryOperator SQUARE=Doubles::square;

    private Doubles() {
    }

    /**
     * Throws an exception if the value is not finite.
     * The positive infinity, the negative infinity, and NaN are not finite values.
     */
    public static double checkFinite(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(Double.toString(value));
        }
        return value;
    }

    /**
     * Returns the square of the value.
     */
    public static double square(double value) {
        return value*value;
    }
}
