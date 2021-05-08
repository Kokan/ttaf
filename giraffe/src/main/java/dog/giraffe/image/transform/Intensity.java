package dog.giraffe.image.transform;

import dog.giraffe.Context;
import dog.giraffe.util.Doubles;
import dog.giraffe.image.Image;
import dog.giraffe.points.MutablePoints;
import dog.giraffe.threads.Continuation;
import java.util.Map;

/**
 * A transformation the replaces every pixel of the input image with its euclidean length.
 */
public class Intensity extends Image.Transform {
    private Intensity(Image image) {
        super(image);
    }

    /**
     * Creates a new Intensity instance.
     */
    public static Image create(Image image) {
        return new Intensity(image);
    }

    @Override
    public void log(Map<String, Object> log) {
        log.put("type", "intensity");
    }

    @Override
    protected void prepareImpl(Context context, Continuation<Dimensions> continuation) throws Throwable {
        continuation.completed(new Dimensions(1, image.height(), image.width()));
    }

    @Override
    public Reader reader() throws Throwable {
        return new TransformReader() {
            @Override
            protected void setNormalizedLineToTransform(MutablePoints points, int offset) {
                for (int xx=0; width()>xx; ++xx, ++offset) {
                    double value=0.0;
                    for (int dd=0; image.dimensions()>dd; ++dd) {
                        value+=Doubles.square(line.getNormalized(dd, xx));
                    }
                    points.setNormalized(0, offset, Math.sqrt(value/image.dimensions()));
                }
            }
        };
    }
}
