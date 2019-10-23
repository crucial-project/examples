package crucial.examples.mandelbrot.worker;

import java.awt.*;

/**
 * @author Gerard
 */
public class Mandelbrot {

    private static final int BLACK = 0;

    public static int[] computeMandelbrot(int row, int width, int height, int maxIters) {
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
}
