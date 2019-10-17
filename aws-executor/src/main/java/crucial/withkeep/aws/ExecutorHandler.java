package crucial.withkeep.aws;

import org.infinispan.crucial.CFuture;
import org.infinispan.crucial.CrucialClient;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

/**
 * Contains the logic to run a task from a CloudExecutor in a CloudThread.
 */
public class ExecutorHandler implements Runnable {
    private static final CrucialClient crucial = CloudExecutor.crucial;

    @Keep
    private int me, nWorkers;
    @Keep
    private String executorName, className, batchName;
    @Keep
    private boolean iterative;
    @Keep
    private long fromInclusive, toExclusive;

//    /**
//     * For single executions.
//     *
//     * @param me           Number of this task in the batch.
//     * @param className    Name of the Class that implements the task (Runnable or Callable).
//     * @param executorName Name of the executor.
//     */
//    public ExecutorHandler(int me, String className, String executorName) {
//        this(me, 0, className, null, executorName);
//    }
//
//    /**
//     * Basic for a batch execution.
//     *
//     * @param me           Number of this task in the batch.
//     * @param nWorkers     Number of tasks in the batch.
//     * @param className    Name of the Class that implements the task (Runnable or Callable).
//     * @param batchName    Name of the batch.
//     * @param executorName Name of the executor.
//     */
//    public ExecutorHandler(int me, int nWorkers, String className,
//                           String batchName, String executorName) {
//        this(me, nWorkers, className,
//                batchName, executorName,
//                0, 0);
//    }

    /**
     * For iterative tasks. That are also batches.
     *
     * @param me            Number of this task in the batch.
     * @param nWorkers      Number of tasks in the batch.
     * @param className     Name of the Class that implements the task (Runnable or Callable).
     * @param batchName     Name of the batch.
     * @param executorName  Name of the executor.
     * @param fromInclusive Iterative: start of the iteration counter.
     * @param toExclusive   Iterative: end of the iteration counter (i < toExclusive).
     */
    public ExecutorHandler(int me, int nWorkers, String className,
                           String batchName, String executorName,
                           long fromInclusive, long toExclusive) {
        this.me = me;
        this.nWorkers = nWorkers;
        this.className = className;
        this.batchName = batchName;
        this.executorName = executorName;
        this.iterative = true;
        this.fromInclusive = fromInclusive;
        this.toExclusive = toExclusive;
    }


    @Override
    public void run() {
        CFuture<Object> future;

        if (nWorkers == 0) {        // Single execution
            future = crucial.getFuture(executorName + CloudExecutor.ID_SEPARATOR + me);
            ExecutorContext.setInstance(me, executorName);
        } else {                    // Batches
            future = crucial.getFuture(executorName + CloudExecutor.ID_SEPARATOR +
                    batchName + CloudExecutor.ID_SEPARATOR + me);
            ExecutorContext.setInstance(me, nWorkers,
                    executorName + CloudExecutor.ID_SEPARATOR + batchName);
        }
        try {
            Class taskClass = Class.forName(className);
            Callable task;
//            if (!iterative) {
            if (Runnable.class.isAssignableFrom(taskClass))
                task = Executors.callable((Runnable) taskClass.newInstance());
            else
                task = (Callable) taskClass.newInstance();

//            } else {
//                // Iterative callable
//                task = new IterativeCallable(taskClass, me, nWorkers, fromInclusive, toExclusive);
//            }

            // do work
            Object result = task.call();

            System.out.println(me + " - I'm completing work.");
            future.set(result);     // Runnable set *null*
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
            /* We let exceptions escalate since the CloudThread logic will
                manage them. Also, the CloudThread may perform retries.
             */
        }
        System.out.println(me + " - END TASK");
    }
}
