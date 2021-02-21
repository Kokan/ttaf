package dog.giraffe;

import com.github.sarxos.webcam.Webcam;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.IntToDoubleFunction;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

public class WebcamFrame extends JFrame {
    private static class WebcamRunnable implements Runnable {
        private final WebcamFrame frame;

        public WebcamRunnable(WebcamFrame frame) {
            this.frame=frame;
        }

        private BufferedImage kMeans(int clusters, BufferedImage image, Random random) {
            int height=image.getHeight();
            int width=image.getWidth();
            int[] pixels=new int[height*width];
            image.getRGB(0, 0, width, height, pixels, 0, width);
            List<Color.RGB> values=new ArrayList<>(pixels.length);
            for (int pixel: pixels) {
                values.add(Color.RGB.createFromRGB(pixel, 1.0));
            }
            List<Color.RGB> centers=KMeans.cluster(
                    clusters, Color.RGB.DISTANCE, 0.95, 1000,
                    Color.RGB.MEAN, random, Sum.PREFERRED, values);
            int[] pixels2=new int[height*width];
            for (int ii=0; pixels.length>ii; ++ii) {
                Color.RGB center=KMeans.nearestCenter(centers, Color.RGB.DISTANCE, values.get(ii));
                pixels2[ii]=center.toARGBInt(1.0);
            }
            BufferedImage image2=new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
            image2.setRGB(0, 0, width, height, pixels2, 0, width);
            return image2;
        }

        private BufferedImage otsu(BufferedImage image, List<Pair<IntToDoubleFunction, Integer>> channels) {
            int height=image.getHeight();
            int width=image.getWidth();
            int[] pixels=new int[height*width];
            image.getRGB(0, 0, width, height, pixels, 0, width);
            List<Double> values=new ArrayList<>(pixels.length);
            int[] pixels2=new int[height*width];
            Arrays.fill(pixels2, 0xff000000);
            for (Pair<IntToDoubleFunction, Integer> channel: channels) {
                values.clear();
                for (int pixel: pixels) {
                    values.add(channel.first.applyAsDouble(pixel));
                }
                double threshold=Otsu2.threshold(512, Sum.PREFERRED, values);
                for (int ii=0; pixels.length>ii; ++ii) {
                    if (values.get(ii)>=threshold) {
                        pixels2[ii]|=channel.second;
                    }
                }
            }
            BufferedImage image2=new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
            image2.setRGB(0, 0, width, height, pixels2, 0, width);
            return image2;
        }

        @Override
        public void run() {
            try {
                Random random=new Random();
                Webcam webcam=Webcam.getDefault();
                try {
                    Dimension viewSize=null;
                    for (Dimension dimension: webcam.getViewSizes()) {
                        if ((null==viewSize)
                                || (viewSize.height*viewSize.width<dimension.height*dimension.width)) {
                            viewSize=dimension;
                        }
                    }
                    if (null!=viewSize) {
                        webcam.setViewSize(viewSize);
                    }
                    webcam.open();
                    System.out.println(Arrays.toString(webcam.getViewSizes()));
                    while (frame.isDisplayable()) {
                        BufferedImage image0=webcam.getImage();
                        SwingUtilities.invokeLater(()->frame.image0.setImage(image0));
                        BufferedImage otsu1=otsu(image0, Collections.singletonList(
                                new Pair<>(
                                        (rgb)->0.2126*((rgb>>>16)&0xff)+0.7152*((rgb>>>8)&0xff)+0.0722*(rgb&0xff),
                                        0xffffff)));
                        SwingUtilities.invokeLater(()->frame.image1.setImage(otsu1));
                        BufferedImage otsu2=otsu(image0, Arrays.asList(
                                new Pair<>(
                                        (rgb)->(rgb>>>16)&0xff,
                                        0xff0000),
                                new Pair<>(
                                        (rgb)->(rgb>>>8)&0xff,
                                        0x00ff00),
                                new Pair<>(
                                        (rgb)->rgb&0xff,
                                        0x0000ff)));
                        SwingUtilities.invokeLater(()->frame.image2.setImage(otsu2));
                        BufferedImage kMeans3=kMeans(2, image0, random);
                        SwingUtilities.invokeLater(()->frame.image3.setImage(kMeans3));
                        BufferedImage kMeans4=kMeans(3, image0, random);
                        SwingUtilities.invokeLater(()->frame.image4.setImage(kMeans4));
                        BufferedImage kMeans5=kMeans(5, image0, random);
                        SwingUtilities.invokeLater(()->frame.image5.setImage(kMeans5));
                        BufferedImage kMeans6=kMeans(7, image0, random);
                        SwingUtilities.invokeLater(()->frame.image6.setImage(kMeans6));
                        BufferedImage kMeans7=kMeans(11, image0, random);
                        SwingUtilities.invokeLater(()->frame.image7.setImage(kMeans7));
                        BufferedImage kMeans8=kMeans(13, image0, random);
                        SwingUtilities.invokeLater(()->frame.image8.setImage(kMeans8));
                        sleep();
                    }
                }
                finally {
                    webcam.close();
                }
            }
            catch (Throwable throwable) {
                throwable.printStackTrace(System.err);
            }
        }

        private void sleep() throws Throwable {
            Thread.sleep(25L);
        }
    }

    private static final long serialVersionUID=0L;

    private final ImageComponent image0=new ImageComponent();
    private final ImageComponent image1=new ImageComponent();
    private final ImageComponent image2=new ImageComponent();
    private final ImageComponent image3=new ImageComponent();
    private final ImageComponent image4=new ImageComponent();
    private final ImageComponent image5=new ImageComponent();
    private final ImageComponent image6=new ImageComponent();
    private final ImageComponent image7=new ImageComponent();
    private final ImageComponent image8=new ImageComponent();

    public WebcamFrame() throws Throwable {
        super("Giraffe webcam");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setIconImages(Icons.icons());

        JPanel panel=new JPanel(new GridLayout(0, 3));
        getContentPane().add(panel);
        panel.add(image0);
        panel.add(image1);
        panel.add(image2);
        panel.add(image3);
        panel.add(image4);
        panel.add(image5);
        panel.add(image6);
        panel.add(image7);
        panel.add(image8);

        pack();
        Dimension screen=getToolkit().getScreenSize();
        setBounds(screen.width/16, screen.height/8, 3*screen.width/8, 3*screen.height/4);
        setVisible(true);
    }

    public static void main(String[] args) throws Throwable {
        new Thread(new WebcamRunnable(new WebcamFrame()))
                .start();
    }
}
