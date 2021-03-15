package dog.giraffe;

public interface Distance<T> {
    default void addDistanceTo(T center, T point, Sum sum) {
        sum.add(distance(center, point));
    }

    double distance(T center, T point);
}
