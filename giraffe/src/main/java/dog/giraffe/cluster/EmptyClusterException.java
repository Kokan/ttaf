package dog.giraffe.cluster;

/**
 * Thrown when a cluster becomes empty and no replacement can be selected for it.
 */
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
