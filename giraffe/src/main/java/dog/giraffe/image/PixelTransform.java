package dog.giraffe.image;

import dog.giraffe.Context;
import dog.giraffe.Doubles;
import dog.giraffe.points.MutablePoints;
import dog.giraffe.threads.Continuation;

public abstract class PixelTransform implements ImageTransform {
    protected final int dimensions;

    public PixelTransform(int dimensions) {
        this.dimensions=dimensions;
    }

    public static PixelTransform constNormalizedOutput(int dimensions, double value) {
        return new PixelTransform(dimensions) {
            @Override
            protected void write(MutablePoints inputLine, ImageWriter.Line outputLine, int dimension, int xx) {
                for (int dd=0; dimensions>dd; ++dd) {
                    outputLine.setNormalized(dimension+dd, xx, value);
                }
            }
        };
    }

    @Override
    public int dimensions() {
        return dimensions;
    }

    public static PixelTransform intensity(int... selectedDimensions) {
        return new PixelTransform(1) {
            @Override
            protected void write(MutablePoints inputLine, ImageWriter.Line outputLine, int dimension, int xx) {
                double value=0.0;
                for (int selectedDimension: selectedDimensions) {
                    value+=Doubles.square(inputLine.getNormalized(selectedDimension, xx));
                }
                outputLine.setNormalized(dimension, xx, Math.sqrt(value/selectedDimensions.length));
            }
        };
    }

    public static PixelTransform normalizedDifferenceVegetationIndex(int nirDimension, int redDimension) {
        return new PixelTransform(2) {
            @Override
            protected void write(MutablePoints inputLine, ImageWriter.Line outputLine, int dimension, int xx) {
                double nir=inputLine.getNormalized(nirDimension, xx);
                double red=inputLine.getNormalized(redDimension, xx);
                double value;
                if (0.0==nir+red) {
                    value=0.0;
                }
                else {
                    value=(nir-red)/(nir+red);
                }
                if (0.0<=value) {
                    outputLine.setNormalized(dimension, xx, value);
                    outputLine.setNormalized(dimension+1, xx, 0.0);
                }
                else {
                    outputLine.setNormalized(dimension, xx, 0.0);
                    outputLine.setNormalized(dimension+1, xx, -value);
                }
            }
        };
    }

    @Override
    public void prepare(ImageReader imageReader) {
    }

    @Override
    public void prepare(Context context, ImageReader imageReader, Continuation<Void> continuation) throws Throwable {
        continuation.completed(null);
    }

    @Override
    public PrepareLine prepareLine(Context context, ImageReader imageReader) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean prepareLines() {
        return false;
    }

    public static PixelTransform select(int... selectedDimensions) {
        return new PixelTransform(selectedDimensions.length) {
            @Override
            protected void write(MutablePoints inputLine, ImageWriter.Line outputLine, int dimension, int xx) {
                for (int dd=0; selectedDimensions.length>dd; ++dd) {
                    outputLine.setNormalized(
                            dimension+dd, xx, inputLine.getNormalized(selectedDimensions[dd], xx));
                }
            }
        };
    }

    @Override
    public Write write(Context context, ImageReader imageReader, ImageWriter imageWriter, int dimension) {
        return new Write() {
            @Override
            public void close() {
            }

            @Override
            public void write(
                    Context context, MutablePoints inputLine, ImageWriter.Line outputLine, int dimension)
                    throws Throwable {
                for (int xx=0; inputLine.size()>xx; ++xx) {
                    PixelTransform.this.write(inputLine, outputLine, dimension, xx);
                }
            }
        };
    }

    protected abstract void write(
            MutablePoints inputLine, ImageWriter.Line outputLine, int dimension, int xx) throws Throwable;
}
