package crucial.examples.kmeans.aws.objectsCr;

import crucial.execution.aws.AWSLambdaThread;
import org.infinispan.crucial.CCyclicBarrier;
import org.infinispan.crucial.CrucialClient;
import org.infinispan.crucial.Shared;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Main {
    static CrucialClient cc = CrucialClient.getClient("crucialIP:11222"); // CONFIGURE IP

    //private static final int DATAPOINTS_PER_FILE = 6736; // dataset 10,000 dimensions
    private static final int DATAPOINTS_PER_FILE = 695_866; // dataset 100 dimensions

    private static final int DEFAULT_DIMENSIONS = 2;
    private static final int DEFAULT_CLUSTERS = 20;
    private static final int DEFAULT_ITERATIONS = 50;
    private static final int DEFAULT_PARALLELISM = 4;

    private final int numberOfDataPoints;
    private final int numberOfDimensions;
    private final int numberOfClusters;
    private final int numberOfIterations;
    private final int parallelism;

    @Shared(key = "delta")
    private GlobalDelta globalDelta = new GlobalDelta();

    @Shared(key = "stats")
    private GlobalStats globalStats = new GlobalStats();

    private CCyclicBarrier barrier;

    private GlobalCentroids globalCentroids;

    public Main(int numberOfDimensions, int numberOfClusters,
                int numberOfIterations, int parallelism) {
        this.numberOfDataPoints = parallelism * DATAPOINTS_PER_FILE;
        this.numberOfDimensions = numberOfDimensions;
        this.numberOfClusters = numberOfClusters;
        this.numberOfIterations = numberOfIterations;
        this.parallelism = parallelism;
    }

    public static void main(String[] args) {
        Main main;
        if (args.length == 4) {
            main = new Main(Integer.parseInt(args[0]), Integer.parseInt(args[1]),
                    Integer.parseInt(args[2]), Integer.parseInt(args[3]));
        } else {
            main = new Main(DEFAULT_DIMENSIONS, DEFAULT_CLUSTERS,
                    DEFAULT_ITERATIONS, DEFAULT_PARALLELISM);
        }
        main.doMain();
    }

    public void doMain() {

        // Initializing shared data
        globalCentroids = new GlobalCentroids(numberOfClusters, parallelism);
        globalCentroids.randomInit(numberOfDimensions);
        globalDelta.init(parallelism);
        globalStats.init(parallelism);
        barrier = cc.getCyclicBarrier("barrier", parallelism);
        System.out.println("Configured barrier with " + barrier.getParties() + " parties"); // do not comment this line: useful to ensure barrier object is created in Crucial

        ArrayList<Thread> threads = new ArrayList<>();

        long initTime = System.currentTimeMillis();
        for (int w = 0; w < parallelism; w++) {
            threads.add(new AWSLambdaThread(new Worker(w, numberOfDataPoints, numberOfDimensions, parallelism,
                    numberOfClusters, numberOfIterations)));
        }

        for (Thread t : threads)
            t.start();

        try {
            for (Thread t : threads)
                t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        long endTime = System.currentTimeMillis();

        //showResults();
        System.out.println(globalStats.getStats());

        saveBreakdown();

        System.out.println("Elapsed time: " + (endTime - initTime) / 1000.0 + " s");
    }

    private void showResults() {
        globalCentroids.println();
    }

    private void saveBreakdown() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String strDate = LocalDateTime.now().format(formatter);
        Path path = Paths.get("/tmp/output_breakdown_" + strDate + ".csv");
        Map<Integer, List<Long>> breakdownStats = globalStats.getBreakdownStats();
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            for (int w = 0; w < parallelism; w++) {
                String line = breakdownStats.get(w).stream().map(Object::toString).collect(Collectors.joining(","));
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
