package dog.giraffe;

public class CannotSelectInitialCentersException extends EmptyClusterException {
    private static final long serialVersionUID=0L;

    public CannotSelectInitialCentersException() {
    }

    public CannotSelectInitialCentersException(String message) {
        super(message);
    }

    public CannotSelectInitialCentersException(String message, Throwable cause) {
        super(message, cause);
    }

    public CannotSelectInitialCentersException(Throwable cause) {
        super(cause);
    }
}
