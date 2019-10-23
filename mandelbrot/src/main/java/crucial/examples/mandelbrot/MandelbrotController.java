package crucial.examples.mandelbrot;

import crucial.execution.ServerlessExecutorService;
import crucial.execution.aws.AWSLambdaExecutorService;
import crucial.examples.mandelbrot.objects.MandelbrotImage;
import crucial.examples.mandelbrot.objects.MandelbrotInitData;
import org.infinispan.crucial.CrucialClient;
import org.infinispan.crucial.Shared;
import crucial.examples.mandelbrot.worker.Mandelbrot;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * @author Gerard
 */
public class MandelbrotController {
    private static final int N_LAMBDAS = 10;
    private static final int ROWS = 1_000;
    private static final int COLUMNS = 1_000;
    private static final int MAX_INTERNAL_ITERATIONS = 100;

//    static CrucialClient cc = CrucialClient.getClient("crucialIP:11222"); // CONFIGURE IP
    public static CrucialClient cc = CrucialClient.getClient("localhost:11222"); // CONFIGURE IP

    @Shared(key = "mandelbrotInitData")
    private MandelbrotInitData initData = new MandelbrotInitData();

    @Shared(key = "mandelbrotImage")
    private MandelbrotImage image = new MandelbrotImage();

    public static void main(String[] args) {
        new MandelbrotController().doTest();
    }

    private void doMandelbrot() {
        // Initialize shared crucial.examples.mandelbrot.objects
        initData.init(COLUMNS, ROWS, MAX_INTERNAL_ITERATIONS);
        image.init(COLUMNS, ROWS);

        ServerlessExecutorService se = new AWSLambdaExecutorService();
        se.setLocal(true);
        try {
            se.invokeIterativeTask(new Mandelbrot(), N_LAMBDAS, 0, ROWS);
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
        System.out.println("Job done in " + tSecs + "s.\n");
    }
}
