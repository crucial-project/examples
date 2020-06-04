package crucial.examples.mandelbrot;

import crucial.examples.mandelbrot.objects.MandelbrotImage;
import crucial.execution.ServerlessExecutorService;
import crucial.execution.aws.AWSLambdaExecutorService;
import crucial.execution.aws.Config;
import org.infinispan.crucial.CrucialClient;
import org.infinispan.crucial.Shared;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;

/**
 * @author Gerard
 */
public class Mandelbrot implements Serializable {
    private static final int N_LAMBDAS = 10;
    private static final int ROWS = 1_000;
    private static final int COLUMNS = 1_000;
    private static final int MAX_INTERNAL_ITERATIONS = 100;
    private static final int BLACK = 0;

    //    static CrucialClient cc = CrucialClient.getClient("crucialIP:11222"); // CONFIGURE IP
    public static CrucialClient cc = CrucialClient.getClient("localhost:11222"); // CONFIGURE IP
    @Shared(key = "mandelbrotImage")
    private MandelbrotImage image = new MandelbrotImage();

//    static {
//        Config.functionName = "FunctionName-sufix";  // Change based on pom.xml
//    }

    public static void main(String[] args) {
        new Mandelbrot().doTest();
    }

    private static int[] computeMandelbrot(int row, int width, int height, int maxIters) {
        int[] rowColor = new int[width];
        int[] colors = new int[maxIters];
        for (int i = 0; i < maxIters; i++) {
            colors[i] = Color.HSBtoRGB(i / 256f, 1, i / (i + 8f));
        }

        for (int col = 0; col < width; col++) {
            double x0 = (col - width / 2.) * 4. / width;
            double y0 = (row - height / 2.) * 4. / width;
            double x = 0, y = 0;
            int iteration = 0;
            while (x * x + y * y < 4 && iteration < maxIters) {
                double xTemp = x * x - y * y + x0;
                y = 2 * x * y + y0;
                x = xTemp;
                iteration++;
            }
            if (iteration < maxIters) rowColor[col] = colors[iteration];
            else rowColor[col] = BLACK;
        }
        return rowColor;
    }

    private void doMandelbrot() {
        // Initialize shared image object
        image.init(COLUMNS, ROWS);

        ServerlessExecutorService se = new AWSLambdaExecutorService();
        se.setLocal(true);  // Comment this to run on AWS
        try {
            se.invokeIterativeTask(
                    (row) -> image.setRowColor((int) row,
                            computeMandelbrot((int) row, COLUMNS, ROWS,
                                    MAX_INTERNAL_ITERATIONS)),
                    N_LAMBDAS, 0, ROWS);
            se.shutdown();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Lambdas DONE!");
    }

    private void doTest() {
        long tIni, tEnd;

        tIni = System.currentTimeMillis();
        doMandelbrot();
        tEnd = System.currentTimeMillis();

        // Save image
        int[][] imageData = image.getImage();

        BufferedImage bufImage = new BufferedImage(COLUMNS, ROWS, BufferedImage.TYPE_INT_RGB);

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                bufImage.setRGB(col, row, imageData[row][col]);
            }
        }

        try {
            ImageIO.write(bufImage, "png", new File("./mandelbrot.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Print report
        double tSecs = (double) (tEnd - tIni) / 1000;
        System.out.println("Job done in " + tSecs + " s.\n");
    }
}
