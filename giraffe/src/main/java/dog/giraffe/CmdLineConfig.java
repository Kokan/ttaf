package dog.giraffe;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import picocli.CommandLine;

public class CmdLineConfig {
    public static final String CLUSTERING_ALGORITHM_ISODATA="isodata";
    public static final String CLUSTERING_ALGORITHM_K_MEANS="k-means";
    public static final String CLUSTERING_ALGORITHM_OTSU="otsu";
    public static final String CLUSTERING_ALGORITHM_OTSU_CIRCULAR="otsu-circular";
    public static final String IMAGE_TRANSFORM_HUE="hue";
    public static final String IMAGE_TRANSFORM_HYPER_HUE="hyper-hue";
    public static final String IMAGE_TRANSFORM_INTENSITY="intensity";
    public static final String IMAGE_TRANSFORM_NORMALIZE_MIN_MAX="normalize-min-max";
    public static final Pattern IMAGE_TRANSFORM_NORMALIZE_VARIANCE
            =Pattern.compile("normalize-variance\\(([0-9.]+)\\)");
    public static final String IMAGE_TRANSFORM_NORMALIZED_DIFFERENCE_VEGETATION_INDEX
            ="normalized-difference-vegetation-index";
    public static final Pattern IMAGE_TRANSFORM_NORMALIZED_HYPER_HUE
            =Pattern.compile("normalized-hyper-hue\\(([0-9.]+)\\)");
    public static final Pattern IMAGE_TRANSFORM_SELECT=Pattern.compile("select\\(([0-9]+(?:,[0-9])+)\\)");
    public static final String SATURATION_BASED_HUE="hue";
    public static final String SATURATION_BASED_HYPER_HUE="hyper-hue";

    @CommandLine.Option(names={"--bins"}, paramLabel="BINS",
            description="Number of bins used by Otsu's method.")
    public int bins=32;

    @CommandLine.Option(names={"--buffered-input"}, paramLabel="BUFFEREDINPUT",
            description="Buffer input image in memory")
    public boolean bufferedInput=true;

    @CommandLine.Option(names={"--buffered-output"}, paramLabel="BUFFEREDOUTPUT",
            description="Buffer output image in memory")
    public boolean bufferedOutput=true;

    @CommandLine.Option(names={"-a", "--algorithm"}, paramLabel="CLUSTER",
            description="Specify the clustering algorithm."
                    +" Valid values: ${COMPLETION-CANDIDATES}. Default: kMeans with elbow.")
    public String clusteringAlgorithm=CLUSTERING_ALGORITHM_K_MEANS;

    @CommandLine.Option(names={"-e", "--elbow"}, paramLabel="ELBOW",
            description="Use the elbow method")
    public boolean elbow=true;

    @CommandLine.Option(names={"--error-limit"}, paramLabel="ERRORLIMIT",
            description="Elbow of the curve, as used by the elbow method and k-means. Default value is 0.95.")
    public double errorLimit=0.95;

    @CommandLine.Option(names={"-h", "--help"}, usageHelp=true,
            description="display a help message")
    public boolean helpRequested=false;

    @CommandLine.Option(names={"--image-transform"}, paramLabel="IMAGETRANSFORM",
            description="Transform input image before clustering.")
    public List<String> imageTransforms=new ArrayList<>();

    @CommandLine.Option(names={"--initial-centers-kd-tree"}, paramLabel="INITKDTREE",
            description="Use KD-tree nodes as initial cluster centers.")
    public boolean initialCentersKDTree=false;

    @CommandLine.Option(names={"--initial-centers-mean"}, paramLabel="INITMEAN",
            description="Use mean as initial cluster centers.")
    public boolean initialCentersMean=true;

    @CommandLine.Option(names={"--initial-centers-random"}, paramLabel="INITRANDOM",
            description="Use this many random points as initial cluster centers.")
    public int initialCentersRandom=0;

    @CommandLine.Option(names={"-i", "--input"}, required=true, paramLabel="IMGPATH",
            description="Input image path.")
    public Path inputPath;

    @CommandLine.Option(names={"--mask"}, paramLabel="MASK",
            description="4 coordinates per half-planes.")
    public String mask;

    @CommandLine.Option(names={"--max"}, paramLabel="CLUSTERNUMBER",
            description="Maximum number of clusters.")
    public int maxClusters=20;

    @CommandLine.Option(names={"--max-iterations"}, paramLabel="ITERATIONS",
            description="Maximum number of iterations.")
    public int maxIterations=1000;

    @CommandLine.Option(names={"--min"}, paramLabel="CLUSTERNUMBER",
            description="Minimum number of clusters.")
    public int minClusters=2;

    @CommandLine.Option(names={"--output-format"}, paramLabel="OUTPUTFORMAT",
            description="Format name for the output image.")
    public String outputFormat;

    @CommandLine.Option(names={"-o", "--output"}, required=true, paramLabel="IMGPATH",
            description="Output image path.")
    public Path outputPath;

    @CommandLine.Option(names={"--rgb-cluster-colors"}, paramLabel="RGBCLUSTERCOLORS",
            description="Color clusters.")
    public boolean rgbClusterColors=true;

    @CommandLine.Option(names={"--replace-empty-clusters-farthest"}, paramLabel="REPLACEFARTHEST",
            description="Replacement empty clusters by a farthest point.")
    public boolean replaceEmptyClustersFarthest=true;

    @CommandLine.Option(names={"--replace-empty-clusters-random"}, paramLabel="REPLACERANDOM",
            description="Use this many random points as replacement clusters.")
    public int replaceEmptyClustersRandom=0;

    @CommandLine.Option(names={"--saturation-based"}, paramLabel="SATURATION ",
            description="Use saturation based clustering. Valid values: hue, hyper-hue")
    public String saturationBased=null;
}
