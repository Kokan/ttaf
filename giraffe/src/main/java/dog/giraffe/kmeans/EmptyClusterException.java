package dog.giraffe.kmeans;

public class EmptyClusterException extends RuntimeException {
    private static final long serialVersionUID=0L;

    public EmptyClusterException() {
    }

    public EmptyClusterException(String message) {
        super(message);
    }

    public EmptyClusterException(Throwable cause) {
        super(cause);
    }
}
