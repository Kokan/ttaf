package dog.giraffe.image;

import dog.giraffe.Context;
import dog.giraffe.Pair;
import dog.giraffe.threads.Continuation;

public interface ImageWriter {
    interface Factory<T> {
        <U> void run(
                Context context, int width, int height, int dimensions,
                WriteProcess<U> writeProcess, Continuation<Pair<T, U>> continuation) throws Throwable;
    }

    interface Line {
        void set(int dimension, int xx, double value);

        void setNormalized(int dimension, int xx, double value);

        void write() throws Throwable;
    }

    interface WriteProcess<T> {
        void run(Context context, ImageWriter imageWriter, Continuation<T> continuation) throws Throwable;
    }

    Line getLine(int yy) throws Throwable;
}
