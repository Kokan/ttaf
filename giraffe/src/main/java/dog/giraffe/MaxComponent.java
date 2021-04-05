package dog.giraffe;

public interface MaxComponent<U,T> {
    public U max(T self);

    public T maxVec(T self);
}
