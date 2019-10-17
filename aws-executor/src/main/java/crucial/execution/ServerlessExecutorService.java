package crucial.execution;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class ServerlessExecutorService implements ExecutorService {

    private final String executorName = UUID.randomUUID().toString();
    protected boolean logs = true;
    private ExecutorService executorService;
    private AtomicInteger invocationCounter;
    private boolean local = false;
    private boolean isShutdown = false;

    public ServerlessExecutorService() {
        executorService = Executors.newFixedThreadPool(1000);
        invocationCounter = new AtomicInteger();
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
        return false;
    }

    public <T> Future<T> submit(Callable<T> task) {
        try {
            return invokeAll(Collections.singleton(task)).get(0);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public <T> Future<T> submit(Runnable task, T result) {
        return null;
    }

    public Future<?> submit(Runnable task) {
        return submit(task, null);
    }

    private <T> List<Callable<T>> generateCallables(Collection<? extends Callable<T>> tasks) {
        List<Callable<T>> localCallables = Collections.synchronizedList(new ArrayList<>());
        tasks.parallelStream().forEach(task -> {
            localCallables.add(() -> {
                System.out.println("Invoking #" + invocationCounter.incrementAndGet()
                        + ": " + task);
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
