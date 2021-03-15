package dog.giraffe;

public interface VectorMean<M extends VectorMean<M, T>, T> {
    interface Factory<M extends VectorMean<M, T>, T> {
        M create(int expectedAddends, Sum.Factory sumFactory);
    }

    void add(T addend);
    
    void addTo(M mean);

    void clear();

    T mean();
}
