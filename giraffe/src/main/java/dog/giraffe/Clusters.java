package dog.giraffe;

import java.util.List;

public class Clusters<T> {
    public final List<T> centers;
    public final double error;

    public Clusters(List<T> centers, double error) {
        this.centers=centers;
        this.error=error;
    }
}
