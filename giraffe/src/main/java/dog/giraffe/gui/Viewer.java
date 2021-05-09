package dog.giraffe.gui;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

public class Viewer {
    public static void main(String[] args) throws Throwable {
        List<List<BufferedImage>> images=new ArrayList<>();
        for (String arg: args) {
            BufferedImage image=ImageIO.read(Paths.get(arg).toFile());
            List<BufferedImage> images2=Images.resize(()->{}, image);
            images.add(images2);
        }

        JFrame frame=new JFrame("Giraffe viewer");
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setIconImages(Icons.icons());

        ViewerPanel viewerPanel=new ViewerPanel(()->images);
        frame.getContentPane().add(viewerPanel.component());

        frame.pack();
        if ((100>frame.getWidth())
                || (100>frame.getHeight())) {
            frame.setSize(Math.max(100, frame.getWidth()), Math.max(100, frame.getHeight()));
        }
        Dimension screenSize=frame.getToolkit().getScreenSize();
        frame.setLocation((screenSize.width-frame.getWidth())/2, (screenSize.height-frame.getHeight())/2);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);

        frame.setVisible(true);
    }
}
