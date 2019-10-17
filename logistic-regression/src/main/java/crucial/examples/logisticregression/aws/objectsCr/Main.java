package crucial.examples.logisticregression.aws.objectsCr;

import java.util.ArrayList;

import crucial.execution.aws.AWSLambdaThread;
import org.infinispan.crucial.CCyclicBarrier;
import org.infinispan.crucial.CrucialClient;
import org.infinispan.crucial.Shared;


public class Main {
    static CrucialClient cc = CrucialClient.getClient("crucialIP:11222");
    
    private static final int DEFAULT_ITERATIONS = 100;
    private static final int DEFAULT_PARALLELISM = 80;
    private static final int DEFAULT_NUM_FEATURES = 9;
    
    @Shared(key = "stats")
    private GlobalStats globalStats = new GlobalStats();
    
    private CCyclicBarrier barrier;
    
    private GlobalWeights globalWeights;
    
    private final int numberOfIterations;
    private final int parallelism;
    private final int numFeatures;
    private final String filePrefix;

    public Main(int numberOfIterations, int parallelism, int numFeatures, String filePrefix) {
        this.numberOfIterations = numberOfIterations;
        this.parallelism = parallelism;
        this.numFeatures = numFeatures;
        this.filePrefix = filePrefix;
    }

    public static void main(String[] args) {
        Main main;
        if (args.length == 4) {
            main = new Main(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]), args[3]);
        } else {
            main = new Main(DEFAULT_ITERATIONS, DEFAULT_PARALLELISM, DEFAULT_NUM_FEATURES, "fileprefix");
        }
        main.doMain();
    }

    public void doMain() {
        
        // Initializing shared data
        globalWeights = new GlobalWeights(numFeatures, parallelism);
        globalWeights.init();
        globalStats.init(parallelism);
        barrier = cc.getCyclicBarrier("barrier", parallelism);
        System.out.println("Configured barrier with " + barrier.getParties() + " parties"); // do not comment this line: useful to ensure barrier object is created in Crucial
        

        ArrayList<Thread> threads = new ArrayList<>();

        long initTime = System.currentTimeMillis();
        for (int w = 0; w < parallelism; w++) {
            threads.add(new AWSLambdaThread(new Worker(w, numberOfIterations, parallelism, numFeatures, filePrefix)));
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

        System.out.println(globalStats.getStats());
        
        System.out.println("Elapsed time: " + (endTime - initTime) / 1000.0 + " s");
        
        // Test
        /*
        double[] modelWeights = globalWeights.toArray();
        double modelIntercept = globalWeights.getIntercept();
        
        
        List<List<Double>> testSamples = new ArrayList<>();
        List<Double> testLabels = new ArrayList<>();
        
        
        S3Reader s3Reader = new S3Reader();
        s3Reader.loadData(0, filePrefix, testSamples, testLabels);
        int correct = 0;
        for (int i=0; i< testSamples.size();i++) {
            double prediction = Worker.predict(testSamples.get(i).stream().mapToDouble(Double::doubleValue).toArray(), 
                    modelWeights, modelIntercept);
            double label = testLabels.get(i);
            //System.out.println("Prediction=" + prediction + " label="+label);
            if ((prediction<0.5 && label==0.0) || (prediction>=0.5 && label==1.0)) {
                correct++;
            }
        }
        System.out.println("Test: " + testSamples.size() + " samples [" + correct + " correct = " + (correct * 100.0)/testSamples.size() + "%]");
        
        System.out.println("Stochastic Loss History:");
        System.out.println(globalWeights.getStochasticLossHistory());
        System.out.println("Stochastic Loss History timestamps:");
        System.out.println(globalWeights.getStochasticLossHistoryTimestamps());
        */
        

    }
}
