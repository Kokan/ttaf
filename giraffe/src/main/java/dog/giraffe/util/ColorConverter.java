package dog.giraffe.util;

/**
 * RGB&lt;-&gt;HSV/HSL color conversion.
 * The instances of this class are mutable and not thread-safe.
 */
public class ColorConverter {
    /**
     * The blue component of the color in the RGB color space. Valid values are 0 to 1.
     */
    public double blue;
    /**
     * The green component of the color in the RGB color space. Valid values are 0 to 1.
     */
    public double green;
    /**
     * The hue component of the color in the HSV/HSL color space. Valid values are 0 to 2*PI.
     */
    public double hue;
    /**
     * The lightness component of the color in the HSL color space. Valid values are 0 to 1.
     */
    public double lightness;
    /**
     * The red component of the color in the RGB color space. Valid values are 0 to 1.
     */
    public double red;
    /**
     * The saturation component of the color in the HSL color space. Valid values are 0 to 1.
     */
    public double saturationLightness;
    /**
     * The saturation component of the color in the HSV color space. Valid values are 0 to 1.
     */
    public double saturationValue;
    /**
     * The value component of the color in the HSV color space. Valid values are 0 to 1.
     */
    public double value;

    private static double clamp(double max, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalStateException(Double.toString(value));
        }
        return Math.max(0.0, Math.min(max, value));
    }

    /**
     * Convert HSV coordinates to RGB.
     */
    public void hsvToRgb(double hue, double saturationValue, double value) {
        hue=clamp(2.0*Math.PI, hue);
        saturationValue=clamp(1.0, saturationValue);
        value=clamp(1.0, value);
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
        blue=clamp(1.0, blue);
        green=clamp(1.0, green);
        red=clamp(1.0, red);
    }

    /**
     * Convert RGB coordinates to HSV and HSL.
     */
    public void rgbToHsvAndHsl(double blue, double green, double red) {
        blue=clamp(1.0, blue);
        green=clamp(1.0, green);
        red=clamp(1.0, red);
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
        hue=clamp(2.0*Math.PI, hue);
        if ((0.0>=lightness)
                || (1.0<=lightness)) {
            saturationLightness=0.0;
        }
        else {
            saturationLightness=(value-lightness)/Math.min(lightness, 1.0-lightness);
            saturationLightness=clamp(1.0, saturationLightness);
        }
        if (0.0>=value) {
            saturationValue=0.0;
        }
        else {
            saturationValue=cc/value;
            saturationValue=clamp(1.0, saturationValue);
        }
    }
}
