package dog.giraffe;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import picocli.CommandLine;

/**
 * Configuration for {@link dog.giraffe.CmdLine CmdLine}.
 */
public class CmdLineConfig {
    public static final String CLUSTERING_ALGORITHM_ISODATA="isodata";
    public static final String CLUSTERING_ALGORITHM_K_MEANS="k-means";
    public static final String CLUSTERING_ALGORITHM_OTSU="otsu";
    public static final String CLUSTERING_ALGORITHM_OTSU_CIRCULAR="otsu-circular";
    public static final String IMAGE_TRANSFORM_HUE="hue";
    public static final String IMAGE_TRANSFORM_HYPER_HUE="hyper-hue";
    public static final String IMAGE_TRANSFORM_INTENSITY="intensity";
    public static final Pattern IMAGE_TRANSFORM_NORMALIZE_DEVIATION
            =Pattern.compile("normalize-deviation\\(([0-9.]+)\\)");
    public static final String IMAGE_TRANSFORM_NORMALIZE_MIN_MAX="normalize-min-max";
    public static final String IMAGE_TRANSFORM_NORMALIZED_DIFFERENCE_VEGETATION_INDEX
            ="normalized-difference-vegetation-index";
    public static final Pattern IMAGE_TRANSFORM_NORMALIZED_HYPER_HUE
            =Pattern.compile("normalized-hyper-hue\\(([0-9.]+)\\)");
    public static final Pattern IMAGE_TRANSFORM_SELECT=Pattern.compile("select\\(([0-9]+(?:,[0-9])+)\\)");
    public static final String SATURATION_BASED_HUE="hue";
    public static final String SATURATION_BASED_HYPER_HUE="hyper-hue";

    /**
     * Number of images processed in parallel in batch mode.
     */
    @CommandLine.Option(names={"--batch-parallel-images"}, paramLabel="BATCHPARALLELImAGES",
            description="Number of images processed in parallel in batch mode.")
    public int batchParallelism;

    /**
     * Enable batch mode. In batch mode input file must be a list of path to images, one in every line.
     */
    @CommandLine.Option(names={"--batch-mode"}, paramLabel="BATCHMODE",
            description="Enable batch mode."
                    +" In batch mode input file must be a list of path to images, one in every line.")
    public boolean batchMode;

    /**
     * Number of bins used by Otsu's method.
     */
    @CommandLine.Option(names={"--bins"}, paramLabel="BINS",
            description="Number of bins used by Otsu's method.")
    public int bins=32;

    /**
     * Buffer input image in memory.
     */
    @CommandLine.Option(names={"--buffered-input"}, paramLabel="BUFFEREDINPUT",
            description="Buffer input image in memory.")
    public Boolean bufferedInput;

    /**
     * Buffer output image in memory.
     */
    @CommandLine.Option(names={"--buffered-output"}, paramLabel="BUFFEREDOUTPUT",
            description="Buffer output image in memory.")
    public Boolean bufferedOutput;

    /**
     * Specify the clustering algorithm.
     * Valid values: isodata, k-means, otsu, and otsu-circular. Default: kMeans with elbow.
     */
    @CommandLine.Option(names={"-a", "--algorithm"}, paramLabel="CLUSTER",
            description="Specify the clustering algorithm."
                    +" Valid values: isodata, k-means, otsu, and otsu-circular. Default: kMeans with elbow.")
    public String clusteringAlgorithm=CLUSTERING_ALGORITHM_K_MEANS;

    /**
     * Use the elbow method
     */
    @CommandLine.Option(names={"-e", "--elbow"}, paramLabel="ELBOW",
            description="Use the elbow method")
    public Boolean elbow;

    /**
     * Elbow of the curve, as used by the elbow method and k-means. Default value is 0.95.
     */
    @CommandLine.Option(names={"--error-limit"}, paramLabel="ERRORLIMIT",
            description="Elbow of the curve, as used by the elbow method and k-means. Default value is 0.95.")
    public double errorLimit=0.95;

    /**
     * Display a help message.
     */
    @CommandLine.Option(names={"-h", "--help"}, usageHelp=true,
            description="Display a help message.")
    public boolean helpRequested;

    /**
     * List of image transformations to perform before clustering.
     * Valid values are: hue, hyper-hue, intensity, normalize-deviation(sigma), normalize-min-max
     * , normalized-difference-vegetation-index, normalized-hyper-hue(max-zero)
     * , select(channel,...), hue-hyper-hue.
     */
    @CommandLine.Option(names={"--image-transform"}, paramLabel="IMAGETRANSFORM",
            description="List of image transformations to perform before clustering."
                    +" Valid values are: hue, hyper-hue, intensity, normalize-deviation(sigma), normalize-min-max"
                    +", normalized-difference-vegetation-index, normalized-hyper-hue(max-zero)"
                    +", select(channel,...), hue-hyper-hue.")
    public List<String> imageTransforms=new ArrayList<>();

    /**
     * Use kd-tree nodes as initial cluster centers.
     */
    @CommandLine.Option(names={"--initial-centers-kd-tree"}, paramLabel="INITKDTREE",
            description="Use kd-tree nodes as initial cluster centers.")
    public Boolean initialCentersKDTree;

    /**
     * Use mean as initial cluster centers.
     */
    @CommandLine.Option(names={"--initial-centers-mean"}, paramLabel="INITMEAN",
            description="Use mean as initial cluster centers.")
    public Boolean initialCentersMean;

    /**
     * Use this many random points as initial cluster centers.
     */
    @CommandLine.Option(names={"--initial-centers-random"}, paramLabel="INITRANDOM",
            description="Use this many random points as initial cluster centers.")
    public int initialCentersRandom;

    /**
     * Input image file.
     */
    @CommandLine.Option(names={"-i", "--input"}, required=true, paramLabel="INPUTFILE",
            description="Input image file.")
    public String inputFile;

    /**
     * Log file.
     */
    @CommandLine.Option(names={"--log"}, paramLabel="LOGFILE",
            description="Log file.")
    public String logFile;

    /**
     * Comma-separated list of coordinates. 4 for every half-plane.
     */
    @CommandLine.Option(names={"--mask"}, paramLabel="MASK",
            description="Comma-separated list of coordinates. 4 for every half-plane.")
    public String mask;

    /**
     * Maximum number of clusters.
     */
    @CommandLine.Option(names={"--max"}, paramLabel="CLUSTERNUMBER",
            description="Maximum number of clusters.")
    public int maxClusters=20;

    /**
     * Maximum number of iterations k-means and isodata goes through.
     */
    @CommandLine.Option(names={"--max-iterations"}, paramLabel="ITERATIONS",
            description="Maximum number of iterations k-means and isodata goes through.")
    public int maxIterations=1000;

    /**
     * Minimum number of clusters.
     */
    @CommandLine.Option(names={"--min"}, paramLabel="CLUSTERNUMBER",
            description="Minimum number of clusters.")
    public int minClusters=2;

    /**
     * Format name for the output image.
     */
    @CommandLine.Option(names={"--output-format"}, paramLabel="OUTPUTFORMAT",
            description="Format name for the output image.")
    public String outputFormat;

    /**
     * Output image file.
     */
    @CommandLine.Option(names={"-o", "--output"}, required=true, paramLabel="OUTPUTFILE",
            description="Output image file.")
    public String outputFile;

    /**
     * Use different hues for clusters in the result.
     */
    @CommandLine.Option(names={"--rgb-cluster-colors"}, paramLabel="RGBCLUSTERCOLORS",
            description="Use different hues for clusters in the result.")
    public Boolean rgbClusterColors;

    /**
     * Replace empty clusters by a point farthest from all clusters.
     */
    @CommandLine.Option(names={"--replace-empty-clusters-farthest"}, paramLabel="REPLACEFARTHEST",
            description="Replace empty clusters by a point farthest from all clusters.")
    public Boolean replaceEmptyClustersFarthest;

    /**
     * Use this many random points as replacement clusters.
     */
    @CommandLine.Option(names={"--replace-empty-clusters-random"}, paramLabel="REPLACERANDOM",
            description="Use this many random points as replacement clusters.")
    public int replaceEmptyClustersRandom=0;

    /**
     * Use saturation based clustering. Valid values: hue, hyper-hue.
     */
    @CommandLine.Option(names={"--saturation-based"}, paramLabel="SATURATION ",
            description="Use saturation based clustering. Valid values: hue, hyper-hue")
    public String saturationBased;

    /**
     * Number of threads used.
     */
    @CommandLine.Option(names={"--threads"}, paramLabel="THREADS",
            description="Number of threads used.")
    public Integer threads;

    /**
     * A cluster is dropped if its size is lower then the theta_L percentage of all points. (Default value is 0.05)
     */
    @CommandLine.Option(names={"--isodata-theta-L"}, paramLabel="CLUSTERPOINT",
            description="A cluster is dropped if its size is lower then the theta_L percentage of all points. (Default value is 0.05)")
    public double theta_N=0.05;

    /**
     * The maximum number of cluster merged in lumping phase. (Default value is 3)
     */
    @CommandLine.Option(names={"--isodata-L"}, paramLabel="CLUSTERNUM",
            description="The maximum number of cluster merged in lumping phase. (Default value is 3)")
    public int L=3;

    /**
     * Cluster centers are merged if their distance is lower then lumping parameter. (Default value is 3)
     */
    @CommandLine.Option(names={"--isodata-lumping"}, paramLabel="THRESHOLD",
            description="Cluster centers are merged if their distance is lower then lumping parameter. (Default value is 3)")
    public double lumping=3.0;

    /**
     * Clusters are divided if its std deviation is larger then this value. (Default value is 5)
     */
    @CommandLine.Option(names={"--isodata-std-deviation"}, paramLabel="CLUSTERNUM",
            description="Clusters are divided if its std deviation is larger then this value. (Default value is 5)")
    public double std_deviation;

    /**
     * Sets the default values for options not specified by the user.
     */
    public void setDefaultValues() {
        if (0>=batchParallelism) {
            batchParallelism=1;
        }
        if (null==bufferedInput) {
            bufferedInput=true;
        }
        if (null==bufferedOutput) {
            bufferedOutput=true;
        }
        if (null==elbow) {
            elbow=true;
        }
        if (null==initialCentersKDTree) {
            initialCentersKDTree=false;
        }
        if (null==initialCentersMean) {
            initialCentersMean=true;
        }
        if (null==rgbClusterColors) {
            rgbClusterColors=true;
        }
        if (null==replaceEmptyClustersFarthest) {
            replaceEmptyClustersFarthest=true;
        }
        if ((null==threads)
                || (0>=threads)) {
            threads=Runtime.getRuntime().availableProcessors();
        }
    }
}
