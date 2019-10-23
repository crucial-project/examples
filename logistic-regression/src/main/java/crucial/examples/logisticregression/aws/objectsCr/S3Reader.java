package crucial.examples.logisticregression.aws.objectsCr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

public class S3Reader {

    private static int MAX_RETRIES = 3;

    public void loadData(int workerId, String filePrefix, double[][] samples, double[] labels) {
        final String S3_BUCKET = "bucketwithdataset"; // <-- TODO: CHANGE THIS
        final String fileName = filePrefix + "/part-" + String.format("%05d", workerId);

        System.out.println("Loading data from " + S3_BUCKET + " " + fileName);

        AmazonS3 s3Client = AmazonS3ClientBuilder.standard().
                withRegion(Regions.US_EAST_1).build();

        GetObjectRequest getObjectRequest =
                new GetObjectRequest(S3_BUCKET, fileName);

        int retries = 0;
        while (true) {
            try (S3Object s3Object = s3Client.getObject(getObjectRequest)) {

                InputStream objectInput = s3Object.getObjectContent();

                BufferedReader reader = new BufferedReader(new InputStreamReader(objectInput));
                String line;
                int lines = 0;
                while ((line = reader.readLine()) != null) {

                    double[] values = Arrays.stream(line.split(",")).mapToDouble(Double::parseDouble).toArray();

                    // spark-perf dataset
                    labels[lines] = values[values.length - 1];
                    samples[lines] = new double[values.length - 1];
                    System.arraycopy(values, 0, samples[lines], 0, values.length - 1);
                    // END

                    lines++;
                }

                System.out.println("Loaded " + lines + " samples.");

                reader.close();
                objectInput.close();

                return;
            } catch (IOException e) {
                retries++;
                if (retries >= MAX_RETRIES) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }

                System.out.println("Exception thrown while reading S3 file. Retrying...");
                try {
                    Thread.sleep(1000);  // 1s
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

}
