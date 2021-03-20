package dog.giraffe;

import com.github.sarxos.webcam.Webcam;
import dog.giraffe.threads.AsyncFunction;
import dog.giraffe.threads.Continuation;
import dog.giraffe.threads.Continuations;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.IntToDoubleFunction;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

public class WebcamFrame extends JFrame {
    private static class WebcamGrabber implements Runnable {
        private final WebcamFrame frame;

        public WebcamGrabber(WebcamFrame frame) {
            this.frame=frame;
        }

        @Override
        public void run() {
            try {
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
                    while (!frame.context.stopped()) {
                        BufferedImage image2=webcam.getImage();
                        if (!frame.context.stopped()) {
                            frame.context.executor().execute(()->frame.image.completed(image2));
                        }
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

    private class WindowListenerImpl extends WindowAdapter {
        @Override
        public void windowClosed(WindowEvent event) {
            context.close();
        }
    }

    private static final long serialVersionUID=0L;

    private final Context context;
    private final Continuation<BufferedImage> image;

    public WebcamFrame(Context context) throws Throwable {
        super("Giraffe webcam");
        this.context=context;

        List<AsyncFunction<BufferedImage, BufferedImage>> functions=new ArrayList<>();
        functions.add(AsyncFunction.identity());
        //functions.add(otsu(Collections.singletonList(
        //        new Pair<>(
        //                (rgb)->0.2126*((rgb>>>16)&0xff)+0.7152*((rgb>>>8)&0xff)+0.0722*(rgb&0xff),
        //                0xffffff))));
        //functions.add(otsu(Arrays.asList(
        //        new Pair<>(
        //                (rgb)->(rgb>>>16)&0xff,
        //                0xff0000),
        //        new Pair<>(
        //                (rgb)->(rgb>>>8)&0xff,
        //                0x00ff00),
        //        new Pair<>(
        //                (rgb)->rgb&0xff,
        //                0x0000ff))));
        //functions.add(kMeans(2, 200));
        //functions.add(kMeans(3));
        //functions.add(kMeans(5));
        //functions.add(kMeans(7));
        //functions.add(kMeans(11));
        functions.add(Isodata(2, 200));
        //functions.add(Isodata(100, 200));

        addWindowListener(new WindowListenerImpl());
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setIconImages(Icons.icons());

        int columns=1;
        while (columns*columns<functions.size()) {
            ++columns;
        }
        JPanel panel=new JPanel(new GridLayout(0, columns));
        getContentPane().add(panel);
        List<Continuation<BufferedImage>> images=new ArrayList<>(functions.size());
        for (AsyncFunction<BufferedImage, BufferedImage> function: functions) {
            ImageComponent image=new ImageComponent();
            images.add(
                    Continuations.map(
                            AsyncFunction.async(
                                    limitRate(function),
                                    context.executor()),
                            Continuations.async(
                                        Continuations.consume(image::setImage, context.logger()),
                                        context.executorGui())));
            panel.add(image);
        }
        image=Continuations.split(images);
        pack();
        Dimension screen=getToolkit().getScreenSize();
        if (16*screen.height<9*screen.width) {
            setBounds(screen.width/16, screen.height/8, 3*screen.width/8, 3*screen.height/4);
        }
        else {
            setBounds(screen.width/8, screen.height/8, 3*screen.width/4, 3*screen.height/4);
        }
        setVisible(true);
    }

    private AsyncFunction<BufferedImage, BufferedImage> Isodata(int clusters, int max_clusters) {
        return (image, continuation)->{
            int height=image.getHeight();
            int width=image.getWidth();
            int[] pixels=new int[height*width];
            image.getRGB(0, 0, width, height, pixels, 0, width);
            List<Color.RGB> values=new ArrayList<>(pixels.length);
            for (int pixel: pixels) {
                values.add(Color.RGB.createFromRGB(pixel, 1.0));
            }
            Isodata.cluster(
                    clusters, max_clusters, context,
                    Continuations.map(
                            (centers, continuation2)->{
                                int[] pixels2=new int[height*width];
                                for (int ii=0; pixels.length>ii; ++ii) {
                                    Color.RGB center=Isodata.nearestCenter(centers, Color.RGB.DISTANCE, values.get(ii));
                                    pixels2[ii]=center.toARGBInt(1.0);
                                }
                                BufferedImage image2=new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
                                image2.setRGB(0, 0, width, height, pixels2, 0, width);
                                continuation.completed(image2);
                                System.out.println("starting K="+clusters+" new K="+centers.size());
                            },
                            continuation),
                    Color.RGB.DISTANCE, Color.RGB.MAX, 0.95, 50,
                    Color.RGB.MEAN, Color.RGB.DEV, Sum.PREFERRED, values);
        };
    }


    private AsyncFunction<BufferedImage, BufferedImage> kMeans(int clusters, int max_clusters) {
        return (image, continuation)->{
            int height=image.getHeight();
            int width=image.getWidth();
            int[] pixels=new int[height*width];
            image.getRGB(0, 0, width, height, pixels, 0, width);
            List<Color.RGB> values=new ArrayList<>(pixels.length);
            for (int pixel: pixels) {
                values.add(Color.RGB.createFromRGB(pixel, 1.0));
            }
            KMeans.cluster(
                    clusters, max_clusters, context,
                    Continuations.map(
                            (centers, continuation2)->{
                                int[] pixels2=new int[height*width];
                                for (int ii=0; pixels.length>ii; ++ii) {
                                    Color.RGB center=KMeans.nearestCenter(centers, Color.RGB.DISTANCE, values.get(ii));
                                    pixels2[ii]=center.toARGBInt(1.0);
                                }
                                BufferedImage image2=new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
                                image2.setRGB(0, 0, width, height, pixels2, 0, width);
                                continuation.completed(image2);
                                System.out.println("starting K="+clusters+" new K="+centers.size());
                            },
                            continuation),
                    Color.RGB.DISTANCE, Color.RGB.MAX, 0.95, 1000,
                    Color.RGB.MEAN, Color.RGB.DEV, Sum.PREFERRED, values);
        };
    }

    private <T, U> AsyncFunction<T, U> limitRate(AsyncFunction<T, U> function) {
        return new AsyncFunction<T, U>() {
            class LimitRateContinuation implements Continuation<U> {
                private final Continuation<U> continuation;

                public LimitRateContinuation(Continuation<U> continuation) {
                    this.continuation=continuation;
                }

                private void check() throws Throwable {
                    Continuation<U> continuation3;
                    T input3;
                    synchronized (lock) {
                        if (!has2) {
                            running=false;
                            return;
                        }
                        continuation3=new LimitRateContinuation(continuation2);
                        input3=input2;
                        continuation2=null;
                        has2=false;
                        input2=null;
                    }
                    context.executor().execute(()->function.apply(input3, continuation3));
                }

                @Override
                public void completed(U result) throws Throwable {
                    try {
                        continuation.completed(result);
                    }
                    finally {
                        check();
                    }
                }

                @Override
                public void failed(Throwable throwable) throws Throwable {
                    try {
                        continuation.failed(throwable);
                    }
                    finally {
                        check();
                    }
                }
            }

            private Continuation<U> continuation2;
            private boolean has2;
            private T input2;
            private final Object lock=new Object();
            private boolean running;

            @Override
            public void apply(T input, Continuation<U> continuation) throws Throwable {
                Continuation<U> continuation3;
                synchronized (lock) {
                    if (running) {
                        continuation2=continuation;
                        has2=true;
                        input2=input;
                        return;
                    }
                    continuation3=new LimitRateContinuation(continuation);
                    running=true;
                }
                function.apply(input, continuation3);
            }
        };
    }

    public static void main(String[] args) throws Throwable {
        boolean error=true;
        Context threads=new SwingContext();
        try {
            new Thread(new WebcamGrabber(new WebcamFrame(threads)))
                    .start();
            error=false;
        }
        finally {
            if (error) {
                threads.close();
            }
        }
    }

    private AsyncFunction<BufferedImage, BufferedImage> otsu(List<Pair<IntToDoubleFunction, Integer>> channels) {
        return (image, continuation)->{
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
                double threshold=Otsu2.threshold(512, context, Sum.PREFERRED, values);
                for (int ii=0; pixels.length>ii; ++ii) {
                    if (values.get(ii)>=threshold) {
                        pixels2[ii]|=channel.second;
                    }
                }
            }
            BufferedImage image2=new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
            image2.setRGB(0, 0, width, height, pixels2, 0, width);
            continuation.completed(image2);
        };
    }
}
