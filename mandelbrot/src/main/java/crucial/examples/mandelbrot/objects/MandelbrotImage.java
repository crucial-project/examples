package crucial.examples.mandelbrot.objects;

import java.io.Serializable;

/**
 * @author Gerard
 */
public class MandelbrotImage implements Serializable {

    private int[][] image;

    public void init(int width, int height) {
        image = new int[height][width];
    }

    /**
     * Returns a two-dimensional int array containing the color values of the Mandelbrot Set,
     * the first dimension is the height and the second dimension is the width.
     *
     * @return a two-dimensional int array containing the color values of the Mandelbrot Set.
     */
    public int[][] getImage() {
        return image;
    }

    public void setRowColor(int row, int[] rowColor) {
        image[row] = rowColor;
    }
}
