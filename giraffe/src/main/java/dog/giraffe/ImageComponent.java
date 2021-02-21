package dog.giraffe;

import java.awt.Graphics;
import java.awt.Image;
import javax.swing.JComponent;

public class ImageComponent extends JComponent {
    private static final long serialVersionUID=0L;

    private Image image;

    public Image getImage() {
        return image;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Image image2=image;
        if (null==image2) {
            return;
        }
        graphics.drawImage(image2, 0, 0, getWidth(), getHeight(), this);
    }

    public void setImage(Image image) {
        this.image=image;
        repaint();
    }
}
