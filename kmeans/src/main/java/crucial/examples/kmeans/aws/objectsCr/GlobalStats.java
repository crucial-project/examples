package crucial.examples.kmeans.aws.objectsCr;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GlobalStats {

    private double[] iterationTimes;

    private Map<Integer, List<Long>> breakdown;

    public GlobalStats() {
    }

    public void init(int parallelism) {
        iterationTimes = new double[parallelism];

        breakdown = new HashMap<>();
    }

    public void update(int workerId, double iterationTime) {
        iterationTimes[workerId] = iterationTime;
    }

    public String getStats() {
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append(Arrays.toString(iterationTimes));
        strBuilder.append("\n");
        double min = Arrays.stream(iterationTimes).min().getAsDouble();
        double average = Arrays.stream(iterationTimes).average().getAsDouble();
        double max = Arrays.stream(iterationTimes).max().getAsDouble();
        strBuilder.append("Min/Avg/Max: ").append(min).append(" / ")
                .append(average).append(" / ").append(max).append("\n");

        return strBuilder.toString();
    }

    public void updateBreakdown(int workerId, List<Long> breakdown) {
        this.breakdown.put(workerId, breakdown);
    }

    public Map<Integer, List<Long>> getBreakdownStats() {
        return breakdown;
    }
}
