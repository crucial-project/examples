package crucial.examples.kmeans.threads.objectsCr;

import org.infinispan.crucial.CCyclicBarrier;
import org.infinispan.crucial.Shared;

import java.util.Random;

public class Worker implements Runnable {
    private static final double THRESHOLD = 0.00001;

    private int workerId;
    private double[][] localPartition, localCentroids, correctCentroids;
    private int[] localSizes, localMembership;

    private int maxIterations;
    private int partitionPoints;
    private int numDimensions;
    private int numClusters;
    private int startPartition;
    private int endPartition;

    // Shared crucial.examples.mandelbrot.objects
    @Shared(key = "centroids")
    private GlobalCentroids globalCentroids = new GlobalCentroids();
    @Shared(key = "delta")
    private GlobalDelta globalDelta = new GlobalDelta();
    private CCyclicBarrier barrier = Main.cc.getCyclicBarrier("barrier");

    public Worker(int workerId, int dataPoints, int dimensions, int parallelism,
                  int clusters, int maxIterations) {
        this.workerId = workerId;
        this.numDimensions = dimensions;
        this.numClusters = clusters;
        this.maxIterations = maxIterations;
        this.partitionPoints = dataPoints / parallelism;
        startPartition = partitionPoints * workerId;
        endPartition = partitionPoints * (workerId + 1);
    }

    @Override
    public void run() {
        double delta;

        System.out.println("Thread " + workerId);

        // Generate data
        loadPseudoClusteredDataset();

        localMembership = new int[localPartition.length];

        int iterCount = 0;
        do {
            System.out.println("Iteration " + iterCount + " of crucial.examples.mandelbrot.worker " + workerId);

            // Get local copy of global crucial.examples.mandelbrot.objects
            correctCentroids = globalCentroids.getCorrectCoordinates();

            // Reset data structures that will be used in this iteration
            localSizes = new int[numClusters];
            localCentroids = new double[numClusters][numDimensions];

            // Compute phase, returns number of local membership modifications
            delta = computeClusters();

            // Update global crucial.examples.mandelbrot.objects
            globalDelta.update(delta, localPartition.length);

            // Update global centroids
            globalCentroids.update(localCentroids, localSizes, workerId);

            int p = barrier.await();
            System.out.println("Await = " + p);

            iterCount++;
        } while (iterCount < maxIterations && globalDelta.getGlobalDelta() > THRESHOLD);
    }

    public void loadPseudoClusteredDataset() {
        localPartition = new double[partitionPoints][numDimensions];

        Random rand = new Random(12345);

        double[][] samplePoints = new double[numClusters][numDimensions];
        for (int k = 0; k < numClusters; k++)
            for (int j = 0; j < numDimensions; j++)
                samplePoints[k][j] = rand.nextDouble();

        if (workerId == 0) {
            System.out.println("Base points are:");
            StringBuilder str = new StringBuilder();
            for (int k = 0; k < numClusters; k++) {

                str.append(" [");
                for (int j = 0; j < samplePoints[k].length; j++) {
                    str.append(samplePoints[k][j]).append(",");
                }
                str.append("]\n");
            }
            System.out.println(str.toString());
        }

        rand = new Random(workerId);

        for (int i = 0; i < partitionPoints; i++) {
            double[] basePoint = samplePoints[rand.nextInt(numClusters)];
            for (int j = 0; j < numDimensions; j++) {
                localPartition[i][j] = basePoint[j] + (rand.nextGaussian());
            }
        }
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
        double max = Double.MAX_VALUE, min = max, distance;
        for (int k = 0; k < numClusters; k++) {
            distance = distance(localPartition[point - startPartition],
                    correctCentroids[k]);
            if (distance < min) {
                min = distance;
                cluster = k;
            }
        }
        return cluster;
    }

    public double distance(double[] p, double[] centroid) {
        double distance = 0.0;

        for (int i = 0; i < numDimensions; i++) {
            distance += (p[i] - centroid[i]) * (p[i] - centroid[i]);
        }
        return distance;
    }
}