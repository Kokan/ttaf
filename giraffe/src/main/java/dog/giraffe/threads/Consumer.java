package dog.giraffe.threads;

import java.util.ArrayList;
import java.util.List;

public interface Consumer<T> {
    void accept(T value) throws Throwable;

    static <T> Consumer<T> fork(List<? extends Consumer<? super T>> consumers) {
        List<Consumer<? super T>> consumers2=new ArrayList<>(consumers);
        return (value)->{
            for (Consumer<? super T> consumer: consumers2) {
                consumer.accept(value);
            }
        };
    }
}
