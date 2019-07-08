package crucial.examples.kmeans.aws.objectsCr;

public class GlobalDelta {

    private double correctGlobalDelta = 1;
    private double tmpGlobalDelta;
    private int parallelism, count;
    private int numPoints;

    public GlobalDelta() {
    }

    public void init(int parallelism) {
        this.parallelism = parallelism;
        this.numPoints = 0;
        this.count = 0;
        this.tmpGlobalDelta = 0.0;
    }

    public synchronized void update(int delta, int numPoints) {
        tmpGlobalDelta += delta;
        this.numPoints += numPoints;
        count++;
        if (count == parallelism) { // If it's the last thread
            System.out.println("Last thread " + count + "/" + parallelism
                    + " updating delta... " + tmpGlobalDelta + "/" + this.numPoints
                    + "=...");
            correctGlobalDelta = tmpGlobalDelta / this.numPoints;
            System.out.println("globalDelta = " + correctGlobalDelta);
            tmpGlobalDelta = 0;
            this.numPoints = 0;
            count = 0;
        }
    }

    public synchronized double getGlobalDelta() {
        return correctGlobalDelta;
    }

}
