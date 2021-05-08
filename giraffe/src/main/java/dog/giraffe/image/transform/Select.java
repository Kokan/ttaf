package dog.giraffe.image.transform;

import dog.giraffe.Context;
import dog.giraffe.util.Lists;
import dog.giraffe.image.Image;
import dog.giraffe.points.MutablePoints;
import dog.giraffe.threads.Continuation;
import java.util.Map;

/**
 * A transformation which select components of an input image.
 */
public class Select extends Image.Transform {
    private final int[] selectedDimensions;

    private Select(Image image, int[] selectedDimensions) {
        super(image);
        this.selectedDimensions=selectedDimensions;
    }

    /**
     * Creates a new image which contains the selectedDimensions components of the image parameter.
     */
    public static Image create(Image image, int... selectedDimensions) {
        return new Select(image, selectedDimensions);
    }

    @Override
    public void log(Map<String, Object> log) {
        log.put("type", "select");
        log.put("selected-dimensions", Lists.toList(selectedDimensions));
    }

    @Override
    protected void prepareImpl(Context context, Continuation<Dimensions> continuation) throws Throwable {
        if (image.dimensions()<selectedDimensions.length) {
            throw new RuntimeException(String.format(
                    "not enough dimensions. image: %1$d, selected: %2$d",
                    image.dimensions(), selectedDimensions.length));
        }
        continuation.completed(new Dimensions(selectedDimensions.length, image.height(), image.width()));
    }

    @Override
    public Reader reader() throws Throwable {
        return new TransformReader() {
            @Override
            protected void setNormalizedLineToTransform(MutablePoints points, int offset) {
                for (int xx=0; image.width()>xx; ++xx, ++offset) {
                    for (int dd=0; selectedDimensions.length>dd; ++dd) {
                        points.setNormalized(dd, offset, line.getNormalized(selectedDimensions[dd], xx));
                    }
                }
            }
        };
    }
}
