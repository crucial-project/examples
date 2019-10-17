package crucial.execution;

import java.io.IOException;
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
        Runnable localRunnable = () -> {
            ThreadCall call = new ThreadCall("ServerlessExecutor-"
                    + Thread.currentThread().getName());
            call.setTarget(task);
            try {
                invoke(call);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        };
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
            localCallables.add(() -> {
                ThreadCall threadCall = new ThreadCall("ServerlessExecutor-"
                        + Thread.currentThread().getName());
                threadCall.setTarget(task);
                return invoke(threadCall);
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

    }

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
}
