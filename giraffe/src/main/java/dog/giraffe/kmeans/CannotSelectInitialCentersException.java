package dog.giraffe.kmeans;

public class CannotSelectInitialCentersException extends EmptyClusterException {
    private static final long serialVersionUID=0L;

    public CannotSelectInitialCentersException() {
    }

    public CannotSelectInitialCentersException(String message) {
        super(message);
    }
}
