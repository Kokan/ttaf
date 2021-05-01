package dog.giraffe;

public class ColorConverter {
    public double blue;
    public double green;
    public double hue;
    public double lightness;
    public double red;
    public double saturationLightness;
    public double saturationValue;
    public double value;

    private static double clamp(double max, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalStateException(Double.toString(value));
        }
        return Math.max(0.0, Math.min(max, value));
    }

    public void hslToRgb(double hue, double lightness, double saturationLightness) {
        clamp(2.0*Math.PI, hue);
        clamp(1.0, lightness);
        clamp(1.0, saturationLightness);
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
        clamp(1.0, blue);
        clamp(1.0, green);
        clamp(1.0, red);
    }

    public void hsvToRgb(double hue, double saturationValue, double value) {
        clamp(2.0*Math.PI, hue);
        clamp(1.0, saturationValue);
        clamp(1.0, value);
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
        clamp(1.0, blue);
        clamp(1.0, green);
        clamp(1.0, red);
    }

    public void rgbToHslv(int rgb) {
        rgbToHslv(
                (rgb&0xff)/255.0,
                ((rgb>>8)&0xff)/255.0,
                ((rgb>>16)&0xff)/255.0);
    }

    public void rgbToHslv(double blue, double green, double red) {
        clamp(1.0, blue);
        clamp(1.0, green);
        clamp(1.0, red);
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
        clamp(2.0*Math.PI, hue);
        if ((0.0>=lightness)
                || (1.0<=lightness)) {
            saturationLightness=0.0;
        }
        else {
            saturationLightness=(value-lightness)/Math.min(lightness, 1.0-lightness);
            clamp(1.0, saturationLightness);
        }
        if (0.0>=value) {
            saturationValue=0.0;
        }
        else {
            saturationValue=cc/value;
            clamp(1.0, saturationValue);
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
