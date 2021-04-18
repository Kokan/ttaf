package dog.giraffe;

public interface VectorStdDeviation<T> {
    interface Factory<T> {
        VectorStdDeviation<T> create(T mean, int addends, Sum.Factory sumFactory);
    }

    VectorStdDeviation<T> add(T addend);

    VectorStdDeviation<T> clear();

    T mean();

    T deviation();
}
