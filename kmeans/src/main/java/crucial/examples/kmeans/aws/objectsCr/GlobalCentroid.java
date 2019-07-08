package crucial.examples.kmeans.aws.objectsCr;

import java.util.Random;

public class GlobalCentroid {

    private static final int SEED = 1002;

    private int clusterId;
    private int parallelism;

    private double[] coordinates;
    private double[] correctCoordinates;
    private int size;
    private int count;

    public GlobalCentroid() {
    }

    public GlobalCentroid(int clusterId) {
        this.clusterId = clusterId;
    }

    public void init(int numDimensions, int parallelism) {
        System.out.println("Initializing GlobalCentroid " + clusterId);

        this.coordinates = new double[numDimensions];
        this.correctCoordinates = new double[numDimensions];
        this.size = 0;
        this.count = 0;
        this.parallelism = parallelism;

        // Initializing coordinates randomly
        Random rand = new Random(SEED + clusterId);
        for (int d = 0; d < numDimensions; d++) {
            correctCoordinates[d] = rand.nextGaussian();
        }
    }

    public double[] getCorrectCoordinates() {
        return correctCoordinates;
    }

    public void update(double[] coordinates, int size) {
        if (count == 0) { // If I'm the first thread --> reset coordinates and sizes to 0
            this.coordinates = new double[coordinates.length];
            this.size = 0;
        }

        for (int d = 0; d < this.coordinates.length; d++) {
            this.coordinates[d] += coordinates[d];
        }
        this.size += size;
        count++;

        if (count == parallelism) { // If I'm the last thread --> calculate means
            if (this.size != 0) {
                for (int d = 0; d < this.coordinates.length; d++) {
                    this.correctCoordinates[d] = this.coordinates[d] / this.size;
                }
            }
            count = 0;
        }
    }
}
