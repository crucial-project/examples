package crucial.examples.logisticregression.aws.objectsCr;

import java.util.ArrayList;
import java.util.List;

public class StochasticLossHistory {

    private List<Double> lossHistory;
    private List<Long> timestamps;
    
    private double regVal;
    
    private int parallelism;
    private int count;
    
    private double lossSum;
    private int miniBatchSizeSum;
    
    public StochasticLossHistory(){
    }
    
    public void init(int parallelism) {
        this.parallelism = parallelism;
        lossHistory = new ArrayList<>();
        timestamps = new ArrayList<>();
    }

    public List<Double> getLossHistory() {
        return lossHistory;
    }

    public List<Long> getTimestamps() {
        return timestamps;
    }

    public void update(double cumLoss, int miniBatchSize) {
        if (count==0) {
            lossSum = 0.0;
            miniBatchSizeSum = 0;
        }
        count++;
        lossSum += cumLoss;
        miniBatchSizeSum += miniBatchSize;
        if (count == parallelism) {
            lossHistory.add((lossSum/miniBatchSizeSum) + regVal);
            timestamps.add(System.currentTimeMillis());
            count = 0;
        }
    }
    
    public void updateRegVal(double norm, double regParam) {
        regVal = 0.5 * regParam * norm * norm;
    }
    
}
