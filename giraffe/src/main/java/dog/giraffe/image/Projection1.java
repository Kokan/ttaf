package dog.giraffe.image;

import dog.giraffe.points.FloatArrayPoints;
import dog.giraffe.points.MutablePoints;
import dog.giraffe.points.Points;
import dog.giraffe.points.Vector;

public interface Projection1 {
    abstract class Abstract implements Projection1 {
        protected final int[] selectedDimensions;
        protected final Vector vector;

        public Abstract(int[] selectedDimensions) {
            this.selectedDimensions=selectedDimensions;
            vector=new Vector(selectedDimensions.length);
        }

        @Override
        public int dimensions() {
            return selectedDimensions.length;
        }

        @Override
        public void project(Points input, MutablePoints output) {
            for (int ii=0; input.size()>ii; ++ii) {
                project(input, ii, vector);
                output.add(vector);
            }
        }
    }

    interface Factory {
        Projection1 create(ImageReader imageReader);
    }

    MutablePoints createPoints(int expectedSize);

    int dimensions();

    static Factory multidimensionalHue(int... selectedDimensions) {
        return (imageReader)->new Projection1.Abstract(selectedDimensions) {
            @Override
            public MutablePoints createPoints(int expectedSize) {
                return new FloatArrayPoints(selectedDimensions.length, expectedSize);
            }

            @Override
            public void project(Points input, int index, Vector output) {
                double dotProduct=0.0;
                for (int dd=0; selectedDimensions.length>dd; ++dd) {
                    double cc=input.getNormalized(selectedDimensions[dd], index);
                    dotProduct+=cc;
                    output.coordinate(dd, cc);
                }
                dotProduct/=selectedDimensions.length;
                for (int dd=0; selectedDimensions.length>dd; ++dd) {
                    output.coordinate(dd, 0.5+0.5*(output.coordinate(dd)-dotProduct));
                }
            }
        };
    }

    static Factory multidimensionalHueNormalized(double maxZero, int... selectedDimensions) {
        return (imageReader)->new Projection1.Abstract(selectedDimensions) {
            @Override
            public FloatArrayPoints createPoints(int expectedSize) {
                return new FloatArrayPoints(selectedDimensions.length, expectedSize);
            }

            @Override
            public void project(Points input, int index, Vector output) {
                double dotProduct=0.0;
                for (int dd=0; selectedDimensions.length>dd; ++dd) {
                    double cc=input.getNormalized(selectedDimensions[dd], index);
                    dotProduct+=cc;
                    output.coordinate(dd, cc);
                }
                dotProduct/=selectedDimensions.length;
                double length=0.0;
                for (int dd=0; selectedDimensions.length>dd; ++dd) {
                    double cc=output.coordinate(dd)-dotProduct;
                    length+=cc*cc;
                    output.coordinate(dd, cc);
                }
                length=Math.sqrt(length);
                if (maxZero>length) {
                    for (int dd=0; selectedDimensions.length>dd; ++dd) {
                        output.coordinate(dd, 0.5);
                    }
                }
                else {
                    for (int dd=0; selectedDimensions.length>dd; ++dd) {
                        output.coordinate(dd, 0.5+0.5*output.coordinate(dd)/length);
                    }
                }
            }
        };
    }

    void project(Points input, MutablePoints output);

    void project(Points input, int index, Vector output);

    static Factory select(int... selectedDimensions) {
        return (imageReader)->new Projection1.Abstract(selectedDimensions) {
            @Override
            public MutablePoints createPoints(int expectedSize) {
                return imageReader.createPoints(selectedDimensions.length, expectedSize);
            }

            @Override
            public void project(Points input, int index, Vector output) {
                for (int dd=0; selectedDimensions.length>dd; ++dd) {
                    output.coordinate(dd, input.get(selectedDimensions[dd], index));
                }
            }
        };
    }
}
