package dog.giraffe.image.transform;

import dog.giraffe.Context;
import dog.giraffe.Lists;
import dog.giraffe.image.Image;
import dog.giraffe.points.MutablePoints;
import dog.giraffe.threads.Continuation;
import java.util.Map;

public class Constant extends Image.Transform {
    private MutablePoints line;
    private final double[] values;

    private Constant(Image image, double[] values) {
        super(image);
        this.values=values;
    }

    public static Image create(Image image, double... values) {
        return new Constant(image, values);
    }

    @Override
    public void log(Map<String, Object> log) {
        log.put("type", "constant");
        log.put("values", Lists.toList(values));
    }

    @Override
    protected void prepareImpl(Context context, Continuation<Dimensions> continuation) throws Throwable {
        line=image.createPoints(values.length, image.width());
        line.size(image.width());
        for (int xx=0; image.width()>xx; ++xx) {
            for (int dd=0; values.length>dd; ++dd) {
                line.setNormalized(dd, xx, values[dd]);
            }
        }
        continuation.completed(new Dimensions(values.length, image.height(), image.width()));
    }

    @Override
    public Reader reader() throws Throwable {
        return new Reader() {
            @Override
            public Image image() {
                return Constant.this;
            }

            @Override
            public void setNormalizedLineTo(int yy, MutablePoints points, int offset) {
                line.setNormalizedTo(0, line.size(), points, offset);
            }
        };
    }
}
