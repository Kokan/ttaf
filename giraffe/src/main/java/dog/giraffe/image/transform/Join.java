package dog.giraffe.image.transform;

import dog.giraffe.Context;
import dog.giraffe.image.Image;
import dog.giraffe.points.FloatArrayPoints;
import dog.giraffe.points.MutablePoints;
import dog.giraffe.threads.Continuation;
import java.util.List;
import java.util.Map;

public class Join extends Image.Abstract {
    private final Image[] images;

    private Join(Image[] images) {
        super(List.of(images));
        this.images=images;
    }

    public static Image create(Image... images) {
        return new Join(images);
    }

    @Override
    public MutablePoints createPoints(int dimensions, int expectedSize) {
        return new FloatArrayPoints(dimensions, expectedSize);
    }

    @Override
    public void log(Map<String, Object> log) {
        log.put("type", "join");
    }

    @Override
    protected void prepareImpl(Context context, Continuation<Dimensions> continuation) throws Throwable {
        int width=Integer.MAX_VALUE;
        int height=Integer.MAX_VALUE;
        int dimensions=0;
        for (Image image: images) {
            width=Math.min(width, image.width());
            height=Math.min(width, image.height());
            dimensions+=image.dimensions();
        }
        continuation.completed(new Dimensions(dimensions, height, width));
    }

    @Override
    public Reader reader() throws Throwable {
        MutablePoints[] lines=new MutablePoints[images.length];
        Reader[] readers=new Reader[images.length];
        for (int ii=0; images.length>ii; ++ii) {
            lines[ii]=images[ii].createPoints(images[ii].dimensions(), images[ii].width());
            lines[ii].size(images[ii].width());
            readers[ii]=images[ii].reader();
        }
        return new Reader() {
            @Override
            public Image image() {
                return Join.this;
            }

            @Override
            public void setNormalizedLineTo(int yy, MutablePoints points, int offset) throws Throwable {
                for (int ii=0; images.length>ii; ++ii) {
                    readers[ii].setNormalizedLineTo(yy, lines[ii], 0);
                }
                for (int xx=0; width()>xx; ++xx, ++offset) {
                    for (int de=0, ii=0; images.length>ii; ++ii) {
                        for (int dd=0; images[ii].dimensions()>dd; ++dd, ++de) {
                            points.setNormalized(de, offset, lines[ii].getNormalized(dd, xx));
                        }
                    }
                }
            }
        };
    }
}
