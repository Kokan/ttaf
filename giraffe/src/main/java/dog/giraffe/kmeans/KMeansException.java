package dog.giraffe.kmeans;

public class KMeansException extends RuntimeException {
    private static final long serialVersionUID=0L;

    public KMeansException() {
    }

    public KMeansException(String message) {
        super(message);
    }

    public KMeansException(String message, Throwable cause) {
        super(message, cause);
    }

    public KMeansException(Throwable cause) {
        super(cause);
    }
}
