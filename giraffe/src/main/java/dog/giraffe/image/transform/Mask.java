package dog.giraffe.image.transform;

import dog.giraffe.Context;
import dog.giraffe.image.Image;
import dog.giraffe.points.MutablePoints;
import dog.giraffe.threads.Continuation;

public interface Mask {
    class ImageMask extends Image.Transform {
        private final double defaultValue;
        private final Mask mask;
        private final double[] values;
        private double[] values2;

        private ImageMask(double defaultValue, Image image, Mask mask, double[] values) {
            super(image);
            this.defaultValue=defaultValue;
            this.mask=mask;
            this.values=values;
        }

        @Override
        protected void prepareImpl(Context context, Continuation<Dimensions> continuation) throws Throwable {
            values2=new double[image.dimensions()];
            for (int dd=0; image.dimensions()>dd; ++dd) {
                values2[dd]=(values.length>dd)?values[dd]:defaultValue;
            }
            continuation.completed(new Dimensions(image));
        }

        @Override
        public Reader reader() throws Throwable {
            return new TransformReader() {
                @Override
                protected void setNormalizedLineToTransform(int yy, MutablePoints points, int offset)
                        throws Throwable {
                    reader.setNormalizedLineTo(yy, line, 0);
                    for (int xx=0; width()>xx; ++xx, ++offset) {
                        if (mask.visible(xx, yy)) {
                            for (int dd=0; dimensions()>dd; ++dd) {
                                points.setNormalized(dd, offset, line.getNormalized(dd, offset));
                            }
                        }
                        else {
                            for (int dd=0; dimensions()>dd; ++dd) {
                                points.setNormalized(dd, offset, values2[dd]);
                            }
                        }
                    }
                }
            };
        }
    }

    static Mask all() {
        return (xx, yy)->true;
    }

    static Mask and(Mask... masks) {
        return (xx, yy)->{
            for (Mask mask: masks) {
                if (!mask.visible(xx, yy)) {
                    return false;
                }
            }
            return true;
        };
    }

    static Mask halfPlane(double cx, double cy, double cc) {
        return (xx, yy)->cx*xx+cy*yy+cc>=0.0;
    }

    static Mask halfPlane(double x1, double y1, double x2, double y2) {
        double dx=x2-x1;
        double dy=y2-y1;
        return halfPlane(dy, -dx, y1*dx-x1*dy);
    }

    default Image mask(Image image, double defaultValue, double... values) {
        return new ImageMask(defaultValue, image, this, values);
    }

    boolean visible(double xx, double yy);
}
