package crucial.execution;

import java.io.Serializable;
import java.util.concurrent.Callable;

/**
 * Packages the needed elements to send to a cloud thread. Such as the Runnable
 * or Callable, and the name of that thread (ID).
 */
class ThreadCall implements Serializable {
    private String threadName;
    private Callable target;

    public ThreadCall(String threadName) {
        this.threadName = threadName;
    }

    String getThreadName() {
        return threadName;
    }

    void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    /**
     * Will be a Runnable or a Callable.
     * Runnables are Callables that return null.
     *
     * @return A Callable.
     */
    Callable getTarget() {
        return target;
    }

    /**
     * Must be a Runnable or a Callable
     *
     * @param target Runnable or callable.
     */
    void setTarget(Callable target) {
        if (target == null)
            throw new NullPointerException();
        this.target = target;
    }

    void setTarget(Runnable target) {
        if (target == null)
            throw new NullPointerException();
        this.target = (Serializable & Callable) ()->{
            target.run();
            return null;
        };
    }
}
