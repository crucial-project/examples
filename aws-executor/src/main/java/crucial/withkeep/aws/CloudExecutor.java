package crucial.withkeep.aws;

import org.infinispan.crucial.CFuture;
import org.infinispan.crucial.CrucialClient;

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.*;

/**
 * Similar to a Java Executor, this will run tasks on the cloud
 * (with serverless functions). Each task will be run on a new
 * worker (an AWS Lambda function). All workers run by the same
 * executor share some context, which makes them easy to coordinate.
 * <p>
 * Date: 2018-05-22
 *
 * @author Daniel
 */
public class CloudExecutor implements ExecutorService{
    /**
     * For Future IDs
     */
    static final String ID_SEPARATOR = "##";
    static final CrucialClient crucial = CrucialClient.getClient("localhost:11222");
    /**
     * Set of run Lambdas
     */
    private final HashSet<ServerlessTask> workers = new HashSet<>();
    private final String executorName = UUID.randomUUID().toString();

    private boolean local = false;

    public void setLocal(boolean local) {
        this.local = local;
        if (local) ExecutorContext.setLocal();
    }

    /**
     * Check if the class is an inner class that is not static.
     *
     * @param clazz Class to check.
     * @return {@code true} if the given class is declared as an inner class
     * (Anonymous, Member or Local) and it is not static. {@code false}
     * otherwise.
     */
    private static boolean isInnerNonStaticClass(Class<?> clazz) {
        return (clazz.isAnonymousClass() || clazz.isMemberClass() || clazz.isLocalClass())
                && !Modifier.isStatic(clazz.getModifiers());
    }

    private <T> ServerlessTask<T> newTaskForCallable(Class<? extends Callable<T>> callable) {
        return new ServerlessTask<>(callable);
    }

    private <T> ServerlessTask<T> newTaskForRunnable(
            Class<? extends Runnable> runnable, T value) {
        return new ServerlessTask<>(runnable, value);
    }

    //private <T> ServerlessTask<T> newTaskForIterative(Class<? extends IterativeRunnable> iterativeRunnable, T value,
    //		ServerlessBatch batch, long fromInclusive, long toExclusive){
    //    return new ServerlessTask<>(iterativeRunnable, value, batch, fromInclusive, toExclusive);
    //}

    /* === === === PUBLIC METHODS === === ===*/

    @Override
    public void shutdown() {

    }

    @Override
    public List<Runnable> shutdownNow() {
        return null;
    }

    @Override
    public boolean isShutdown() {
        return false;
    }

    @Override
    public boolean isTerminated() {
        return false;
    }

    /**
     * Blocks until the termination of the tasks created by this Executor. Or
     * the timeout is reached. This will wait for all futures to be completed.
     *
     * @param timeout maximum time to wait
     * @param unit    time unit of the timeout argument
     * @return {@code true} if this executor terminated and
     * {@code false} if the timeout elapsed before termination
     */
    public boolean awaitTermination(long timeout, TimeUnit unit) {
        long tInit = System.currentTimeMillis();
        long tEnd = tInit + TimeUnit.MILLISECONDS.convert(timeout, unit);
        for (ServerlessTask w : workers) {
            try {
                w.get(tEnd - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                return false;
            }
        }
        return true;
    }

    /**
     * Submits a value-returning task for execution and returns a
     * Future representing the pending results of the task. The
     * Future's {@code get} method will return the task's result upon
     * successful completion.
     * <p>
     * If you would like to immediately block waiting
     * for a task, you can use constructions of the form
     * {@code result = exec.submit(aTaskClass).get();}
     *
     * @param task A {@link Callable} that defines the task.
     *             It cannot be an inner class unless it is static.
     * @param <T>  Return type of the task
     * @return a Future representing pending completion of the task
     */
    @Override
    public <T> Future<T> submit(Callable<T> task) {
        if (task == null) throw new NullPointerException();
        if (isInnerNonStaticClass(task.getClass()))
            throw new RuntimeException("task cannot be an inner class.");
        ServerlessTask<T> fTask = newTaskForCallable((Class<Callable<T>>) task.getClass());
        execute(fTask);
        return fTask;
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        if (task == null) throw new NullPointerException();
        if (isInnerNonStaticClass(task.getClass()))
            throw new RuntimeException("task cannot be an inner class.");
        ServerlessTask<T> fTask = newTaskForRunnable(task.getClass(), result);
        execute(fTask);
        return fTask;
    }

    @Override
    public Future<?> submit(Runnable task) {
        return submit(task, null);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return null;
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return null;
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return null;
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return null;
    }

    /**
     * Submits a value-returning task for execution and returns a
     * Future representing the pending results of the task. The
     * Future's {@code get} method will return the task's result upon
     * successful completion.
     * <p>
     * If you would like to immediately block waiting
     * for a task, you can use constructions of the form
     * {@code result = exec.submit(aTaskClass).get();}
     *
     * @param task Class extending {@link Callable} that defines the task.
     *             It cannot be an inner class unless it is static.
     * @param <T>  Return type of the task
     * @return a Future representing pending completion of the task
     */
    public <T> Future<T> submitCallable(Class<? extends Callable<T>> task) {
        if (task == null) throw new NullPointerException();
        if (isInnerNonStaticClass(task))
            throw new RuntimeException("task cannot be an inner class.");
        ServerlessTask<T> fTask = newTaskForCallable(task);
        execute(fTask);
        return fTask;
    }

    /**
     * Submits a Runnable task for execution and returns a Future
     * representing that task. The Future's {@code get} method will
     * return the given result upon successful completion.
     *
     * @param task   the task to submit.
     *               It cannot be an inner class unless it is static.
     * @param result the result to return
     * @param <T>    the type of the result
     * @return a Future representing pending completion of the task
     */
    public <T> Future<T> submitRunnable(Class<? extends Runnable> task, T result) {
        if (task == null) throw new NullPointerException();
        if (isInnerNonStaticClass(task))
            throw new RuntimeException("task cannot be an inner class.");
        ServerlessTask<T> fTask = newTaskForRunnable(task, result);
        execute(fTask);
        return fTask;
    }

    /**
     * Submits a Runnable task for execution and returns a Future
     * representing that task. The Future's {@code get} method will
     * return {@code null} upon <em>successful</em> completion.
     *
     * @param task the task to submit.
     *             It cannot be an inner class unless it is static.
     * @return a Future representing pending completion of the task
     */
    public Future<?> submitRunnable(Class<? extends Runnable> task) {
        return submitRunnable(task, null);
    }

    /**
     * Invokes a batch of tasks and wait for their completion.
     *
     * @param taskClass class of the task to submit.
     *                  It cannot be an inner class unless it is static.
     * @param nTasks    task batch size
     * @param <T>       return type of the callable
     * @return a list of Futures with the results of all tasks
     */
    public <T> List<Future<T>> invokeAllCallable(
            Class<? extends Callable<T>> taskClass, int nTasks) {
        if (taskClass == null) throw new NullPointerException();
        if (isInnerNonStaticClass(taskClass))
            throw new RuntimeException("task cannot be an inner class.");

        // Create batch
        String batchName = UUID.randomUUID().toString();
        List<ServerlessTask<T>> tasks = new ArrayList<>();
        for (int i = 0; i < nTasks; i++) {
            tasks.add(new ServerlessTask<>(taskClass, i, batchName, nTasks));
        }

        // Run batch
        if (local)
            tasks.forEach(ServerlessTask::run);
        else executeBatch(tasks);

        // AWAIT
        List<Future<T>> futures = new ArrayList<>();
        for (ServerlessTask<T> t : tasks) {
            try {
                futures.add(t);
                t.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return futures;
    }

    /**
     * Invokes a batch of Runnable tasks and waits for their completion.
     *
     * @param taskClass class of the task to submit.
     *                  It cannot be an inner class unless it is static.
     * @param nTasks    task batch size
     */
    public void invokeAllRunnable(Class<? extends Runnable> taskClass, int nTasks) {
        if (taskClass == null) throw new NullPointerException();
        if (isInnerNonStaticClass(taskClass))
            throw new RuntimeException("task cannot be an inner class.");

        // Create batch
        String batchName = UUID.randomUUID().toString();
        List<ServerlessTask<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < nTasks; i++) {
            tasks.add(new ServerlessTask<>(taskClass, null, i, batchName, nTasks));
        }

        // Run batch
        if (local)
            tasks.forEach(ServerlessTask::run);
        else executeBatch(tasks);

        // AWAIT
        for (ServerlessTask t : tasks) {
            try {
                t.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

//    public void invokeIterativeTask(Class<? extends IterativeRunnable> task, int nWorkers,
//                                    long fromInclusive, long toExclusive) {
//        if (task == null) throw new NullPointerException();
//        if (isInnerNonStaticClass(task))
//            throw new RuntimeException("task cannot be an inner class.");
//
//        // Create batch
//        String batchName = UUID.randomUUID().toString();
//        List<ServerlessTask<Void>> tasks = new ArrayList<>();
//        for (int i = 0; i < nWorkers; i++) {
//            tasks.add(new ServerlessTask<>(task, null, i, batchName, nWorkers, fromInclusive, toExclusive));
//        }
//
//        // Run batch
//        if (local)
//            tasks.forEach(ServerlessTask::run);
//        else executeBatch(tasks);
//
//        // AWAIT
//        for (ServerlessTask<Void> t : tasks) {
//            try {
//                t.get();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//    }

    /**
     * Run a worker as an individual task.
     *
     * @param worker task to run
     */
    private void execute(ServerlessTask worker) {
        workers.add(worker);
        worker.run();
    }

    /**
     * Run a batch of workers.
     *
     * @param workers list of tasks to run
     * @param <T>     return type of the workers
     */
    private <T> void executeBatch(List<ServerlessTask<T>> workers) {
        workers.parallelStream().forEach(ServerlessTask::run);
    }

    @Override
    public void execute(Runnable command) {

    }

    /**
     * AKA Worker
     * Represents a task executed on a Lambda. Contains that task as a
     * {@link Class} and the shared Future object. The Future
     * object is also used as container for the worker's state.
     *
     * @param <V> return type of the task
     */
    private class ServerlessTask<V> implements RunnableFuture<V> {
        CloudThread workerThread;
        int workerId;
        String batchName;
        int batchSize = 0;      // 0 means individual task
        Class<?> task;
        CFuture<V> future;
        V runnableResult;
        long fromInclusive = 0;
        long toExclusive = 0;

        ServerlessTask(Class<? extends Callable<V>> callable) {
            this.task = callable;
            workerId = workers.size();
            future = crucial.getCleanFuture(executorName + ID_SEPARATOR + workerId);
        }

        ServerlessTask(Class<? extends Runnable> runnable, V result) {
            this.task = runnable;
            this.runnableResult = result;
            workerId = workers.size();
            future = crucial.getCleanFuture(executorName + ID_SEPARATOR + workerId);
        }

        ServerlessTask(Class<? extends Runnable> runnable, V result,
                       int workerId, String batchName, int batchSize) {
            this.task = runnable;
            this.runnableResult = result;
            this.workerId = workerId;
            this.batchName = batchName;
            this.batchSize = batchSize;
            future = crucial.getCleanFuture(executorName + ID_SEPARATOR +
                    batchName + ID_SEPARATOR + workerId);
        }

        ServerlessTask(Class<? extends Callable> callable,
                       int workerId, String batchName, int batchSize) {
            this.task = callable;
            this.workerId = workerId;
            this.batchName = batchName;
            this.batchSize = batchSize;
            future = crucial.getCleanFuture(executorName + ID_SEPARATOR +
                    batchName + ID_SEPARATOR + workerId);
        }

//        ServerlessTask(Class<? extends IterativeRunnable> iterativeTask, V result, int workerId,
//                       String batchName, int batchSize, long fromInclusive, long toExclusive) {
//            this.task = iterativeTask;
//            this.workerId = String.valueOf(workerId);
//            this.batchName = batchName;
//            this.batchSize = batchSize;
//            future = crucial.getCleanFuture(executorName + ID_SEPARATOR +
//                    batchName + ID_SEPARATOR + workerId);
//            this.fromInclusive = fromInclusive;
//            this.toExclusive = toExclusive;
//            payload = createPayload(this);
//        }

        @Override
        public void run() {
            workerThread = new CloudThread(
                    new ExecutorHandler(
                            workerId, batchSize, task.getName(),
                            batchName, executorName,
                            fromInclusive, toExclusive));
            if (local) workerThread.setLocal(true);
            workerThread.start();
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return future.cancel(mayInterruptIfRunning);
        }

        @Override
        public boolean isCancelled() {
            return future.isCancelled();
        }

        @Override
        public boolean isDone() {
            return future.isDone();
        }

        @Override
        public V get() throws InterruptedException {
            workerThread.join();
            V result = future.get();
            if (runnableResult != null) return runnableResult;
            else return result;
        }

        @Override
        public V get(long timeout, TimeUnit unit)
                throws InterruptedException, TimeoutException {
            V result = future.get(timeout, unit);
            if (runnableResult != null) {
                workerThread.join();
                return runnableResult;
            } else return result;
        }

        public boolean isIterative() {
            return !(fromInclusive == 0 && toExclusive == 0);
        }
    }
}
