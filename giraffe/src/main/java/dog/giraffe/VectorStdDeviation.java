package dog.giraffe;

public interface VectorStdDeviation<S extends VectorStdDeviation<S, T>, T> {
    interface Factory<S extends VectorStdDeviation<S,T>, T> {
        VectorStdDeviation<S, T> create(T mean, int addends, Sum.Factory sumFactory);
    }

    void add(T addend);

    void addTo(S dev);

    void clear();

    T mean();

    T deviation();
}
