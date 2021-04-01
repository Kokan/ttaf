package dog.giraffe;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ColorTest {
    @Test
    public void testHSV() throws Throwable {
        testHSV(0.0, 0.0, 0.0, 0.0*Math.PI/180.0, 0.0, 0.0);
        testHSV(0.0, 0.0, 0.5, 240.0*Math.PI/180.0, 1.0, 0.5);
        testHSV(0.0, 0.0, 1.0, 240.0*Math.PI/180.0, 1.0, 1.0);
        testHSV(0.0, 0.5, 0.0, 120.0*Math.PI/180.0, 1.0, 0.5);
        testHSV(0.0, 0.5, 0.5, 180.0*Math.PI/180.0, 1.0, 0.5);
        testHSV(0.0, 0.5, 1.0, 210.0*Math.PI/180.0, 1.0, 1.0);
        testHSV(0.0, 1.0, 0.0, 120.0*Math.PI/180.0, 1.0, 1.0);
        testHSV(0.0, 1.0, 0.5, 150.0*Math.PI/180.0, 1.0, 1.0);
        testHSV(0.0, 1.0, 1.0, 180.0*Math.PI/180.0, 1.0, 1.0);
        testHSV(0.5, 0.0, 0.0, 0.0*Math.PI/180.0, 1.0, 0.5);
        testHSV(0.5, 0.0, 0.5, 300.0*Math.PI/180.0, 1.0, 0.5);
        testHSV(0.5, 0.0, 1.0, 270.0*Math.PI/180.0, 1.0, 1.0);
        testHSV(0.5, 0.5, 0.0, 60.0*Math.PI/180.0, 1.0, 0.5);
        testHSV(0.5, 0.5, 0.5, 0.0*Math.PI/180.0, 0.0, 0.5);
        testHSV(0.5, 0.5, 1.0, 240.0*Math.PI/180.0, 0.5, 1.0);
        testHSV(0.5, 1.0, 0.0, 90.0*Math.PI/180.0, 1.0, 1.0);
        testHSV(0.5, 1.0, 0.5, 120.0*Math.PI/180.0, 0.5, 1.0);
        testHSV(0.5, 1.0, 1.0, 180.0*Math.PI/180.0, 0.5, 1.0);
        testHSV(1.0, 0.0, 0.0, 0.0*Math.PI/180.0, 1.0, 1.0);
        testHSV(1.0, 0.0, 0.5, 330.0*Math.PI/180.0, 1.0, 1.0);
        testHSV(1.0, 0.0, 1.0, 300.0*Math.PI/180.0, 1.0, 1.0);
        testHSV(1.0, 0.5, 0.0, 30.0*Math.PI/180.0, 1.0, 1.0);
        testHSV(1.0, 0.5, 0.5, 0.0*Math.PI/180.0, 0.5, 1.0);
        testHSV(1.0, 0.5, 1.0, 300.0*Math.PI/180.0, 0.5, 1.0);
        testHSV(1.0, 1.0, 0.0, 60.0*Math.PI/180.0, 1.0, 1.0);
        testHSV(1.0, 1.0, 0.5, 60.0*Math.PI/180.0, 0.5, 1.0);
        testHSV(1.0, 1.0, 1.0, 0.0*Math.PI/180.0, 0.0, 1.0);
    }

    private void testHSV(double red, double green, double blue, double hue, double saturation, double value) {
        Color.Converter colorConverter=new Color.Converter();
        colorConverter.rgbToHslv(blue, green, red);
        assertEquals(hue, colorConverter.hue, 0.01);
        assertEquals(saturation, colorConverter.saturationValue, 0.01);
        assertEquals(value, colorConverter.value, 0.01);
        colorConverter=new Color.Converter();
        colorConverter.hsvToRgb(hue, saturation, value);
        assertEquals(red, colorConverter.red, 0.01);
        assertEquals(green, colorConverter.green, 0.01);
        assertEquals(blue, colorConverter.blue, 0.01);
    }
}
