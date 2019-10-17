package crucial.withkeep.aws;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class ExecutorContext {
    private static ExecutorContext instance;
    private static boolean local = false;
    private static Map<Thread, ExecutorContext> localIds;
    private int nWorkers = -1;
    private int workerID;
    private String executorName;

    private ExecutorContext(int workerID, int nWorkers, String executorName){
        this.nWorkers = nWorkers;
        this.workerID = workerID;
        this.executorName = executorName;
    }

    private ExecutorContext(int workerID, String executorName){
        this.workerID = workerID;
        this.executorName = executorName;
    }

    static void setLocal(){
        local = true;
        localIds = new HashMap<>();
        instance = null;
    }

    public static boolean isLocal(){
        return local;
    }

    public static ExecutorContext currentWorker(){
        if (local) return localIds.get(Thread.currentThread());
        else return instance;
    }

    static void setInstance(int workerID, int nWorkers, String executorName) {
        if (local)
            localIds.put(Thread.currentThread(), new ExecutorContext(workerID, nWorkers, executorName));
        else
            instance = new ExecutorContext(workerID, nWorkers, executorName);
    }

    static void setInstance(int workerID, String executorName) {
        if (local)
            localIds.put(Thread.currentThread(), new ExecutorContext(workerID, executorName));
        else
            instance = new ExecutorContext(workerID, executorName);
    }

    public int getnWorkers(){
        if (nWorkers != -1) return nWorkers;
        else throw new NoSuchElementException();
    }

    public int getWorkerID(){
        return workerID;
    }

    public String getExecutorName(){
        return executorName;
    }
}
