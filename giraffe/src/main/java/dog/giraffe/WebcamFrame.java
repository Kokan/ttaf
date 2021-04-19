package dog.giraffe;

import com.github.sarxos.webcam.Webcam;
import dog.giraffe.InitialCenters;
import dog.giraffe.ReplaceEmptyCluster;
import dog.giraffe.points.ByteArrayL2Points;
import dog.giraffe.points.KDTree;
import dog.giraffe.points.L2Points;
import dog.giraffe.threads.AsyncFunction;
import dog.giraffe.threads.AsyncSupplier;
import dog.giraffe.threads.Block;
import dog.giraffe.threads.Consumer;
import dog.giraffe.threads.Continuation;
import dog.giraffe.threads.Continuations;
import dog.giraffe.isodata.Isodata;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.function.Function;
import java.util.function.IntToDoubleFunction;
import java.util.function.Predicate;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

public class WebcamFrame extends JFrame {
    private class ImageConsumer implements Consumer<BufferedImage> {
        class LimitRateContinuation implements Continuation<BufferedImage> {
            private final Continuation<BufferedImage> continuation;

            public LimitRateContinuation(Continuation<BufferedImage> continuation) {
                this.continuation=continuation;
            }

            private void check() throws Throwable {
                BufferedImage image3;
                synchronized (lock) {
                    if (null==image2) {
                        running=false;
                        return;
                    }
                    image3=image2;
                    image2=null;
                }
                execute(image3);
            }

            @Override
            public void completed(BufferedImage result) throws Throwable {
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

        private final AsyncFunction<BufferedImage, BufferedImage> function;
        private final ImageComponent image;
        private BufferedImage image2;
        private final Object lock=new Object();
        private boolean running;

        public ImageConsumer(AsyncFunction<BufferedImage, BufferedImage> function, ImageComponent image) {
            this.function=function;
            this.image=image;
        }

        @Override
        public void accept(BufferedImage value) throws Throwable {
            synchronized (lock) {
                if ((null==value)
                        || running) {
                    image2=value;
                    return;
                }
                running=true;
            }
            execute(value);
        }

        private void execute(BufferedImage image) throws Throwable {
            Continuation<BufferedImage> continuation
                    =Continuations.async(
                            Continuations.consume(this.image::setImage, context.logger()),
                            context.executorGui());
            continuation=Continuations.singleRun(new LimitRateContinuation(continuation));
            continuation=Continuations.map(function, continuation);
            continuation=Continuations.async(continuation, context.executor());
            Block block=Block.supply(AsyncSupplier.constSupplier(image), continuation);
            try {
                context.executor().execute(block);
            }
            catch (Throwable throwable) {
                continuation.failed(throwable);
            }
        }
    }

    private interface Projection {
        Projection HUE=new Projection() {
            @Override
            public int dimensions() {
                return 1;
            }

            @Override
            public void project(byte[] buf, Color.Converter colorConverter, int offset, int rgb) {
                colorConverter.rgbToHslv(rgb);
                buf[offset]=(byte)Color.Converter.toInt255(colorConverter.hue/(2.0*Math.PI));
            }

            @Override
            public void project(Color.Converter colorConverter, ByteArrayL2Points.Builder points, int rgb) {
                colorConverter.rgbToHslv(rgb);
                points.add((byte)Color.Converter.toInt255(colorConverter.hue/(2.0*Math.PI)));
            }

            @Override
            public int rgb(Color.Converter colorConverter, Vector point) {
                colorConverter.hsvToRgb(
                        2.0*Math.PI*point.coordinate(0)/255.0, 1.0, 1.0);
                return colorConverter.toRGB();
            }
        };

        Projection RGB=new Projection() {
            @Override
            public int dimensions() {
                return 3;
            }

            @Override
            public void project(byte[] buf, Color.Converter colorConverter, int offset, int rgb) {
                buf[offset]=(byte)(rgb&0xff);
                buf[offset+1]=(byte)((rgb>>8)&0xff);
                buf[offset+2]=(byte)((rgb>>16)&0xff);
            }

            @Override
            public void project(Color.Converter colorConverter, ByteArrayL2Points.Builder points, int rgb) {
                points.add((byte)(rgb&0xff), (byte)((rgb>>8)&0xff), (byte)((rgb>>16)&0xff));
            }

            @Override
            public int rgb(Color.Converter colorConverter, Vector point) {
                return 0xff000000
                        |Color.Converter.toInt(point.coordinate(0))
                        |(Color.Converter.toInt(point.coordinate(1))<<8)
                        |(Color.Converter.toInt(point.coordinate(2))<<16);
            }
        };

        int dimensions();

        void project(byte[] buf, Color.Converter colorConverter, int offset, int rgb);

        void project(Color.Converter colorConverter, ByteArrayL2Points.Builder points, int rgb);

        int rgb(Color.Converter colorConverter, Vector point);

        default void set(int c) {
        }
    }

    private class ReplaceCenters implements Projection {
        private final Projection projection;
        private int c;
        

        @Override
        public void set(int c) {
            this.c=c;
        }

        public ReplaceCenters(Projection projection) {
           this.projection=projection;

           Vector darkgreen = new Vector(0,0x64,0);
           colors.add(darkgreen);
           Vector darkblue = new Vector(0x8b,0,0);
           colors.add(darkblue);
           Vector maroon3 = new Vector(0x60, 0x30, 0xb0);// #b03060 
           colors.add(maroon3);
           Vector red  = new Vector(0, 0, 0xff);//#ff0000
           colors.add(red);
           Vector yellow  = new Vector(0, 0xff, 0xff);//#ffff00
           colors.add(yellow);
           Vector burlywood  = new Vector(0x87, 0xb8, 0xde);//#deb887
           colors.add(burlywood);
           Vector lime  = new Vector(0, 0xff, 0);//#00ff00
           colors.add(lime);
           Vector aqua  = new Vector(0xff, 0xff, 0);//#00ffff
           colors.add(aqua);
           Vector fuchsia  = new Vector(0xff, 0, 0xff);//#ff00ff
           colors.add(fuchsia);
           Vector cornflower  = new Vector(0xed, 0x95, 0x64);//#6495ed
           colors.add(cornflower);
        }
        private Map<Vector, Vector> newc = new HashMap<>();
        private List<Vector> colors = new ArrayList<>();

        private Vector replace_point(Vector point) {
            if (newc.containsKey(point)) {
               return newc.get(point);
            }
            //newc.put(point, colors.get(newc.size() % colors.size()));
            newc.put(point, colors.get(newc.size()));

            return newc.get(point);
        }

        @Override
        public int dimensions() {
           return projection.dimensions();
        }

        @Override
        public void project(byte[] buf, Color.Converter colorConverter, int offset, int rgb) {
           projection.project(buf, colorConverter, offset, rgb);
        }

        @Override
        public void project(Color.Converter colorConverter, ByteArrayL2Points.Builder points, int rgb) {
             projection.project(colorConverter, points, rgb);
        }

        @Override
        public int rgb(Color.Converter colorConverter, Vector point) {
            //return projection.rgb(colorConverter, replace_point(point));
            return projection.rgb(colorConverter, point);
        }
    };

    private static class WebcamGrabber implements Runnable {
        private final WebcamFrame frame;

        public WebcamGrabber(WebcamFrame frame) {
            this.frame=frame;
        }

        @Override
        public void run() {
            try {
                Webcam webcam=Webcam.getDefault();
                if (null==webcam) {
                    throw new RuntimeException("no webcam");
                }
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
                            frame.context.executor().execute(()->frame.image.accept(image2));
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

    private static class FileGrabber implements Runnable {
        private final WebcamFrame frame;
        private final File file;

        public FileGrabber(WebcamFrame frame, File file) {
            this.frame=frame;
            this.file=file;
        }

        @Override
        public void run() {
            try {
                    if (frame.context.stopped()) {
                       return;
                    }
                    BufferedImage image2=ImageIO.read(file);

                    frame.context.executor().execute(()->frame.image.accept(image2));
            }
            catch (Throwable throwable) {
                throwable.printStackTrace(System.err);
            }
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
    private final Consumer<BufferedImage> image;

    public WebcamFrame(Context context) throws Throwable {
        super("Giraffe webcam");
        this.context=context;

        List<AsyncFunction<BufferedImage, BufferedImage>> functions=new ArrayList<>();
        functions.add(AsyncFunction.identity());
        functions.add(otsu(Collections.singletonList(
                new Pair<>(
                        (rgb)->0.2126*((rgb>>>16)&0xff)+0.7152*((rgb>>>8)&0xff)+0.0722*(rgb&0xff),
                        0xffffff))));
        functions.add(otsu(Arrays.asList(
                new Pair<>(
                        (rgb)->(rgb>>>16)&0xff,
                        0xff0000),
                new Pair<>(
                        (rgb)->(rgb>>>8)&0xff,
                        0x00ff00),
                new Pair<>(
                        (rgb)->rgb&0xff,
                        0x0000ff))));
        //functions.add(kMeans(2, Projection.RGB));
        //functions.add(kMeans(3, Projection.RGB));
        functions.add(kMeans(-13, Projection.RGB));
        //functions.add(kMeans(2, Projection.HUE));
        //functions.add(kMeans(3, Projection.HUE));
        functions.add(kMeans(-13, Projection.HUE));
        functions.add(saturationBased(kMeansStrategy(-13)));

        functions.add(Isodata( 2, 30, Projection.RGB));
        functions.add(Isodata( 2, 30, Projection.HUE));
        functions.add(saturationBased(isodataStrategy(2,30)));

        addWindowListener(new WindowListenerImpl());
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setIconImages(Icons.icons());

        int columns=1;
        while (columns*columns<functions.size()) {
            ++columns;
        }
        JPanel panel=new JPanel(new GridLayout(0, columns));
        getContentPane().add(panel);
        List<Consumer<BufferedImage>> images=new ArrayList<>(functions.size());
        for (AsyncFunction<BufferedImage, BufferedImage> function: functions) {
            ImageComponent image=new ImageComponent();
            images.add(new ImageConsumer(function, image));
            panel.add(image);
        }
        image=Consumer.fork(images);
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

    private ClusteringStrategy<L2Points.Distance, L2Points.Mean, L2Points.StdDeviation, KDTree<ByteArrayL2Points>, Vector> isodataStrategy(
            int start_clusters, int desired_clusters) {
            double errorLimit=0.95;
            int maxIterations=10;
            List<ClusteringStrategy<L2Points.Distance, L2Points.Mean, L2Points.StdDeviation, KDTree<ByteArrayL2Points>, Vector>>
                    strategies=new ArrayList<>();
            strategies.add((contex,points,cont)-> {

            Isodata.cluster(
                    start_clusters,
                    desired_clusters,
                    context,
                    cont,
                    0.95,
                    InitialCenters.meanAndFarthest(false),
                    maxIterations,
                    points,
                    ReplaceEmptyCluster.farthest(false)
                    );

});
            return ClusteringStrategy.best(strategies);
    }

    private ClusteringStrategy<L2Points.Distance, L2Points.Mean, L2Points.StdDeviation, KDTree<ByteArrayL2Points>, Vector> kMeansStrategy(
            int clusters) {
        Function<Integer, ClusteringStrategy<L2Points.Distance, L2Points.Mean, L2Points.StdDeviation, KDTree<ByteArrayL2Points>, Vector>>
                strategyGenerator=(clusters2)->{
            double errorLimit=0.95;
            int maxIterations=1000;
            List<ClusteringStrategy<L2Points.Distance, L2Points.Mean, L2Points.StdDeviation, KDTree<ByteArrayL2Points>, Vector>>
                    strategies=new ArrayList<>();
            strategies.add(ClusteringStrategy.
                    <L2Points.Distance, L2Points.Mean, L2Points.StdDeviation, KDTree<ByteArrayL2Points>, Vector>kMeans(
                            clusters2,
                            errorLimit,
                            InitialCenters.meanAndFarthest(false),
                            maxIterations,
                            ReplaceEmptyCluster.farthest(false)));
                        /*strategies.add(ClusteringStrategy.
                                <L2Points.Distance, L2Points.Mean, L2Points.StdDeviation, KDTree<ByteArrayL2Points>, Vector>kMeans(
                                        clusters2,
                                        errorLimit,
                                        InitialCenters.meanAndFarthest(true),
                                        maxIterations,
                                        ReplaceEmptyCluster.farthest(true)));*/
                        /*strategies.add(ClusteringStrategy.kMeans(
                                clusters2,
                                errorLimit,
                                KDTree.initialCenters(false),
                                maxIterations,
                                ReplaceEmptyCluster.farthest(false)));*/
                        /*strategies.add(ClusteringStrategy.kMeans(
                                clusters2,
                                errorLimit,
                                KDTree.initialCenters(true),
                                maxIterations,
                                ReplaceEmptyCluster.farthest(true)));*/
                        /*strategies.add(ClusteringStrategy.best(
                                5,
                                ClusteringStrategy.
                                        <L2Points.Distance, L2Points.Mean, L2Points.StdDeviation, KDTree<ByteArrayL2Points>, Vector>kMeans(
                                                clusters2,
                                                errorLimit,
                                                InitialCenters.random(),
                                                maxIterations,
                                                ReplaceEmptyCluster.farthest(false))));*/
                        /*strategies.add(ClusteringStrategy.best(
                                5,
                                ClusteringStrategy.
                                        <L2Points.Distance, L2Points.Mean, L2Points.StdDeviation, KDTree<ByteArrayL2Points>, Vector>kMeans(
                                                clusters2,
                                                errorLimit,
                                                InitialCenters.random(),
                                                maxIterations,
                                                ReplaceEmptyCluster.farthest(true))));*/
            return ClusteringStrategy.best(strategies);
        };
        return (0<clusters)
                ?strategyGenerator.apply(clusters)
                :ClusteringStrategy.elbow(0.95, -clusters, 1, strategyGenerator, 1);
    }

    private AsyncFunction<BufferedImage, BufferedImage> Isodata(int start_clusters, int desired_clusters, Projection projection) {
        return (image, continuation)->{
            int height=image.getHeight();
            int width=image.getWidth();
            int maxIterations=10;
            int[] pixels=new int[height*width];
            image.getRGB(0, 0, width, height, pixels, 0, width);
            Color.Converter colorConverter=new Color.Converter();
            ByteArrayL2Points.Builder pointsBuilder
                    =new ByteArrayL2Points.Builder(projection.dimensions(), height*width);
            for (int rgb: pixels) {
                projection.project(colorConverter, pointsBuilder, rgb);
            }

            Isodata.cluster(
                    start_clusters,
                    desired_clusters,
                    context,
                    Continuations.map(
                            (clusters2, continuation2)->{
                                List<Vector> centers=clusters2.centers;
                                byte[] buf=new byte[projection.dimensions()];
                                Vector point=new Vector(projection.dimensions());
                                int[] pixels2=new int[height*width];
                                Function<Vector, Vector> nearestCenter=(16>centers.size())
                                        ?Distance.nearestCenter(centers, L2Points.DISTANCE)
                                        :KDTree.nearestCenter(centers, Sum.PREFERRED);
                                for (int ii=0; pixels.length>ii; ++ii) {
                                    projection.project(buf, colorConverter, 0, pixels[ii]);
                                    for (int dd=0; projection.dimensions()>dd; ++dd) {
                                        point.coordinate(dd, buf[dd]&0xff);
                                    }
                                    Vector center=nearestCenter.apply(point);
                                    pixels2[ii]=projection.rgb(colorConverter, center);
                                }
                                BufferedImage image2=new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
                                image2.setRGB(0, 0, width, height, pixels2, 0, width);
                                continuation2.completed(image2);
                            },
                            continuation),
                    0.95,
                    InitialCenters.meanAndFarthest(false),
                    maxIterations,
                    KDTree.create(4096, pointsBuilder.create(), context.sum()),
                    ReplaceEmptyCluster.farthest(false)
                    );
        };
    }

    private AsyncFunction<BufferedImage, BufferedImage> kMeans(int clusters, Projection projection) {
        return (image, continuation)->{
            ClusteringStrategy<L2Points.Distance, L2Points.Mean, L2Points.StdDeviation, KDTree<ByteArrayL2Points>, Vector> strategy
                    =kMeansStrategy(clusters);
            int height=image.getHeight();
            int width=image.getWidth();
            int[] pixels=new int[height*width];
            image.getRGB(0, 0, width, height, pixels, 0, width);
            Color.Converter colorConverter=new Color.Converter();
            ByteArrayL2Points.Builder pointsBuilder
                    =new ByteArrayL2Points.Builder(projection.dimensions(), height*width);
            for (int rgb: pixels) {
                projection.project(colorConverter, pointsBuilder, rgb);
            }
            strategy.cluster(
                    context,
                    KDTree.create(4096, pointsBuilder.create(), context.sum()),
                    Continuations.map(
                            (clusters2, continuation2)->{
                                List<Vector> centers=clusters2.centers;
                                byte[] buf=new byte[projection.dimensions()];
                                Vector point=new Vector(projection.dimensions());
                                int[] pixels2=new int[height*width];
                                Function<Vector, Vector> nearestCenter=(16>centers.size())
                                        ?Distance.nearestCenter(centers, L2Points.DISTANCE)
                                        :KDTree.nearestCenter(centers, Sum.PREFERRED);
                                for (int ii=0; pixels.length>ii; ++ii) {
                                    projection.project(buf, colorConverter, 0, pixels[ii]);
                                    for (int dd=0; projection.dimensions()>dd; ++dd) {
                                        point.coordinate(dd, buf[dd]&0xff);
                                    }
                                    Vector center=nearestCenter.apply(point);
                                    pixels2[ii]=projection.rgb(colorConverter, center);
                                }
                                BufferedImage image2=new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
                                image2.setRGB(0, 0, width, height, pixels2, 0, width);
                                continuation2.completed(image2);
                            },
                            continuation));
        };
    }

    private static Runnable grabberFactory(WebcamFrame frame, String[] args) {
        return (1<=args.length)
                ?new FileGrabber(frame, new File(args[0]))
                :new WebcamGrabber(frame);
    }

    public static void main(String[] args) throws Throwable {
        boolean error=true;
        Context threads=new SwingContext();
        try {
            new Thread(grabberFactory(new WebcamFrame(threads), args))
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

    private AsyncFunction<BufferedImage, BufferedImage> saturationBased(
            ClusteringStrategy<L2Points.Distance, L2Points.Mean, L2Points.StdDeviation, KDTree<ByteArrayL2Points>, Vector> strategy) {
        return (image, continuation)->{
            Predicate<Color.Converter> predicate=(colorConverter)->
                    1.0-0.8*colorConverter.value>=colorConverter.saturationValue;
                    //(0.1>colorConverter.saturationValue) || (0.1>colorConverter.value);
            int height=image.getHeight();
            int width=image.getWidth();
            int[] pixels=new int[height*width];
            image.getRGB(0, 0, width, height, pixels, 0, width);
            Color.Converter colorConverter=new Color.Converter();
            ByteArrayL2Points.Builder grayBuilder
                    =new ByteArrayL2Points.Builder(1, height*width/2);
            ByteArrayL2Points.Builder trueBuilder
                    =new ByteArrayL2Points.Builder(1, height*width/2);
            for (int rgb: pixels) {
                colorConverter.rgbToHslv(rgb);
                if (predicate.test(colorConverter)) {
                    grayBuilder.add((byte)(255.0*colorConverter.value));
                }
                else {
                    trueBuilder.add((byte)(127.5*colorConverter.hue/Math.PI));
                }
            }
            List<AsyncSupplier<Clusters<Vector>>> forks=new ArrayList<>(2);
            for (ByteArrayL2Points.Builder builder: new ByteArrayL2Points.Builder[]{grayBuilder, trueBuilder}) {
                forks.add((continuation2)->{
                    ByteArrayL2Points points=builder.create();
                    if (0>=points.size()) {
                        continuation2.completed(new Clusters<>(Collections.emptyList(), 0.0));
                    }
                    else {
                        strategy.cluster(
                                context, KDTree.create(4096, points, context.sum()), continuation2);
                    }
                });
            }
            Continuation<List<Clusters<Vector>>> join=Continuations.map(
                    (clusters2, continuation2)->{
                        List<Vector> grayCenters=clusters2.get(0).centers;
                        List<Vector> trueCenters=clusters2.get(1).centers;
                        Vector point=new Vector(1);
                        int[] pixels2=new int[height*width];
                        Function<Vector, Vector> nearestGrayCenter=(16>grayCenters.size())
                                ?Distance.nearestCenter(grayCenters, L2Points.DISTANCE)
                                :KDTree.nearestCenter(grayCenters, Sum.PREFERRED);
                        Function<Vector, Vector> nearestTrueCenter=(16>trueCenters.size())
                                ?Distance.nearestCenter(trueCenters, L2Points.DISTANCE)
                                :KDTree.nearestCenter(trueCenters, Sum.PREFERRED);
                        for (int ii=0; pixels.length>ii; ++ii) {
                            colorConverter.rgbToHslv(pixels[ii]);
                            if (predicate.test(colorConverter)) {
                                point.coordinate(0, 255.0*colorConverter.value);
                                Vector center=nearestGrayCenter.apply(point);
                                colorConverter.hsvToRgb(
                                        0.0,
                                        0.0,
                                        center.coordinate(0)/255.0);
                            }
                            else {
                                point.coordinate(0, 127.5*colorConverter.hue/Math.PI);
                                Vector center=nearestTrueCenter.apply(point);
                                colorConverter.hsvToRgb(
                                        Math.PI*center.coordinate(0)/127.5,
                                        1.0,
                                        1.0);
                            }
                            pixels2[ii]=colorConverter.toRGB();
                        }
                        BufferedImage image2=new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
                        image2.setRGB(0, 0, width, height, pixels2, 0, width);
                        continuation2.completed(image2);
                    },
                    continuation);
            Continuations.forkJoin(forks, join, context.executor());
        };
    }
}
