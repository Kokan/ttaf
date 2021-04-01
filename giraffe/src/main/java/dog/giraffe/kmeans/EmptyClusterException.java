package dog.giraffe.kmeans;

public class EmptyClusterException extends KMeansException {
    private static final long serialVersionUID=0L;

    public EmptyClusterException() {
    }

    public EmptyClusterException(String message) {
        super(message);
    }

    public EmptyClusterException(String message, Throwable cause) {
        super(message, cause);
    }

    public EmptyClusterException(Throwable cause) {
        super(cause);
    }
}
