package dog.giraffe;

import java.util.Objects;

public interface Color {
    class Converter {
        double blue;
        double green;
        double hue;
        double lightness;
        double red;
        double saturationLightness;
        double saturationValue;
        double value;

        private void check(double max, double min, double value) {
            if ((!Double.isFinite(value))
                    || (max<value)
                    || (min>value)) {
                throw new IllegalStateException(Double.toString(value));
            }
        }

        public void hslToRgb(double hue, double lightness, double saturationLightness) {
            check(2.0*Math.PI, 0.0, hue);
            check(1.0, 0.0, lightness);
            check(1.0, 0.0, saturationLightness);
            double cc=(1.0-Math.abs(2.0*lightness-1.0))*saturationLightness;
            double hue2=3.0*hue/Math.PI;
            double xx=cc*(1.0-Math.abs((hue2%2.0)-1.0));
            blue=0.0;
            green=0.0;
            red=0.0;
            switch ((int)hue2) {
                case 0:
                    green=xx;
                    red=cc;
                    break;
                case 1:
                    green=cc;
                    red=xx;
                    break;
                case 2:
                    blue=xx;
                    green=cc;
                    break;
                case 3:
                    blue=cc;
                    green=xx;
                    break;
                case 4:
                    blue=cc;
                    red=xx;
                    break;
                case 5:
                    blue=xx;
                    red=cc;
                    break;
            }
            double mm=lightness-0.5*cc;
            blue+=mm;
            green+=mm;
            red+=mm;
            check(1.0, 0.0, blue);
            check(1.0, 0.0, green);
            check(1.0, 0.0, red);
        }

        public void hsvToRgb(double hue, double saturationValue, double value) {
            check(2.0*Math.PI, 0.0, hue);
            check(1.0, 0.0, saturationValue);
            check(1.0, 0.0, value);
            double cc=saturationValue*value;
            double hue2=3.0*hue/Math.PI;
            double xx=cc*(1.0-Math.abs((hue2%2.0)-1.0));
            blue=0.0;
            green=0.0;
            red=0.0;
            switch ((int)hue2) {
                case 0:
                    green=xx;
                    red=cc;
                    break;
                case 1:
                    green=cc;
                    red=xx;
                    break;
                case 2:
                    blue=xx;
                    green=cc;
                    break;
                case 3:
                    blue=cc;
                    green=xx;
                    break;
                case 4:
                    blue=cc;
                    red=xx;
                    break;
                case 5:
                    blue=xx;
                    red=cc;
                    break;
            }
            double mm=value-cc;
            blue+=mm;
            green+=mm;
            red+=mm;
            check(1.0, 0.0, blue);
            check(1.0, 0.0, green);
            check(1.0, 0.0, red);
        }

        public void rgbToHslv(int rgb) {
            rgbToHslv(
                    (rgb&0xff)/255.0,
                    ((rgb>>8)&0xff)/255.0,
                    ((rgb>>16)&0xff)/255.0);
        }

        public void rgbToHslv(double blue, double green, double red) {
            check(1.0, 0.0, blue);
            check(1.0, 0.0, green);
            check(1.0, 0.0, red);
            value=Math.max(blue, Math.max(green, red));
            double min=Math.min(blue, Math.min(green, red));
            lightness=0.5*(value+min);
            double cc=value-min;
            if (0.0>=cc) {
                hue=0.0;
            }
            else if (value==red) {
                hue=(green-blue)*Math.PI/(3.0*cc);
                if (0.0>hue) {
                    hue+=2.0*Math.PI;
                }
            }
            else if (value==green) {
                hue=(2.0*cc+blue-red)*Math.PI/(3.0*cc);
            }
            else {
                hue=(4.0*cc+red-green)*Math.PI/(3.0*cc);
            }
            check(2.0*Math.PI, 0.0, hue);
            if ((0.0>=lightness)
                    || (1.0<=lightness)) {
                saturationLightness=0.0;
            }
            else {
                saturationLightness=(value-lightness)/Math.min(lightness, 1.0-lightness);
                check(1.0, 0.0, saturationLightness);
            }
            if (0.0>=value) {
                saturationValue=0.0;
            }
            else {
                saturationValue=cc/value;
                check(1.0, 0.0, saturationValue);
            }
        }

        public static int toInt(double value) {
            return Math.max(0, Math.min(255, (int)Math.round(value)));
        }

        public static int toInt255(double value) {
            return Math.max(0, Math.min(255, (int)Math.round(255.0*value)));
        }

        public int toRGB() {
            return 0xff000000
                    |toInt255(blue)
                    |(toInt255(green)<<8)
                    |(toInt255(red)<<16);
        }
    }

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
