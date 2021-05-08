package dog.giraffe.gui;

import dog.giraffe.util.Block;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class Images {
    public static final List<BufferedImage> NO_IMAGE;

    static {
        BufferedImage noImage=new BufferedImage(800, 600, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D graphics=noImage.createGraphics();
        try {
            renderingHintsQuality(graphics);
            graphics.setColor(Color.WHITE);
            graphics.setBackground(Color.WHITE);
            graphics.fillRect(0, 0, noImage.getWidth(), noImage.getHeight());
            graphics.setColor(Color.BLACK);
            int radius1=4*Math.min(noImage.getWidth(), noImage.getHeight())/10;
            int radius2=3*Math.min(noImage.getWidth(), noImage.getHeight())/10;
            int centerX=noImage.getWidth()/2;
            int centerY=noImage.getHeight()/2;
            graphics.fillOval(centerX-radius1, centerY-radius1, 2*radius1, 2*radius1);
            graphics.setColor(Color.WHITE);
            graphics.fillOval(centerX-radius2, centerY-radius2, 2*radius2, 2*radius2);
            graphics.setColor(Color.BLACK);
            int radius3=(radius1-radius2)/2;
            AffineTransform affineTransform=new AffineTransform();
            affineTransform.translate(centerX, centerY);
            affineTransform.rotate(-Math.PI/4.0);
            graphics.setTransform(affineTransform);
            graphics.fillRect(-radius2, -radius3, 2*radius2, 2*radius3);
        }
        finally {
            graphics.dispose();
        }
        try {
            NO_IMAGE=resize(()->{}, noImage);
        }
        catch (Error|RuntimeException ex) {
            throw ex;
        }
        catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    private Images() {
    }

    public static void renderingHintsQuality(Graphics2D graphics) {
        graphics.setRenderingHint(
                RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        graphics.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(
                RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        graphics.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(
                RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    public static List<BufferedImage> resize(Block checkedStopped, BufferedImage image) throws Throwable {
        List<BufferedImage> images=new ArrayList<>();
        images.add(image);
        AffineTransformOp op=new AffineTransformOp(new AffineTransform(), AffineTransformOp.TYPE_BICUBIC);
        while ((1<image.getWidth())
                && (1<image.getHeight())) {
            checkedStopped.run();
            int width=(image.getWidth()+1)/2;
            int height=(image.getHeight()+1)/2;
            BufferedImage image2=op.createCompatibleDestImage(
                    image.getSubimage(0, 0, width, height),
                    image.getColorModel());
            Graphics2D graphics=image2.createGraphics();
            try {
                renderingHintsQuality(graphics);
                graphics.drawImage(image, 0, 0, width, height, null);
            }
            finally {
                graphics.dispose();
            }
            images.add(image2);
            image=image2;
        }
        return images;
    }
}
