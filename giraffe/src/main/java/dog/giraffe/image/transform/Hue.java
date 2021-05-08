package dog.giraffe.image.transform;

import dog.giraffe.Context;
import dog.giraffe.image.Image;
import dog.giraffe.points.MutablePoints;
import dog.giraffe.threads.Continuation;
import dog.giraffe.util.ColorConverter;
import java.util.Map;

/**
 * Hue transforms an RGB image to the HSV color space and selects the hue components as its sole output component.
 */
public class Hue extends Image.Transform {
    private Hue(Image image) {
        super(image);
    }

    /**
     * Creates a new {@link Hue} instance.
     */
    public static Image create(Image image) {
        return new Hue(image);
    }

    @Override
    public void log(Map<String, Object> log) {
        log.put("type", "hue");
    }

    @Override
    protected void prepareImpl(Context context, Continuation<Dimensions> continuation) throws Throwable {
        if (image.dimensions()<3) {
            throw new RuntimeException(String.format(
                    "not enough dimensions. image: %1$d, selected: 3",
                    image.dimensions()));
        }
        continuation.completed(new Dimensions(1, image.height(), image.width()));
    }

    @Override
    public Reader reader() throws Throwable {
        return new TransformReader() {
            private final ColorConverter colorConverter=new ColorConverter();

            @Override
            protected void setNormalizedLineToTransform(MutablePoints points, int offset) {
                for (int xx=0; width()>xx; ++xx, ++offset) {
                    colorConverter.rgbToHsvAndHsl(
                            line.getNormalized(2, xx),
                            line.getNormalized(1, xx),
                            line.getNormalized(0, xx));
                    points.setNormalized(0, offset, colorConverter.hue/(2.0*Math.PI));
                }
            }
        };
    }
}
