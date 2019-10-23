package crucial.examples.kmeans.aws.objectsCr;

import org.infinispan.crucial.CCyclicBarrier;
import org.infinispan.crucial.Shared;

import java.util.ArrayList;
import java.util.List;

public class Worker implements Runnable {
    private static final double THRESHOLD = 0.00001;

    private int workerId;
    private int numDimensions;
    private int numClusters;
    private int maxIterations;
    private int partitionPoints;
    private int parallelism;
    private int startPartition;
    private int endPartition;

    private double[][] localPartition, localCentroids, correctCentroids;
    private int[] localSizes, localMembership;


    // Shared crucial.examples.mandelbrot.objects
    @Shared(key = "delta")
    private GlobalDelta globalDelta = new GlobalDelta();
    @Shared(key = "stats")
    private GlobalStats globalStats = new GlobalStats();
    private GlobalCentroids globalCentroids;
    private CCyclicBarrier barrier = Main.cc.getCyclicBarrier("barrier");

    public Worker(int workerId, int dataPoints, int dimensions, int parallelism,
                  int clusters, int maxIterations) {
        this.workerId = workerId;
        this.numDimensions = dimensions;
        this.numClusters = clusters;
        this.maxIterations = maxIterations;
        this.partitionPoints = dataPoints / parallelism;
        this.parallelism = parallelism;
        startPartition = partitionPoints * workerId;
        endPartition = partitionPoints * (workerId + 1);
    }

    @Override
    public void run() {
        int delta;

        System.out.println("Thread " + workerId + "/" + parallelism +
                " with k=" + numClusters + " maxIt=" + maxIterations);

        globalCentroids = new GlobalCentroids(numClusters, parallelism);

        List<Long> breakdown = new ArrayList<>();
        breakdown.add(System.currentTimeMillis());

        // Load data
        loadDataset();

        localMembership = new int[localPartition.length];

        // barrier before starting iterations, to avoid different execution times
        barrier.await();

        long initTime = System.currentTimeMillis();
        breakdown.add(initTime);
        int iterCount = 0;
        double globalDeltaVal;
        do {
            System.out.println("Iteration " + iterCount + " of crucial.examples.mandelbrot.worker " + workerId);

            // Get local copy of global crucial.examples.mandelbrot.objects
            correctCentroids = globalCentroids.getCorrectCoordinates();

            breakdown.add(System.currentTimeMillis());

            // Reset data structures that will be used in this iteration
            localSizes = new int[numClusters];
            localCentroids = new double[numClusters][numDimensions];

            // Compute phase, returns number of local membership modifications
            delta = computeClusters();

            breakdown.add(System.currentTimeMillis());

            // Update global crucial.examples.mandelbrot.objects
            globalDelta.update(delta, localPartition.length);

            // Update global centroids
            globalCentroids.update(localCentroids, localSizes);

            breakdown.add(System.currentTimeMillis());

            int p = barrier.await();
            System.out.println("Await = " + p);

            breakdown.add(System.currentTimeMillis());
            globalDeltaVal = globalDelta.getGlobalDelta();
            System.out.println("DEBUG: Finished Iteration " + iterCount + " of crucial.examples.mandelbrot.worker " + workerId + " [GlobalDelta=" + globalDeltaVal + "]");
            iterCount++;
        } while (iterCount < maxIterations && globalDeltaVal > THRESHOLD);
        long endTime = System.currentTimeMillis();

        double iterationTime = (endTime - initTime) / 1000.0;
        System.out.println(iterCount + " iterations in " + iterationTime + " s");
        globalStats.update(workerId, iterationTime);

        breakdown.add(System.currentTimeMillis());
        globalStats.updateBreakdown(workerId, breakdown);
    }

    public void loadDataset() {
        S3Reader s3Reader = new S3Reader();
        localPartition = s3Reader.getPoints(workerId, partitionPoints, numDimensions);
    }

    public int computeClusters() {
        int cluster;
        int delta = 0;

        for (int i = startPartition; i < endPartition; i++) {
            cluster = findNearestCluster(i);

            /* For every dimension, add new point to local centroid */
            for (int j = 0; j < numDimensions; j++) {
                localCentroids[cluster][j] += localPartition[i - startPartition][j];
            }

            /* Update local data structure */
            localSizes[cluster]++;

            /* If now point is a member of a different cluster */
            if (localMembership[i - startPartition] != cluster) {
                delta++;
                localMembership[i - startPartition] = cluster;
            }
        }
        return delta;
    }

    public int findNearestCluster(int point) {
        int cluster = 0;
        double min = Double.MAX_VALUE;
        for (int k = 0; k < numClusters; k++) {
            double distance = distance(localPartition[point - startPartition],
                    correctCentroids[k]);
            if (distance < min) {
                min = distance;
                cluster = k;
            }
        }
        return cluster;
    }

    /**
     * We use squared Euclidean distance to avoid unnecessary computations.
     *
     * @param p
     * @param centroid
     * @return
     */
    public double distance(double[] p, double[] centroid) {
        double distance = 0.0;

        for (int i = 0; i < numDimensions; i++) {
            distance += (p[i] - centroid[i]) * (p[i] - centroid[i]);
        }
        return distance;
    }
}