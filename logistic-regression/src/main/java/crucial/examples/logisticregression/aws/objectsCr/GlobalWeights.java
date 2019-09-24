package crucial.examples.logisticregression.aws.objectsCr;

import java.util.ArrayList;
import java.util.List;

import org.infinispan.crucial.Factory;

public class GlobalWeights {
    
    private Factory factory;
    private final int numFeatures;
    private final int parallelism;
    private List<CAtomicWeight> weights;
    private CAtomicWeight intercept;
    private StochasticLossHistory stochasticLossHistory;

    public GlobalWeights(int numFeatures, int parallelism) {
        factory = Factory.getSingleton();
        
        this.numFeatures = numFeatures;
        this.parallelism = parallelism;
        weights = new ArrayList<>();
    }
    
    public void init() {
        double initialValue = 0.0;  // Spark inits weights with zeros (GeneralizedLinearAlgorithm)
        for (int i = 0; i < numFeatures; i++) {
            String key = "w"+i;
            CAtomicWeight weight = factory.getInstanceOf(CAtomicWeight.class, key, false, true);
            weight.init(initialValue, parallelism);
            weights.add(weight);
        }
        if (Worker.USE_INTERCEPT) {
            intercept = factory.getInstanceOf(CAtomicWeight.class, "intercept", false, true);
            intercept.init(initialValue, parallelism);
        }
        
        stochasticLossHistory = factory.getInstanceOf(StochasticLossHistory.class, "lossHistory", false, true);
        stochasticLossHistory.init(parallelism);
    }
    
    public void localInit() {
        for (int i = 0; i < numFeatures; i++) {
            String key = "w"+i;
            weights.add(factory.getInstanceOf(CAtomicWeight.class, key));
        }
        if (Worker.USE_INTERCEPT) {
            intercept = factory.getInstanceOf(CAtomicWeight.class, "intercept");
        }
        stochasticLossHistory = factory.getInstanceOf(StochasticLossHistory.class, "lossHistory");
    }
    
    public double get(int index) {
        return weights.get(index).get();
    }
    
    public double getIntercept() {
        double interceptValue = 0.0;
        if (Worker.USE_INTERCEPT) {
            interceptValue = intercept.get();
        }
        return interceptValue;
    }
    
    public List<Double> getStochasticLossHistory(){
        return stochasticLossHistory.getLossHistory();
    }
    
    public List<Long> getStochasticLossHistoryTimestamps(){
        return stochasticLossHistory.getTimestamps();
    }
    
    public void merge(double[] cumGradient, double cumLoss, int miniBatchSize, double learningRate, double regParam, int iter) {
        for (int i = 0; i < numFeatures; i++) {
            weights.get(i).update(cumGradient[i], miniBatchSize, learningRate, regParam, iter);
        }
        if (Worker.USE_INTERCEPT) {
            intercept.update(cumGradient[numFeatures], miniBatchSize, learningRate, regParam, iter);
        }
        
        stochasticLossHistory.update(cumLoss, miniBatchSize);
    }
    
    public void updateRegVal(double norm, double regParam) {
        stochasticLossHistory.updateRegVal(norm, regParam);
    }
    
    public List<Double> toList() {
        List<Double> list = new ArrayList<>();
        for (int i = 0; i < numFeatures; i++) {
            list.add(weights.get(i).get());
        }
        return list;
    }
    
    public double[] toArray() {
        double[] array = new double[numFeatures];
        for (int i = 0; i < numFeatures; i++) {
            array[i] = weights.get(i).get();
        }
        return array;
    }
  
}
