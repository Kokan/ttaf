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

    class RGB {
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

        public static KMeans.Distance<Color.RGB> DISTANCE
                =(center, point)->Math.sqrt(
                        Doubles.square(0.0722*(center.blue-point.blue))
                                +Doubles.square(0.7152*(center.green-point.green))
                                +Doubles.square(0.2126*(center.red-point.red)));
        public static VectorMean.Factory<Color.RGB> MEAN=new Mean.Factory(Sum.PREFERRED);

        public final double blue;
        public final double green;
        public final double red;

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
