package dog.giraffe.image.transform;

import dog.giraffe.ColorConverter;
import dog.giraffe.Context;
import dog.giraffe.image.Image;
import dog.giraffe.points.MutablePoints;
import dog.giraffe.threads.Continuation;

public class Hue extends Image.Transform {
    private Hue(Image image) {
        super(image);
    }

    public static Image create(Image image) {
        return new Hue(image);
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
            protected void setNormalizedLineToTransform(int yy, MutablePoints points, int offset) throws Throwable {
                for (int xx=0; width()>xx; ++xx, ++offset) {
                    colorConverter.rgbToHslv(
                            line.getNormalized(2, xx),
                            line.getNormalized(1, xx),
                            line.getNormalized(0, xx));
                    points.setNormalized(0, offset, colorConverter.hue/(2.0*Math.PI));
                }
            }
        };
    }
}
