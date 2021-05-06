package dog.giraffe.gui;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.JComponent;

public class ImageComponent extends JComponent {
    private static final long serialVersionUID=0L;

    private List<BufferedImage> images;

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        BufferedImage bestImage=null;
        int bestMissing=Integer.MAX_VALUE;
        int width=getWidth();
        int height=getHeight();
        for (BufferedImage image2: images) {
            int missing2=width*height-Math.min(image2.getWidth(), width)*Math.min(image2.getHeight(), height);
            if ((null==bestImage)
                    || (missing2<bestMissing)
                    || ((missing2==bestMissing)
                            && (image2.getWidth()*image2.getHeight()<bestImage.getWidth()*bestImage.getHeight()))) {
                bestImage=image2;
                bestMissing=missing2;
            }
        }
        if (null!=bestImage) {
            graphics.drawImage(bestImage, 0, 0, width, height, this);
        }
    }

    public void setImage(BufferedImage image) {
        setImages(List.of(image));
    }

    public void setImages(List<BufferedImage> images) {
        this.images=images;
        repaint();
    }
}
