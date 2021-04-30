package dog.giraffe;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class Plap {
    public static void main(String[] args) throws Throwable {
        BufferedImage image=new BufferedImage(800, 600, BufferedImage.TYPE_3BYTE_BGR);
        ColorConverter converter=new ColorConverter();
        for (int yy=0; image.getHeight()>yy; ++yy) {
            for (int xx=0; image.getWidth()>xx; ++xx) {
                double x2=1.0*xx/image.getWidth();
                double y2=1.0*yy/image.getHeight();
                if (2*xx<image.getWidth()) {
                    if (2*yy<image.getHeight()) {
                        converter.hsvToRgb(2.0*Math.PI+0.25*(y2-0.5), 1.0, 1.0);
                    }
                    else {
                        converter.hsvToRgb(0.25*(y2-0.5), 1.0, 1.0);
                    }
                }
                else {
                    converter.hsvToRgb(Math.PI+0.25*(y2-0.5), 1.0, 1.0);
                }
                int red=(int)Math.max(0, Math.min(255, Math.round(255.0*converter.red)));
                int green=(int)Math.max(0, Math.min(255, Math.round(255.0*converter.green)));
                int blue=(int)Math.max(0, Math.min(255, Math.round(255.0*converter.blue)));
                int rgb=(red<<16)|(green<<8)|blue;
                image.setRGB(xx, yy, rgb);
            }
        }
        ImageIO.write(image, "tiff", new File("/stuff/unistuff/ttaf2/huetest1.tiff"));

        image=new BufferedImage(800, 600, BufferedImage.TYPE_3BYTE_BGR);
        for (int yy=0; image.getHeight()>yy; ++yy) {
            for (int xx=0; image.getWidth()>xx; ++xx) {
                double x2=1.0*xx/image.getWidth();
                double y2=1.0*yy/image.getHeight();
                if (2*xx<image.getWidth()) {
                    converter.hsvToRgb(1.5*Math.PI+0.25*(y2-0.5), 1.0, 1.0);
                }
                else {
                    converter.hsvToRgb(0.5*Math.PI+0.25*(y2-0.5), 1.0, 1.0);
                }
                int red=(int)Math.max(0, Math.min(255, Math.round(255.0*converter.red)));
                int green=(int)Math.max(0, Math.min(255, Math.round(255.0*converter.green)));
                int blue=(int)Math.max(0, Math.min(255, Math.round(255.0*converter.blue)));
                int rgb=(red<<16)|(green<<8)|blue;
                image.setRGB(xx, yy, rgb);
            }
        }
        ImageIO.write(image, "tiff", new File("/stuff/unistuff/ttaf2/huetest2.tiff"));
    }
}
