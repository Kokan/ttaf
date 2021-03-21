package dog.giraffe;

import java.util.Objects;

public interface Color {
    class Grayscale {
        public final double luminance;

        public Grayscale(double luminance) {
            Doubles.checkFinite(luminance);
            this.luminance=luminance;
        }

        @Override
        public boolean equals(Object obj) {
            if (this==obj) {
                return true;
            }
            if ((null==obj)
                    || (!getClass().equals(obj.getClass()))) {
                return false;
            }
            Grayscale color=(Grayscale)obj;
            return (luminance==color.luminance);
        }

        @Override
        public int hashCode() {
            return Double.hashCode(luminance);
        }

        @Override
        public String toString() {
            return "Grayscale(luminance: "+luminance+")";
        }
    }

    class HSL {
        public final double hue;
        public final double lightness;
        public final double saturation;

        public HSL(double hue, double lightness, double saturation) {
            Doubles.checkFinite(hue);
            Doubles.checkFinite(lightness);
            Doubles.checkFinite(saturation);
            this.hue=hue;
            this.lightness=lightness;
            this.saturation=saturation;
        }

        @Override
        public boolean equals(Object obj) {
            if (this==obj) {
                return true;
            }
            if ((null==obj)
                    || (!getClass().equals(obj.getClass()))) {
                return false;
            }
            HSL color=(HSL)obj;
            return (hue==color.hue)
                    && (lightness==color.lightness)
                    && (saturation==color.saturation);
        }

        @Override
        public int hashCode() {
            return Double.hashCode(hue)
                    +19*Double.hashCode(lightness)
                    +41*Double.hashCode(saturation);
        }

        @Override
        public String toString() {
            return "HSV(hue: "+hue+", lightness: "+lightness+", saturation: "+saturation+")";
        }
    }

    class HSV {
        public final double hue;
        public final double saturation;
        public final double value;

        public HSV(double hue, double saturation, double value) {
            Doubles.checkFinite(hue);
            Doubles.checkFinite(saturation);
            Doubles.checkFinite(value);
            this.hue=hue;
            this.saturation=saturation;
            this.value=value;
        }

        @Override
        public boolean equals(Object obj) {
            if (this==obj) {
                return true;
            }
            if ((null==obj)
                    || (!getClass().equals(obj.getClass()))) {
                return false;
            }
            HSV color=(HSV)obj;
            return (hue==color.hue)
                    && (saturation==color.saturation)
                    && (value==color.value);
        }

        @Override
        public int hashCode() {
            return Double.hashCode(hue)
                    +19*Double.hashCode(saturation)
                    +41*Double.hashCode(value);
        }

        @Override
        public String toString() {
            return "HSV(hue: "+hue+", saturation: "+saturation+", value: "+value+")";
        }
    }

    class RGB implements Arith<RGB> {
        public static class Mean implements VectorMean<Color.RGB> {
            public static class Factory implements VectorMean.Factory<Color.RGB> {
                private final Sum.Factory sumFactory;

                public Factory(Sum.Factory sumFactory) {
                    this.sumFactory=sumFactory;
                }

                @Override
                public VectorMean<RGB> create(int expectedAddends) {
                    return new Mean(expectedAddends, sumFactory);
                }
            }

            private int addends;
            private final Sum blue;
            private final Sum green;
            private final Sum red;

            public Mean(int expectedAddends, Sum.Factory sumFactory) {
                this.blue=sumFactory.create(expectedAddends);
                this.green=sumFactory.create(expectedAddends);
                this.red=sumFactory.create(expectedAddends);
            }

            @Override
            public VectorMean<RGB> add(RGB addend) {
                Objects.requireNonNull(addend);
                ++addends;
                blue.add(addend.blue);
                green.add(addend.green);
                red.add(addend.red);
                return this;
            }

            @Override
            public VectorMean<RGB> clear() {
                addends=0;
                blue.clear();
                green.clear();
                red.clear();
                return this;
            }

            @Override
            public RGB mean() {
                double blue2=blue.sum();
                double green2=green.sum();
                double red2=red.sum();
                return new RGB(blue2/addends, green2/addends, red2/addends);
            }
        }

        public static class StdDeviation implements VectorStdDeviation<Color.RGB> {
            public static class Factory implements VectorStdDeviation.Factory<Color.RGB> {
                private final VectorMean.Factory<Color.RGB> meanFactory;

                public Factory(VectorMean.Factory<Color.RGB> meanFactory) {
                     this.meanFactory = meanFactory;
                }

                @Override
                public VectorStdDeviation<RGB> create(RGB mean, int addends) {
                       return new StdDeviation(mean, addends, meanFactory);
                }
            }

            private int addends;
            private final RGB meanValue;
            private final VectorMean<Color.RGB> mean;

            public StdDeviation(RGB mean, int addends, VectorMean.Factory<Color.RGB> meanFactory) {
              this.addends = 0;
              this.meanValue = mean;
              this.mean = meanFactory.create();
            }

            @Override
            public VectorStdDeviation<RGB> add(RGB addend) {
                ++addends;
                mean.add(addend.subtract(meanValue).pow());
                return this;
            }

            @Override
            public VectorStdDeviation<RGB> clear() {
                mean.clear();
                addends=0;
                return this;
            }

            @Override
            public Color.RGB mean() {
                return meanValue.sqrt();
            }

            @Override
            public Color.RGB deviation() {
                if (addends==0) return new RGB(0,0,0);
                return mean.mean().div(addends).sqrt(); //Sqrt(mean.mean())
            }
        }

        public static class Comp implements MaxComponent<Double, Color.RGB> {
           @Override
           public Double max(RGB self) { return Math.max(self.blue, Math.max(self.green, self.red)); }

           @Override
           public RGB maxVec(RGB self) {
              Double max = max(self);

              return new RGB(self.blue==max ? max : 0,self.green==max ? max : 0,self.red==max ? max : 0);
           }
        }

        public static final Distance<Color.RGB> DISTANCE
                =(center, point)->Math.sqrt(
                        Doubles.square(0.0722*(center.blue-point.blue))
                                +Doubles.square(0.7152*(center.green-point.green))
                                +Doubles.square(0.2126*(center.red-point.red)));
        public static final VectorMean.Factory<Color.RGB> MEAN=new Mean.Factory(Sum.PREFERRED);
        public static final VectorStdDeviation.Factory<Color.RGB> DEV=new StdDeviation.Factory(MEAN);
        public static final Comp MAX=new Comp();

        public final double blue;
        public final double green;
        public final double red;

        @Override
        public RGB addition(RGB other) {
            return new RGB(blue + other.blue, green + other.green, red + other.red);
        }

        @Override
        public RGB subtract(RGB other) {
            return new RGB(blue - other.blue, green - other.green, red - other.red);
        }

        @Override
        public RGB pow() {
            return new RGB(Math.pow(blue,2), Math.pow(green,2), Math.pow(red, 2));
        }

        @Override
        public RGB sqrt() {
            return new RGB(Math.sqrt(blue), Math.sqrt(green), Math.sqrt(red));
        }

        @Override
        public RGB mul(double multiplier) {
            return new RGB(blue * multiplier, green * multiplier, red * multiplier);
        }

        @Override
        public RGB div(double divisor) {
            return new RGB(blue / divisor, green / divisor, red / divisor);
        }

        public RGB(double blue, double green, double red) {
            Doubles.checkFinite(blue);
            Doubles.checkFinite(green);
            Doubles.checkFinite(red);
            this.blue=blue;
            this.green=green;
            this.red=red;
        }

        public static RGB createFromRGB(int rgb, double divisor) {
            int blue=rgb&0xff;
            int green=(rgb>>>8)&0xff;
            int red=(rgb>>>16)&0xff;
            return new RGB(blue/divisor, green/divisor, red/divisor);
        }

        @Override
        public boolean equals(Object obj) {
            if (this==obj) {
                return true;
            }
            if ((null==obj)
                    || (!getClass().equals(obj.getClass()))) {
                return false;
            }
            RGB color=(RGB)obj;
            return (blue==color.blue)
                    && (green==color.green)
                    && (red==color.red);
        }

        @Override
        public int hashCode() {
            return Double.hashCode(blue)
                    +19*Double.hashCode(green)
                    +41*Double.hashCode(red);
        }

        public int toARGBInt(double multiplier) {
            int blue2=(int)Math.max(0.0, Math.min(255.0, blue*multiplier));
            int green2=(int)Math.max(0.0, Math.min(255.0, green*multiplier));
            int red2=(int)Math.max(0.0, Math.min(255.0, red*multiplier));
            return 0xff000000|(red2<<16)|(green2<<8)|blue2;
        }

        @Override
        public String toString() {
            return "RGB(blue: "+blue+", green: "+green+", red: "+red+")";
        }
    }

    interface Visitor<R> {
        R grayscale(Grayscale color);
        R hsl(HSL color);
        R hsv(HSV color);
        R rgb(RGB color);
    }

    <R> R visit(Visitor<R> visitor);
}
