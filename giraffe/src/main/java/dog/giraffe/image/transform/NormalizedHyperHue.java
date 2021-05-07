package dog.giraffe.image.transform;

import dog.giraffe.Context;
import dog.giraffe.image.Image;
import dog.giraffe.points.FloatArrayPoints;
import dog.giraffe.points.MutablePoints;
import dog.giraffe.threads.Continuation;
import java.util.Map;

public class NormalizedHyperHue extends Image.Transform {
    private final double maxZero2;

    private NormalizedHyperHue(Image image, double maxZero) {
        super(image);
        this.maxZero2=maxZero*maxZero;
    }

    public static Image create(Image image, double maxZero) {
        return new NormalizedHyperHue(image, maxZero);
    }

    @Override
    public MutablePoints createPoints(int dimensions, int expectedSize) {
        return new FloatArrayPoints(dimensions, expectedSize);
    }

    @Override
    public void log(Map<String, Object> log) {
        log.put("type", "normalized-hyper-hue");
        log.put("max-zero^2", maxZero2);
    }

    @Override
    protected void prepareImpl(Context context, Continuation<Dimensions> continuation) throws Throwable {
        continuation.completed(new Dimensions(image));
    }

    @Override
    public Reader reader() throws Throwable {
        return new TransformReader() {
            private final double[] temp=new double[dimensions()];

            @Override
            protected void setNormalizedLineToTransform(MutablePoints points, int offset) {
                for (int xx=0; width()>xx; ++xx, ++offset) {
                    double dotProduct=0.0;
                    for (int dd=0; dimensions()>dd; ++dd) {
                        double cc=line.getNormalized(dd, xx);
                        dotProduct+=cc;
                        temp[dd]=cc;
                    }
                    dotProduct/=dimensions();
                    double length=0.0;
                    for (int dd=0; dimensions()>dd; ++dd) {
                        double cc=temp[dd]-dotProduct;
                        length+=cc*cc;
                        temp[dd]=cc;
                    }
                    if (maxZero2>length) {
                        for (int dd=0; dimensions()>dd; ++dd) {
                            points.setNormalized(dd, offset, 0.5);
                        }
                    }
                    else {
                        length=Math.sqrt(length);
                        for (int dd=0; dimensions()>dd; ++dd) {
                            points.setNormalized(dd, offset, 0.5+0.5*temp[dd]/length);
                        }
                    }
                }
            }
        };
    }
}
