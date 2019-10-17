package crucial.withkeep;

/**
 * @author Gerard
 */
public abstract class IterativeRunnable {

    /**
     * This method is invoked once per each iteration assigned to
     * a particular worker.
     *
     * @param index the index of the iteration
     */
    public abstract void run(long index);

    /**
     * This method is invoked after all the iterations have been
     * invoked in a particular worker. The default implementation
     * body is empty. It can be overridden in a concrete subclass.
     */
    public void finalizeIterations() {
    }
}
