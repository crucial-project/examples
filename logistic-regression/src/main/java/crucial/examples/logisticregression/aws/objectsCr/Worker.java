package crucial.examples.logisticregression.aws.objectsCr;

import org.infinispan.crucial.CCyclicBarrier;
import org.infinispan.crucial.Shared;


public class Worker implements Runnable {
    
    private static final double LEARNING_RATE = 1;
    private static final double REG_PARAM = 0.0; //0.01;
    public static final boolean USE_INTERCEPT = false;
    private static final int DATASET_FRAGMENT_SIZE = 695866;

    private int workerId;
    private int maxIterations;
    private int parallelism;
    private int numFeatures;
    private String filePrefix;
    
    private CCyclicBarrier barrier = Main.cc.getCyclicBarrier("barrier");
    private GlobalWeights globalWeights;
    
    @Shared(key = "stats")
    private GlobalStats globalStats = new GlobalStats();
    
    private double[][] samples;
    private double[] labels;
    
    private double[] localWeights;
    private double localIntercept;
    
    public Worker() {
        
    }
    
    public Worker(int workerId, int maxIterations, int parallelism, int numFeatures, String filePrefix) {
      this.workerId = workerId;
      this.maxIterations = maxIterations;
      this.parallelism = parallelism;
      this.numFeatures = numFeatures;
      this.filePrefix = filePrefix;
    }
    
    @Override
    public void run() {
        
        globalWeights = new GlobalWeights(numFeatures, parallelism);
        globalWeights.localInit();
        
        // load data
        loadDataset();
        
        localWeights = new double[numFeatures];
        
        // barrier before starting iterations, to avoid different execution times
        barrier.await();
        
        int gradientSize = numFeatures;
        if (USE_INTERCEPT) {
            gradientSize = numFeatures + 1;
        }
        
        long initTime = System.currentTimeMillis();
        double[] cumGradient = new double[gradientSize];
        double cumLoss;
        
        for (int w = 0; w < numFeatures; w++) {
            localWeights[w] = globalWeights.get(w);
        }
        localIntercept = globalWeights.getIntercept();
        if (workerId==0) {
            globalWeights.updateRegVal(norm(localWeights, localIntercept), REG_PARAM);
        }
        
        for (int i = 0; i < maxIterations; i++) {
            
            for (int f = 0; f < gradientSize; f++) {
                cumGradient[f] = 0.0; // includes intercept as the last element if USE_INTERCEPT==true
            }
            cumLoss = 0.0;
            
            int miniBatchSize = 0;
            for (int s=0;s < samples.length; s++) {  // TODO: implement minibatch selection.
                //int index = rand.nextInt(DATASET_FRAGMENT_SIZE);
                double margin = -1.0 * dot(samples[s], localWeights, localIntercept);
                double multiplier = (1.0 / (1.0 + Math.exp(margin))) - labels[s];
                
                for (int fi = 0; fi < numFeatures; fi++) {
                    cumGradient[fi] += multiplier * samples[s][fi];
                }
                if (USE_INTERCEPT) {
                    cumGradient[numFeatures] += multiplier;
                }
                if (labels[s] > 0) {
                    cumLoss += log1pExp(margin);
                } else {
                    cumLoss += log1pExp(margin) - margin;
                }
                miniBatchSize++;
            }
            //System.out.println("END ITER crucial.examples.mandelbrot.worker " + workerId + " iteration: " + i);
            globalWeights.merge(cumGradient, cumLoss, miniBatchSize, LEARNING_RATE, REG_PARAM, i+1);
            
            barrier.await();
            
            // Get new weights
            for (int w = 0; w < numFeatures; w++) {
                localWeights[w] = globalWeights.get(w);
            }
            localIntercept = globalWeights.getIntercept();
            
            if (workerId == 0) {
                // crucial.examples.mandelbrot.worker in charge of updating regVal
                double norm = norm(localWeights, localIntercept);
                globalWeights.updateRegVal(norm, REG_PARAM);
            }
        }
        long endTime = System.currentTimeMillis();
        
        double iterationTime = (endTime - initTime) / 1000.0;
        System.out.println(maxIterations + " iterations in " + iterationTime + " s");
        globalStats.update(workerId, iterationTime);
        
    }
    
    

    private void loadDataset() {   
        samples = new double[DATASET_FRAGMENT_SIZE][];
        labels = new double[DATASET_FRAGMENT_SIZE];
        
        S3Reader s3Reader = new S3Reader();
        s3Reader.loadData(workerId, filePrefix, samples, labels);
    }
    
    private static double dot(double[] data, double[] weights, double intercept) {
        double result = 0.0;
        if (USE_INTERCEPT) {
            result += intercept;
        }
        for (int i = 0; i < weights.length; i++) {
            result += weights[i] * data[i];
        }
        return result;
    }
    
    public static double predict(double[] sample, double[] weights, double intercept) {
        double z = dot(sample, weights, intercept);
        return 1.0 / (1.0 + Math.exp(-z));
    }
    
    private double log1pExp(double x) {
        double ret;
        if (x > 0) {
            ret = x + Math.log1p(Math.exp(-x));
        } else {
            ret = Math.log1p(Math.exp(x));
        }
        return ret;
    }
    
    private double norm(double[] weights, double intercept) {
        double ret = 0.0;
        for (double weight : weights) {
            ret += weight * weight;
        }
        if (USE_INTERCEPT) {
            ret += intercept * intercept;
        }
        return Math.sqrt(ret);
    }
    
    private String toStringPartial(double[] cumGradient) {
        int size = cumGradient.length;
        return "[" + cumGradient[0] + ", " + cumGradient[1] + ", ..., " + cumGradient[size-2] + ", " + cumGradient[size-1] + "]"; 
    }

}
