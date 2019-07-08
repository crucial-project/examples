package crucial.examples.kmeans.aws.objectsCr;

import org.infinispan.crucial.Factory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GlobalCentroids {

    private int parallelism;
    private int numClusters;

    Map<String, GlobalCentroid> globalCentroidsMap = new ConcurrentHashMap<>();

    private Factory factory;

    public GlobalCentroids(int numClusters, int parallelism) {
        factory = Factory.getSingleton();
        this.parallelism = parallelism;
        this.numClusters = numClusters;
    }

    public void randomInit(int numDimensions) {
        System.out.println("Initializing GlobalCentroids with " + numClusters + "," + numDimensions + "," + parallelism);

        for (int i = 0; i < numClusters; i++) {
            GlobalCentroid centroid = getGlobalCentroid(centroidKey(i), i);
            centroid.init(numDimensions, parallelism);
            globalCentroidsMap.put(centroidKey(i), centroid);
        }
        
        /*
        correctCoordinates = new double[numClusters][numDimensions];
        for (int i = 0; i < numClusters; i++) {
            for (int j = 0; j < numDimensions; j++) {
                correctCoordinates[i][j] = rand.nextGaussian();
            }
        }
        sizes = new int[numClusters];
        */
    }

    private GlobalCentroid getGlobalCentroid(String key, int clusterId) {
        return factory.getInstanceOf(GlobalCentroid.class, key, false, true, clusterId);
    }

    private GlobalCentroid getGlobalCentroid(String key) {
        return factory.getInstanceOf(GlobalCentroid.class, key);
    }

    private GlobalCentroid getGlobalCentroid(int clusterId) {
        GlobalCentroid centroid = globalCentroidsMap.get(centroidKey(clusterId));
        if (centroid == null) {
            centroid = getGlobalCentroid(centroidKey(clusterId));
            globalCentroidsMap.put(centroidKey(clusterId), centroid);
        }
        return centroid;
    }

    private static String centroidKey(int clusterId) {
        return "centroid" + clusterId;
    }

    public void update(double[][] coordinates, int[] sizes) {
        for (int clusterId = 0; clusterId < numClusters; clusterId++) {
            getGlobalCentroid(clusterId).update(coordinates[clusterId], sizes[clusterId]);
        }
    }

    public double[][] getCorrectCoordinates() {
        double[][] correctCoordinates = new double[numClusters][];

        for (int clusterId = 0; clusterId < numClusters; clusterId++) {
            correctCoordinates[clusterId] = getGlobalCentroid(clusterId).getCorrectCoordinates();
        }
        return correctCoordinates;
    }

    public void println() {
        System.out.println("CENTROIDS:");
        for (int k = 0; k < numClusters; k++) {
            double[] correctCoordinates = getGlobalCentroid(k).getCorrectCoordinates();
            System.out.print("Cluster " + k + " = [" + correctCoordinates[0] + ", ..., " + correctCoordinates[correctCoordinates.length - 1] + "]");
        }
    }
}