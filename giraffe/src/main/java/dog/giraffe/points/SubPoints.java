package dog.giraffe.points;

public interface SubPoints<P extends SubPoints<P>> {
    P subPoints(int fromIndex, int toIndex);
}
