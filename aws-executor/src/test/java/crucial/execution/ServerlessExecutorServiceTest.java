package crucial.execution;

import crucial.execution.aws.AWSLambdaExecutorService;
import org.testng.annotations.Test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static org.testng.Assert.*;

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
}