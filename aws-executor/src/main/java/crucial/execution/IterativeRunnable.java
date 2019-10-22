package crucial.execution;

import java.io.Serializable;

/**
 * @author Gerard
 */
public interface IterativeRunnable extends Serializable {

    /**
     * This method is invoked once per each iteration assigned to
     * a particular worker.
     *
     * @param index the index of the iteration
     */
    void run(long index);
}
