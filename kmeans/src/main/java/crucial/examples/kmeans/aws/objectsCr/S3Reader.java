package crucial.examples.kmeans.aws.objectsCr;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

public class S3Reader {

    public double[][] getPoints(int workerId, int partitionPoints, int numDimensions) {
        final String S3_BUCKET = "bucketwithdataset"; // <-- TODO: CHANGE THIS
        final String fileName = "dataset-100GB-100d/part-" + String.format("%05d", workerId);
        //final String fileName = "dataset-100GB/part-" + String.format("%05d", workerId);

        System.out.println("s3BUCKET::::::: " + S3_BUCKET);

        AmazonS3 s3Client = AmazonS3ClientBuilder.standard().
                withRegion(Regions.US_EAST_1).build();

        GetObjectRequest getObjectRequest =
                new GetObjectRequest(S3_BUCKET, fileName);

        double[][] points = new double[partitionPoints][numDimensions];

        try (S3Object s3Object = s3Client.getObject(getObjectRequest)) {
            InputStream objectInput = s3Object.getObjectContent();

            BufferedReader reader = new BufferedReader(new InputStreamReader(objectInput));
            String line;
            int lines = 0;
            while ((line = reader.readLine()) != null) {
                points[lines] = Arrays.stream(line.split(","))
                        .mapToDouble(Double::parseDouble).toArray();
                lines++;
            }

            System.out.println("Dataset loaded from file " + fileName);
            System.out.println("First point: " + points[0][0] + " Last point: "
                    + points[partitionPoints - 1][numDimensions - 1]);
            System.out.println("Points loaded: " + points.length);
            for (int p = 0; p < partitionPoints; p++) {
                if (points[p].length != numDimensions) {
                    System.out.println("Worker " + workerId + " Reading ERROR: point "
                            + p + " only has " + points[p].length + " dimensions!");
                    break;
                }
            }
            reader.close();
            objectInput.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return points;
    }
}
