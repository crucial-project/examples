package crucial.execution;

import crucial.execution.aws.AWSLambdaExecutorService;
import org.testng.annotations.Test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

public class ServerlessExecutorServiceTest {
    @Test
    public void testSubmit() throws ExecutionException, InterruptedException {
        final String ret = "yeey";

        ServerlessExecutorService es = new AWSLambdaExecutorService();
        es.setLocal(true);

        Future<String> future = es.submit((Serializable & Callable<String>) () -> {
            System.out.println("I am run.");
            return ret;
        });

        assert future.get().equals(ret);


        Future<?> futureR = es.submit((Serializable & Runnable) () -> System.out.println("I am run."));

        assert futureR.get() == null;

        List<Future<String>> futs = new LinkedList<>();
        for (int i = 0; i < 10; i++) {
            int finalI = i;
            futs.add(es.submit((Serializable & Callable<String>) () -> {
                System.out.println("I am run. " + finalI);
                return ret;
            }));
        }
        futs.forEach(stringFuture -> {
            try {
                stringFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });
    }

    @Test
    public void testInvokeAll() throws ExecutionException, InterruptedException {
        final String ret = "yeey";

        ServerlessExecutorService es = new AWSLambdaExecutorService();
        es.setLocal(true);
        List<Callable<String>> myTasks = Collections.synchronizedList(new ArrayList<>());
        IntStream.range(0, 10).forEach(i ->
                myTasks.add((Serializable & Callable<String>) () -> {
                    System.out.println("I am run." + i);
                    return ret;
                }));
        List<Future<String>> futures = es.invokeAll(myTasks);
        for (Future<String> future : futures) {
            assert future.get().equals(ret);
        }
    }

    @Test
    public void testInvokeIterativeTask() {
        ServerlessExecutorService es = new AWSLambdaExecutorService();
        es.setLocal(true);

        System.out.println("EXECUTOR:");
        try {
            es.invokeIterativeTask((IterativeRunnable) index -> System.out.println("HI " + index),
                    2, 0, 10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("With finalize:");
        try {
            es.invokeIterativeTask(
                    (IterativeRunnable) index -> System.out.println("HI " + index),
                    2, 0, 10,
                    (Serializable & Runnable) () -> System.out.println("I'm finished"));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}