package org.crucial.examples;

import org.crucial.dso.client.Client;
import org.crucial.executor.aws.AWSLambdaExecutorService;
import org.crucial.executor.aws.BaseTest;
import org.testng.annotations.Test;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MonteCarlo extends BaseTest {

    private static final int TRIALS = 10000000;
    private static final int NPAR = 100;

    private double approximation(int trials, long in) {
        return 4.0 * (double) in / (double ) trials;
    }

    public void approximatePi(){
        int counter = 0;
        for(long i=0L; i<TRIALS; i++) {
            if (Math.pow(Math.random(),2) +
                    Math.pow(Math.random(),2)<=1)
                counter++;
        }
        System.out.println("\u03C0 \u2245 "+approximation(TRIALS,counter));
    }

    public void approximatePiExecutor() throws ExecutionException, InterruptedException {
        ExecutorService service = Executors.newCachedThreadPool();
        AtomicInteger cnt = new AtomicInteger();
        Future<Void> f =
                service.submit(
                        () -> {
                            for(long i=0L; i<TRIALS; i++)
                                if (Math.pow(Math.random(), 2) +
                                        Math.pow(Math.random(), 2) <= 1)
                                    cnt.incrementAndGet();
                            return null;
                        });
        f.get();
        System.out.println("\u03C0 \u2245 "+approximation(TRIALS,cnt.get()));
    }

    public void approximatePiParallel() throws InterruptedException, ExecutionException {
        ExecutorService service = Executors.newCachedThreadPool();
        AtomicLong cnt = new AtomicLong();
        List<Future<Void>> l =
                service.invokeAll(
                        IntStream.range(0,NPAR).mapToObj(n ->
                                (Callable<Void>) () -> {
                                    long count = 0;
                                    for (long i = 0L; i < TRIALS; i++)
                                        if (Math.pow(Math.random(), 2) +
                                                Math.pow(Math.random(), 2) <= 1) cnt.incrementAndGet();
                                    cnt.addAndGet(count);
                                    return null;
                                }).collect(Collectors.toList()));
        for (Future<Void> f: l) {f.get();}
        System.out.println("\u03C0 \u2245 "+approximation(TRIALS*NPAR,cnt.get()));
    }

    public void approximatePiCrucial() throws InterruptedException, ExecutionException {
        ExecutorService service = new AWSLambdaExecutorService();
        Client client = new Client();
        client.clear();
        org.crucial.dso.AtomicLong cnt = client.getAtomicLong("counter",0);
        List<Future<Void>> l =
                service.invokeAll(
                        IntStream.range(0,NPAR).mapToObj(n ->
                                (Serializable & Callable<Void>) () -> {
                                    long count = 0;
                                    for (long i = 0L; i < TRIALS; i++)
                                        if (Math.pow(Math.random(), 2) +
                                                Math.pow(Math.random(), 2) <= 1) count++;
                                    cnt.addAndGet(count);
                                    return null;
                                }).collect(Collectors.toList()));
        for (Future<Void> f: l) {f.get();}
        System.out.println("\u03C0 \u2245 "+approximation(TRIALS*NPAR,cnt.get()));
    }

}
