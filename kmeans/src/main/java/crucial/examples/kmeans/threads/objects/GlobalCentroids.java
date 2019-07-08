package crucial.examples.kmeans.threads.objects;

import java.util.Random;


public class GlobalCentroids {
    private static final int SEED = 1002;
    private int parallelism;

    private double[][] coordinates, correctCoordinates;

    private int[] sizes;
    private int count = 0;

    public GlobalCentroids() {
    }

    public void randomInit(int numClusters, int numDimensions, int parallelism) {
        this.parallelism = parallelism;
        Random rand = new Random(SEED);
        correctCoordinates = new double[numClusters][numDimensions];
        for (int i = 0; i < numClusters; i++) {
            for (int j = 0; j < numDimensions; j++) {
                correctCoordinates[i][j] = rand.nextDouble();
            }
        }
        sizes = new int[numClusters];
    }

    public synchronized int update(double[][] coordinates, int[] sizes, int id) {
        if (count == 0) { // If I'm the first thread --> reset coordinates and sizes to 0
            this.coordinates = new double[coordinates.length][coordinates[0].length];
            this.sizes = new int[sizes.length];
        }

        for (int i = 0; i < coordinates.length; i++) {
            for (int j = 0; j < coordinates[i].length; j++) {
                this.coordinates[i][j] += coordinates[i][j];
            }
            this.sizes[i] += sizes[i];
        }
        count++;
        System.out.println("updateCount = " + count + " id: " + id);

        if (count == parallelism) { // If I'm the last thread --> calculate means
            for (int i = 0; i < coordinates.length; i++) {
                if (this.sizes[i] != 0) {
                    for (int j = 0; j < coordinates[i].length; j++) {
                        this.correctCoordinates[i][j] = this.coordinates[i][j] / this.sizes[i];
                    }
                }
            }
            count = 0;
        }
        return count;
    }

    public double[][] getCorrectCoordinates() {
        return correctCoordinates;
    }

    public void println() {
        System.out.println("CENTROIDS:");
        for (int k = 0; k < correctCoordinates.length; k++) {
            System.out.print("Cluster " + k + " = [");
            for (int j = 0; j < correctCoordinates[k].length; j++) {
                System.out.print(correctCoordinates[k][j] + ",");
            }
            System.out.println("]");
        }
    }
}