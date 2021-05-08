package dog.giraffe.cluster;

/**
 * Thrown when the initial centers points fos isodata or k-means cannot be selected.
 * This can indicate bad luck for random selections or fewer distinct data points than the required number of clusters.
 */
public class CannotSelectInitialCentersException extends EmptyClusterException {
    private static final long serialVersionUID=0L;

    public CannotSelectInitialCentersException() {
    }

    public CannotSelectInitialCentersException(String message) {
        super(message);
    }
}
