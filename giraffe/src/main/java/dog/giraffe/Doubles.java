package dog.giraffe;

public class Doubles {
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
