package dog.giraffe;

import java.util.function.DoubleUnaryOperator;

public class Doubles {
    public static final DoubleUnaryOperator IDENTITY=DoubleUnaryOperator.identity();
    public static final DoubleUnaryOperator SQUARE=Doubles::square;

    private Doubles() {
    }

    public static double checkFinite(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(Double.toString(value));
        }
        return value;
    }

    public static double square(double value) {
        return value*value;
    }
}
