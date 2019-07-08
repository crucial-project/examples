package crucial.examples.kmeans.threads.objectsCr;

import org.infinispan.crucial.CCyclicBarrier;
import org.infinispan.crucial.CrucialClient;
import org.infinispan.crucial.Shared;

import java.util.ArrayList;

public class Main {
    static CrucialClient cc = CrucialClient.getClient("crucialIP:11222");

    private static final int DEFAULT_DATA_POINTS = 1000;
    private static final int DEFAULT_DIMENSIONS = 2;
    private static final int DEFAULT_CLUSTERS = 20;
    private static final int DEFAULT_ITERATIONS = 50;
    private static final int DEFAULT_PARALLELISM = 4;

    private final int numberOfDataPoints;
    private final int numberOfDimensions;
    private final int numberOfClusters;
    private final int numberOfIterations;
    private final int parallelism;

    @Shared(key = "centroids")
    private GlobalCentroids globalCentroids = new GlobalCentroids();

    @Shared(key = "delta")
    private GlobalDelta globalDelta = new GlobalDelta();

    private CCyclicBarrier barrier;

    public Main(int numberOfDataPoints, int numberOfDimensions, int numberOfClusters,
                int numberOfIterations, int parallelism) {
        this.numberOfDataPoints = numberOfDataPoints;
        this.numberOfDimensions = numberOfDimensions;
        this.numberOfClusters = numberOfClusters;
        this.numberOfIterations = numberOfIterations;
        this.parallelism = parallelism;
    }

    public static void main(String[] args) {
        Main main;
        if (args.length == 5) {
            main = new Main(Integer.parseInt(args[0]), Integer.parseInt(args[1]),
                    Integer.parseInt(args[2]), Integer.parseInt(args[3]),
                    Integer.parseInt(args[4]));
        } else {
            main = new Main(DEFAULT_DATA_POINTS, DEFAULT_DIMENSIONS,
                    DEFAULT_CLUSTERS, DEFAULT_ITERATIONS,
                    DEFAULT_PARALLELISM);
        }
        main.doMain();
    }

    public void doMain() {
        if ((numberOfDataPoints % parallelism) != 0) {
            System.out.println("Cannot apply an even partition of data points.");
            System.exit(1);
        }

        // Initializing shared data
        globalCentroids.randomInit(numberOfClusters, numberOfDimensions, parallelism);
        globalDelta.init(parallelism);
        barrier = cc.getCyclicBarrier("barrier", parallelism);

        ArrayList<Thread> threads = new ArrayList<>();

        long initTime = System.currentTimeMillis();
        for (int w = 0; w < parallelism; w++) {
            threads.add(new Thread(new Worker(w, numberOfDataPoints, numberOfDimensions, parallelism,
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

        showResults();
        System.out.println("Elapsed time: " + (endTime - initTime) / 1000.0 + " s");
    }

    private void showResults() {
        globalCentroids.println();
    }
}
