package dog.giraffe.gui;

import dog.giraffe.Context;
import dog.giraffe.threads.Continuation;

public interface AsyncTask<T> {
    void run(Context context, Continuation<T> continuation) throws Throwable;
}
