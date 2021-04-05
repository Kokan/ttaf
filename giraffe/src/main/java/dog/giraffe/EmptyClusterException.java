package dog.giraffe;

public class EmptyClusterException extends RuntimeException {
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
