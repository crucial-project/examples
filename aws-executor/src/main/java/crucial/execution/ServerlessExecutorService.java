package crucial.execution;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.*;

public abstract class ServerlessExecutorService implements ExecutorService {

    private final String executorName = UUID.randomUUID().toString();
    protected boolean logs = true;
    private ExecutorService executorService;
    private boolean local = false;
    private boolean isShutdown = false;
    private List<Future<?>> submittedTasks = new LinkedList<>();

    public ServerlessExecutorService() {
        executorService = Executors.newFixedThreadPool(1000);
    }

    protected String printExecutorPrefix() {
        return "[" + this.executorName + "] ";
    }

    protected String printThreadPrefix() {
        return "[" + Thread.currentThread() + "] ";
    }

    protected String printPrefix() {
        return printExecutorPrefix() + "-" + printThreadPrefix();
    }

    public void shutdown() {
        // Functions cannot be stopped. We do not accept more submissions.
        isShutdown = true;
        executorService.shutdown();
    }

    public List<Runnable> shutdownNow() {
        // Can't do that.
        return null;
    }

    public boolean isShutdown() {
        return isShutdown;
    }

    public boolean isTerminated() {
        return false;
    }

    public boolean awaitTermination(long timeout, TimeUnit unit)
            throws InterruptedException {
        long tInit = System.currentTimeMillis();
        long tEnd = tInit + TimeUnit.MILLISECONDS.convert(timeout, unit);
        for (Future<?> future : submittedTasks) {
            try {
                if (!future.isDone())
                    future.get(tEnd - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
            } catch (ExecutionException e) {
                e.printStackTrace();
                return false;
            } catch (TimeoutException e) {
                return false;
            }
        }
        return true;
    }

    public <T> Future<T> submit(Callable<T> task) {
        if (task == null) throw new NullPointerException();
        if (!(task instanceof Serializable))
            throw new IllegalArgumentException("Tasks must be Serializable");
        Callable<T> localCallable = () -> {
            ThreadCall call = new ThreadCall("ServerlessExecutor-"
                    + Thread.currentThread().getName());
            call.setTarget(task);
            return invoke(call);
        };
        Future<T> f = executorService.submit(localCallable);
        submittedTasks.add(f);
        return f;
    }

    public <T> Future<T> submit(Runnable task, T result) {
        Runnable localRunnable = generateRunnable(task);
        Future<T> f = executorService.submit(localRunnable, result);
        submittedTasks.add(f);
        return f;
    }

    public Future<?> submit(Runnable task) {
        return submit(task, null);
    }

    private <T> List<Callable<T>> generateCallables(Collection<? extends Callable<T>> tasks) {
        List<Callable<T>> localCallables = Collections.synchronizedList(new ArrayList<>());
        tasks.parallelStream().forEach(task -> {
            if (task == null) throw new NullPointerException();
            if (!(task instanceof Serializable))
                throw new IllegalArgumentException("Tasks must be Serializable");
            localCallables.add(() -> {
                ThreadCall threadCall = new ThreadCall("ServerlessExecutor-"
                        + Thread.currentThread().getName());
                threadCall.setTarget(task);
                try {
                    return invoke(threadCall);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            });
        });
        return localCallables;
    }

    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
            throws InterruptedException {
        List<Callable<T>> localCallables = generateCallables(tasks);
        return executorService.invokeAll(localCallables);
    }

    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks,
                                         long timeout, TimeUnit unit)
            throws InterruptedException {
        System.out.println("WARN: invokeAll with timeout. " +
                "If the timeout triggers, Serverless functions cannot be stopped.");
        List<Callable<T>> localCallables = generateCallables(tasks);
        return executorService.invokeAll(localCallables, timeout, unit);
    }

    public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
            throws InterruptedException, ExecutionException {
        return null;
    }

    public <T> T invokeAny(Collection<? extends Callable<T>> tasks,
                           long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return null;
    }

    public void execute(Runnable command) {
        Runnable localRunnable = generateRunnable(command);
        executorService.execute(localRunnable);
    }

    private Runnable generateRunnable(Runnable command) {
        if (command == null) throw new NullPointerException();
        if (!(command instanceof Serializable))
            throw new IllegalArgumentException("Tasks must be Serializable");
        return () -> {
            ThreadCall call = new ThreadCall("ServerlessExecutor-"
                    + Thread.currentThread().getName());
            call.setTarget(command);
            try {
                invoke(call);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        };
    }


    // *** *** *** NEW METHODS *** *** ***

    public void invokeIterativeTask(IterativeRunnable task, int nWorkers,
                                    long fromInclusive, long toExclusive)
            throws InterruptedException {
        if (task == null) throw new NullPointerException();
        // IterativeRunnable is Serializable
        List<Callable<Void>> tasks = new ArrayList<>();
        for (int workerID = 0; workerID < nWorkers; workerID++) {
            tasks.add(new IterativeCallable(fromInclusive, toExclusive,
                    workerID, nWorkers, task, null));
        }
        invokeAll(tasks);
    }

    /**
     * @param task          Task should be an {@link IterativeRunnable}. It can be
     *                      defined with a normal class, a static inner class, or a
     *                      lambda expression (if it does not access a class instance
     *                      field); but not an inner class. Inner classes
     *                      depend on the enclosing instance, which might lead to
     *                      serialization problems.
     * @param nWorkers      Number of workers among which split the iterations.
     * @param fromInclusive Start of the iteration index.
     * @param toExclusive   End of the iteration index.
     * @param finalizer     Runnable to execute by each worker upon completion of
     *                      all iterations.
     * @throws InterruptedException Error awaiting local threads.
     */
    public void invokeIterativeTask(IterativeRunnable task, int nWorkers,
                                    long fromInclusive, long toExclusive,
                                    Runnable finalizer)
            throws InterruptedException {
        if (task == null) throw new NullPointerException();
        // IterativeRunnable is Serializable
        if (finalizer != null && !(finalizer instanceof Serializable))
            throw new IllegalArgumentException("The finalizer must be Serializable");
        if (toExclusive - fromInclusive <= 0)
            throw new IllegalArgumentException("Illegal from-to combination.");
        List<Callable<Void>> tasks = new ArrayList<>();
        for (int workerID = 0; workerID < nWorkers; workerID++) {
            tasks.add(new IterativeCallable(fromInclusive, toExclusive,
                    workerID, nWorkers, task, finalizer));
        }
        invokeAll(tasks);
    }


    // *** *** *** HELPER METHODS *** *** ***

    private <T> T invoke(ThreadCall threadCall) throws IOException, ClassNotFoundException {
        byte[] tC = ByteMarshaller.toBytes(threadCall);
        byte[] ret;
        if (local) ret = invokeLocal(tC);
        else ret = invokeExternal(tC);
        return ByteMarshaller.fromBytes(ret);
    }

    protected abstract byte[] invokeExternal(byte[] threadCall);

    private byte[] invokeLocal(byte[] threadCall) {
        CloudThreadHandler handler = new CloudThreadHandler();
        return handler.handle(threadCall);
    }

    public void setLocal(boolean local) {
        this.local = local;
    }

    public void setLogs(boolean logs) {
        this.logs = logs;
    }

    public abstract void closeInvoker();

    /**
     * This is a static class and not an in-line lambda expression because it
     * needs to be serialized. If it were an in-line definition, it would be
     * serialized along its enclosing instance, which is the entire
     * ExecutorService. And we do not want that.
     */
    static class IterativeCallable implements Serializable, Callable<Void> {
        long fromInclusive, toExclusive;
        int myID, nWorkers;
        IterativeRunnable task;
        Runnable finalizer;

        IterativeCallable(long fromInclusive, long toExclusive,
                          int myID, int nWorkers,
                          IterativeRunnable task,
                          Runnable finalizer) {
            this.fromInclusive = fromInclusive;
            this.toExclusive = toExclusive;
            this.myID = myID;
            this.nWorkers = nWorkers;
            this.task = task;
            this.finalizer = finalizer;
        }

        @Override
        public Void call() throws Exception {
            long size = toExclusive - fromInclusive;
            long range = size / nWorkers;
            // Static partitioning: assigning ranges
            long start = myID * range + fromInclusive;
            long end = (myID == nWorkers - 1) ? toExclusive : start + range;
            for (long l = start; l < end; l++) {
                task.run(l);
            }
            if (finalizer != null) finalizer.run();
            return null;
        }
    }
}
