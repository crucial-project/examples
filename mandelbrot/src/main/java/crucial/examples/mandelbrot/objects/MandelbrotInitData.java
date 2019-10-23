package crucial.examples.mandelbrot.objects;

import java.io.Serializable;

/**
 * Utility class that encapsulates initialization data.
 *
 * @author Gerard
 */
public class MandelbrotInitData implements Serializable {
    private int width;
    private int height;
    private int maxInternalIterations;

    public void init(int width, int height, int maxInternalIterations) {
        this.width = width;
        this.height = height;
        this.maxInternalIterations = maxInternalIterations;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getMaxInternalIterations() {
        return maxInternalIterations;
    }
}
