package crucial.examples.logisticregression.aws.objectsCr;

import java.io.Serializable;

public class CAtomicWeight implements Serializable{
    private static final long serialVersionUID = 289252302213866527L;

    private double value = 0.0;
    
    private int parallelism;
    private int count;
    
    
    private double gradientSum;
    private int miniBatchSizeSum;
    
    public CAtomicWeight(){
    }
    
    public void init(double initialValue, int parallelism) {
        value = initialValue;
        this.parallelism = parallelism;
        count = 0;
    }
    
    public void printValue(){
        System.out.println(value);
    }
    
    public double get(){
        return value;
    }
    
    public void update(double cumGradient, int miniBatchSize, double learningRate, double regParam, int iter){
        if (count==0) {
            gradientSum = 0.0;
            miniBatchSizeSum = 0;
        }
        count++;
        gradientSum += cumGradient;
        miniBatchSizeSum += miniBatchSize;
        //System.out.println(miniBatchSize + " " + iter + " " + count + " " + parallelism);
        if (count == parallelism) {
            double thisIterStepSize = learningRate / Math.sqrt(iter);
            value = value * (1 - thisIterStepSize * regParam);
            value = value - thisIterStepSize * (gradientSum / miniBatchSizeSum) ;
            count = 0;
            //System.out.println(super.toString() + " " + miniBatchSizeSum + " " + gradientSum + " " + (gradientSum / miniBatchSizeSum) + " " + thisIterStepSize);
        }
    }
    
    
    /**
     * Returns the String representation of the current value.
     *
     * @return the String representation of the current value
     */
    public String toString(){
        return Double.toString(get());
    }
    
}
