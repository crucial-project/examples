package crucial.examples.kmeans.threads.objects;

public class GlobalDelta {
    private double correctGlobalDelta = 1;
    private double tmpGlobalDelta = 0;
    private int parallelism, count = 0;
    private int numPoints;

    public GlobalDelta() {
    }

    public void init(int parallelism) {
        this.parallelism = parallelism;
        this.numPoints = 0;
    }

    public synchronized void update(double delta, int numPoints) {
        tmpGlobalDelta += delta;
        this.numPoints += numPoints;
        count++;
        if (count == parallelism) { // If it's the last thread
            correctGlobalDelta = tmpGlobalDelta / this.numPoints;
            tmpGlobalDelta = 0;
            System.out.println("globalDelta = " + correctGlobalDelta);
            count = 0;
        }
    }

    public synchronized double getGlobalDelta() {
        return correctGlobalDelta;
    }

}
