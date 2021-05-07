package dog.giraffe;

import dog.giraffe.image.BufferedImageReader;
import dog.giraffe.image.BufferedImageWriter;
import dog.giraffe.image.FileImageReader;
import dog.giraffe.image.FileImageWriter;
import dog.giraffe.image.Image;
import dog.giraffe.image.ImageWriter;
import dog.giraffe.image.transform.Cluster1;
import dog.giraffe.image.transform.Cluster2;
import dog.giraffe.image.transform.Hue;
import dog.giraffe.image.transform.HyperHue;
import dog.giraffe.image.transform.Intensity;
import dog.giraffe.image.transform.Mask;
import dog.giraffe.image.transform.Normalize;
import dog.giraffe.image.transform.NormalizedDifferenceVegetationIndex;
import dog.giraffe.image.transform.NormalizedHyperHue;
import dog.giraffe.image.transform.Select;
import dog.giraffe.kmeans.InitialCenters;
import dog.giraffe.kmeans.ReplaceEmptyCluster;
import dog.giraffe.points.KDTree;
import dog.giraffe.threads.AsyncJoin;
import dog.giraffe.threads.Continuation;
import dog.giraffe.threads.Continuations;
import dog.giraffe.threads.Function;
import dog.giraffe.threads.batch.Batch;
import dog.giraffe.threads.batch.BatchRunner;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import picocli.CommandLine;
import picocli.CommandLine.MissingParameterException;

public class CmdLine {
    private static void batchMode(
            CmdLineConfig config, Context context, Function<Image, Image> imageMap, Continuation<Void> continuation)
            throws Throwable {
        boolean error=true;
        InputStream fis=Files.newInputStream(Paths.get(config.inputFile));
        try {
            InputStream bis=new BufferedInputStream(fis);
            try {
                Reader rr=new InputStreamReader(bis, StandardCharsets.UTF_8);
                try {
                    BufferedReader br=new BufferedReader(rr);
                    try {
                        Continuation<Void> continuation2=Continuations.finallyBlock(
                                ()->{
                                    try {
                                        try {
                                            br.close();
                                        }
                                        finally {
                                            rr.close();
                                        }
                                    }
                                    finally {
                                        try {
                                            bis.close();
                                        }
                                        finally {
                                            fis.close();
                                        }
                                    }
                                },
                                continuation);
                        error=false;
                        try {
                            batchMode(config, context, imageMap, br, continuation2);
                        }
                        catch (Throwable throwable) {
                            continuation2.failed(throwable);
                        }
                    }
                    finally {
                        if (error) {
                            br.close();
                        }
                    }
                }
                finally {
                    if (error) {
                        rr.close();
                    }
                }
            }
            finally {
                if (error) {
                    bis.close();
                }
            }
        }
        finally {
            if (error) {
                fis.close();
            }
        }
    }

    private static void batchMode(
            CmdLineConfig config, Context context, Function<Image, Image> imageMap, BufferedReader inputFiles,
            Continuation<Void> continuation) throws Throwable {
        BatchRunner.<String>runMultiThreaded(
                new Batch<>() {
                    private String filename(String filename, String dir, String file, String ext) {
                        return (null==filename)
                                ?null
                                :filename.replaceAll(Pattern.quote("$DIR"), dir)
                                        .replaceAll(Pattern.quote("$FILE"), file)
                                        .replaceAll(Pattern.quote("$EXT"), ext);
                    }

                    @Override
                    public Optional<String> next() throws Throwable {
                        return Optional.ofNullable(inputFiles.readLine());
                    }

                    @Override
                    public void process(
                            Context context, String inputFile, Continuation<Void> continuation) throws Throwable {
                        Path inputPath=Paths.get(inputFile);
                        String dir=(null==inputPath.getParent())
                                ?""
                                :(inputPath.getParent().toString());
                        String file=inputPath.getFileName().toString();
                        String ext="";
                        int ii=file.indexOf('.');
                        if (0<=ii) {
                            ext=file.substring(ii);
                            file=file.substring(0, ii);
                        }
                        String outputFile=filename(config.outputFile, dir, file, ext);
                        String logFile=filename(config.logFile, dir, file, ext);
                        singleFileMode(config, context, imageMap, inputFile, outputFile, logFile, continuation);
                    }
                },
                context,
                config.batchParallelism,
                continuation);
    }

    public static void main(String[] args) throws Throwable {
        CmdLineConfig config=new CmdLineConfig();
        CommandLine commandLine=new CommandLine(config);
        commandLine.setCaseInsensitiveEnumValuesAllowed(true);
        try {
            commandLine.parseArgs(args);
        }
        catch (MissingParameterException ignore) {
           config.helpRequested=true;
        }
        if (config.helpRequested) {
           commandLine.usage(System.out);
           return;
        }
        config.setDefaultValues();

        ClusteringStrategy<KDTree> strategy=strategy(config);

        Mask mask=mask(config);

        Function<Image, Image> imageMap;
        if (null==config.saturationBased) {
            imageMap=(image)->Cluster1.create(
                    image,
                    config.rgbClusterColors
                            ?ClusterColors.RGB.falseColor(0, 1, 2)
                            :ClusterColors.Gray.falseColor(1),
                    mask,
                    strategy);
        }
        else {
            switch (config.saturationBased) {
                case CmdLineConfig.SATURATION_BASED_HUE:
                    imageMap=(image)->Cluster2.createHue(image, mask, strategy);
                    break;
                case CmdLineConfig.SATURATION_BASED_HYPER_HUE:
                    imageMap=(image)->Cluster2.createHyperHue(image, mask, strategy);
                    break;
                default:
                    throw new RuntimeException(
                            "unexpected saturation based clustering: "+config.saturationBased);
            }
        }
        for (int ii=config.imageTransforms.size()-1; 0<=ii; --ii) {
            Function<Image, Image> imageMap2;
            String it=config.imageTransforms.get(ii);
            switch (it) {
                case CmdLineConfig.IMAGE_TRANSFORM_HUE:
                    imageMap2=Hue::create;
                    break;
                case CmdLineConfig.IMAGE_TRANSFORM_HYPER_HUE:
                    imageMap2=HyperHue::create;
                    break;
                case CmdLineConfig.IMAGE_TRANSFORM_INTENSITY:
                    imageMap2=Intensity::create;
                    break;
                case CmdLineConfig.IMAGE_TRANSFORM_NORMALIZE_MIN_MAX:
                    imageMap2=(image)->Normalize.createMinMax(image, mask);
                    break;
                case CmdLineConfig.IMAGE_TRANSFORM_NORMALIZED_DIFFERENCE_VEGETATION_INDEX:
                    imageMap2=NormalizedDifferenceVegetationIndex::create;
                    break;
                default:
                    Matcher matcher=CmdLineConfig.IMAGE_TRANSFORM_NORMALIZE_VARIANCE.matcher(it);
                    if (matcher.matches()) {
                        double sigma=Double.parseDouble(matcher.group(1));
                        imageMap2=(image)->Normalize.createDeviation(image, mask, sigma);
                    }
                    else {
                        matcher=CmdLineConfig.IMAGE_TRANSFORM_NORMALIZED_HYPER_HUE.matcher(it);
                        if (matcher.matches()) {
                            double maxZero=Double.parseDouble(matcher.group(1));
                            imageMap2=(image)->NormalizedHyperHue.create(image, maxZero);
                        }
                        else {
                            matcher=CmdLineConfig.IMAGE_TRANSFORM_SELECT.matcher(it);
                            if (matcher.matches()) {
                                String[] dimensionStrings=matcher.group(1).split(",");
                                int[] dimensions=new int[dimensionStrings.length];
                                for (int dd=0; dimensions.length>dd; ++dd) {
                                    dimensions[dd]=Integer.parseInt(dimensionStrings[dd]);
                                }
                                imageMap2=(image)->Select.create(image, dimensions);
                            }
                            else {
                                throw new RuntimeException("unexpected image transform "+it);
                            }
                        }
                    }
                    break;
            }
            imageMap=imageMap.compose(imageMap2);
        }

        try (Context context=new StandardContext(config.threads)) {
            AsyncJoin join=new AsyncJoin();
            if (config.batchMode) {
                batchMode(config, context, imageMap, join);
            }
            else {
                singleFileMode(config, context, imageMap, config.inputFile, config.outputFile, config.logFile, join);
            }
            join.join();
        }
    }

    private static Mask mask(CmdLineConfig config) {
        Mask mask=Mask.all();
        if ((null!=config.mask)
                && (!config.mask.trim().isEmpty())) {
            String[] strings=config.mask.split(",");
            if (0!=(strings.length%4)) {
                throw new RuntimeException("Mask has to have 4 coordinates for every half-plane.");
            }
            List<Mask> masks=new ArrayList<>(strings.length/4);
            for (int ii=0; strings.length>ii; ii+=4) {
                double x1=Double.parseDouble(strings[ii].trim());
                double y1=Double.parseDouble(strings[ii+1].trim());
                double x2=Double.parseDouble(strings[ii+2].trim());
                double y2=Double.parseDouble(strings[ii+3].trim());
                masks.add(Mask.halfPlane(x1, y1, x2, y2));
            }
            mask=Mask.and(masks);
        }
        return mask;
    }

    private static void singleFileMode(
            CmdLineConfig config, Context context, Function<Image, Image> imageMap, String inputFile,
            String outputFile, String logFile, Continuation<Void> continuation) throws Throwable {
        Path inputPath=Paths.get(inputFile);
        Path outputPath=Paths.get(outputFile);
        Files.deleteIfExists(outputPath);
        String outputFormat=ImageWriter.outputFormat(config.outputFormat, outputPath);
        ImageWriter.write(
                context,
                imageMap,
                config.bufferedInput
                        ?BufferedImageReader.factory(inputPath)
                        :FileImageReader.factory(inputPath),
                config.bufferedOutput
                        ?BufferedImageWriter.factory(outputFormat, outputPath)
                        :FileImageWriter.factory(outputFormat, outputPath),
                (null==logFile)
                        ?null
                        :(log)->Log.write(Paths.get(logFile), log),
                continuation);
    }

    private static ClusteringStrategy<KDTree> strategy(CmdLineConfig config) throws Throwable {
        List<InitialCenters<KDTree>> initialCenters=new ArrayList<>();
        if (config.initialCentersKDTree) {
            initialCenters.add(KDTree.initialCenters(false));
        }
        if (config.initialCentersMean) {
            initialCenters.add(InitialCenters.meanAndFarthest(false));
        }
        for (int ii=config.initialCentersRandom; 0<ii; --ii) {
            initialCenters.add(InitialCenters.random());
        }
        List<ReplaceEmptyCluster<KDTree>> replaceEmptyClusters=new ArrayList<>();
        if (config.replaceEmptyClustersFarthest) {
            replaceEmptyClusters.add(ReplaceEmptyCluster.farthest(false));
        }
        for (int ii=config.replaceEmptyClustersRandom; 0<ii; --ii) {
            replaceEmptyClusters.add(ReplaceEmptyCluster.random());
        }
        Function<Integer, ClusteringStrategy<KDTree>> strategyGenerator;
        switch (config.clusteringAlgorithm) {
            case CmdLineConfig.CLUSTERING_ALGORITHM_ISODATA:
                config.elbow=false;
                List<ClusteringStrategy<KDTree>> strategies=new ArrayList<>();
                initialCenters.forEach((init)->replaceEmptyClusters.forEach((replace)->
                        strategies.add(ClusteringStrategy.isodata(
                                config.minClusters,
                                config.maxClusters,
                                config.errorLimit,
                                config.maxIterations,
                                config.theta_N,
                                config.lumping,
                                config.L,
                                config.std_deviation,
                                init,
                                replace))));
                strategyGenerator=(clusters)->ClusteringStrategy.best(strategies);
                break;
            case CmdLineConfig.CLUSTERING_ALGORITHM_K_MEANS:
                strategyGenerator=(clusters)->{
                    List<ClusteringStrategy<KDTree>> strategies2=new ArrayList<>();
                    initialCenters.forEach((init)->replaceEmptyClusters.forEach((replace)->
                            strategies2.add(ClusteringStrategy.kMeans(
                                    clusters,
                                    config.errorLimit,
                                    init,
                                    config.maxIterations,
                                    replace))));
                    return ClusteringStrategy.best(strategies2);
                };
                break;
            case CmdLineConfig.CLUSTERING_ALGORITHM_OTSU:
                strategyGenerator=(clusters)->ClusteringStrategy.otsuLinear(config.bins, clusters);
                break;
            case CmdLineConfig.CLUSTERING_ALGORITHM_OTSU_CIRCULAR:
                strategyGenerator=(clusters)->ClusteringStrategy.otsuCircular(config.bins, clusters);
                break;
            default:
                throw new RuntimeException("unexpected clustering algorithm "+config.clusteringAlgorithm);
        }
        return config.elbow
                ?ClusteringStrategy.elbow(
                        config.errorLimit,
                        config.maxClusters,
                        config.minClusters,
                        strategyGenerator,
                        1)
                :strategyGenerator.apply(config.maxClusters);
    }
}
