package dog.giraffe.util;

/**
 * An executable block of code.
 * Quite similar to {@link java.lang.Runnable Runnable} but allows checked exceptions.
 */
public interface Block {
    /**
     * Run this block of code.
     */
    void run() throws Throwable;
}
