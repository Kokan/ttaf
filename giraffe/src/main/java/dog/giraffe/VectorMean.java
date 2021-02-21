package dog.giraffe;

public interface VectorMean<T> {
    interface Factory<T> {
        default VectorMean<T> create() {
            return create(16);
        }

        VectorMean<T> create(int expectedAddends);
    }

    VectorMean<T> add(T addend);

    VectorMean<T> clear();

    T mean();
}
