package dog.giraffe;

import dog.giraffe.image.BufferedImageReader;
import dog.giraffe.image.BufferedImageWriter;
import dog.giraffe.image.FileImageReader;
import dog.giraffe.image.FileImageWriter;
import dog.giraffe.image.Image;
import dog.giraffe.image.ImageReader;
import dog.giraffe.image.ImageWriter;
import dog.giraffe.image.PrepareImages;
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
import dog.giraffe.points.KDTree;
import dog.giraffe.threads.AsyncJoin;
import dog.giraffe.threads.Continuation;
import dog.giraffe.threads.Continuations;
import dog.giraffe.threads.Function;
import dog.giraffe.threads.Supplier;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import picocli.CommandLine;
import picocli.CommandLine.MissingParameterException;

public class CmdLine {
    public static void main(String[] args) throws Throwable {
        CmdLineConfig cmdLineConfig=new CmdLineConfig();
        CommandLine commandLine=new CommandLine(cmdLineConfig);
        commandLine.setCaseInsensitiveEnumValuesAllowed(true);
        try {
            commandLine.parseArgs(args);
        }
        catch (MissingParameterException ignore) {
           cmdLineConfig.helpRequested=true;
        }
        if (cmdLineConfig.helpRequested) {
           commandLine.usage(System.out);
           return;
        }

        Function<Integer, ClusteringStrategy<KDTree>> strategyGenerator;
        switch (cmdLineConfig.clusteringAlgorithm) {
            case CmdLineConfig.CLUSTERING_ALGORITHM_ISODATA:
                cmdLineConfig.elbow=false;
                strategyGenerator=(clusters)->
                        ClusteringStrategy.isodata(cmdLineConfig.minClusters, cmdLineConfig.maxClusters);
                break;
            case CmdLineConfig.CLUSTERING_ALGORITHM_K_MEANS:
                List<InitialCenters<KDTree>> initialCenters=new ArrayList<>();
                if (cmdLineConfig.initialCentersKDTree) {
                    initialCenters.add(KDTree.initialCenters(false));
                }
                if (cmdLineConfig.initialCentersMean) {
                    initialCenters.add(InitialCenters.meanAndFarthest(false));
                }
                for (int ii=cmdLineConfig.initialCentersRandom; 0<ii; --ii) {
                    initialCenters.add(InitialCenters.random());
                }
                List<ReplaceEmptyCluster<KDTree>> replaceEmptyClusters=new ArrayList<>();
                if (cmdLineConfig.replaceEmptyClustersFarthest) {
                    replaceEmptyClusters.add(ReplaceEmptyCluster.farthest(false));
                }
                for (int ii=cmdLineConfig.replaceEmptyClustersRandom; 0<ii; --ii) {
                    replaceEmptyClusters.add(ReplaceEmptyCluster.random());
                }
                List<Function<Integer, ClusteringStrategy<KDTree>>> strategyGenerators=new ArrayList<>();
                initialCenters.forEach((init)->replaceEmptyClusters.forEach((replace)->
                        strategyGenerators.add((clusters)->ClusteringStrategy.kMeans(
                                clusters,
                                cmdLineConfig.errorLimit,
                                init,
                                cmdLineConfig.maxIterations,
                                replace))));
                strategyGenerator=(clusters)->{
                    List<ClusteringStrategy<KDTree>> strategies=new ArrayList<>(strategyGenerators.size());
                    for (Function<Integer, ClusteringStrategy<KDTree>> generator: strategyGenerators) {
                        strategies.add(generator.apply(clusters));
                    }
                    return ClusteringStrategy.best(strategies);
                };
                break;
            case CmdLineConfig.CLUSTERING_ALGORITHM_OTSU:
                strategyGenerator=(clusters)->ClusteringStrategy.otsuLinear(cmdLineConfig.bins, clusters);
                break;
            case CmdLineConfig.CLUSTERING_ALGORITHM_OTSU_CIRCULAR:
                strategyGenerator=(clusters)->ClusteringStrategy.otsuCircular(cmdLineConfig.bins, clusters);
                break;
            default:
                throw new RuntimeException("unexpected clustering algorithm "+cmdLineConfig.clusteringAlgorithm);
        }
        ClusteringStrategy<KDTree> strategy=cmdLineConfig.elbow
                ?ClusteringStrategy.elbow(
                        cmdLineConfig.errorLimit,
                        cmdLineConfig.maxClusters,
                        cmdLineConfig.minClusters,
                        strategyGenerator,
                        1)
                :strategyGenerator.apply(cmdLineConfig.maxClusters);

        Mask mask2=Mask.all();
        if ((null!=cmdLineConfig.mask)
                && (!cmdLineConfig.mask.trim().isEmpty())) {
            String[] strings=cmdLineConfig.mask.split(",");
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
            mask2=Mask.and(masks);
        }
        Mask mask=mask2;

        Function<Image, Image> imageMap;
        if (null==cmdLineConfig.saturationBased) {
            imageMap=(image)->Cluster1.create(
                    image,
                    cmdLineConfig.rgbClusterColors
                            ?ClusterColors.RGB.falseColor(0, 1, 2)
                            :ClusterColors.Gray.falseColor(1),
                    mask,
                    strategy);
        }
        else {
            switch (cmdLineConfig.saturationBased) {
                case CmdLineConfig.SATURATION_BASED_HUE:
                    imageMap=(image)->Cluster2.createHue(image, mask, strategy);
                    break;
                case CmdLineConfig.SATURATION_BASED_HYPER_HUE:
                    imageMap=(image)->Cluster2.createHyperHue(image, mask, strategy);
                    break;
                default:
                    throw new RuntimeException(
                            "unexpected saturation based clustering: "+cmdLineConfig.saturationBased);
            }
        }
        for (int ii=cmdLineConfig.imageTransforms.size()-1; 0<=ii; --ii) {
            Function<Image, Image> imageMap2;
            String it=cmdLineConfig.imageTransforms.get(ii);
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

        String outputFormat=cmdLineConfig.outputFormat;
        if (null==outputFormat) {
            String filename=cmdLineConfig.outputPath.getFileName().toString();
            int ii=filename.lastIndexOf('.');
            if (0<=ii) {
                switch (filename.substring(ii+1).toLowerCase()) {
                    case "bmp":
                        outputFormat="bmp";
                        break;
                    case "gif":
                        outputFormat="gif";
                        break;
                    case "jpeg":
                    case "jpg":
                        outputFormat="jpeg";
                        break;
                    case "png":
                        outputFormat="png";
                        break;
                    case "tif":
                    case "tiff":
                        outputFormat="tiff";
                        break;
                }
            }
            if (null==outputFormat) {
                outputFormat="tiff";
            }
        }

        Files.deleteIfExists(cmdLineConfig.outputPath);

        try (Context context=new StandardContext()) {
            AsyncJoin<Void> join=new AsyncJoin<>();
            write(
                    context,
                    imageMap,
                    cmdLineConfig.bufferedInput
                            ?BufferedImageReader.factory(cmdLineConfig.inputPath)
                            :FileImageReader.factory(cmdLineConfig.inputPath),
                    cmdLineConfig.bufferedOutput
                            ?BufferedImageWriter.factory(outputFormat, cmdLineConfig.outputPath)
                            :FileImageWriter.factory(outputFormat, cmdLineConfig.outputPath),
                    join);
            join.join();
        }
    }

    private static void write(
            Context context, Function<Image, Image> imageMap, Supplier<ImageReader> imageReader,
            ImageWriter.Factory imageWriter, Continuation<Void> continuation) throws Throwable {
        ImageReader imageReader2=imageReader.get();
        Continuation<Void> continuation2=Continuations.finallyBlock(imageReader2::close, continuation);
        try {
            write(context, imageMap, imageReader2, imageWriter, continuation2);
        }
        catch (Throwable throwable) {
            continuation2.failed(throwable);
        }
    }

    private static void write(
            Context context, Function<Image, Image> imageMap, ImageReader imageReader,
            ImageWriter.Factory imageWriter, Continuation<Void> continuation) throws Throwable {
        Image image=imageMap.apply(imageReader);
        PrepareImages.prepareImages(
                context,
                List.of(image),
                Continuations.map(
                        (input, continuation2)->write(context, image, imageWriter, continuation2),
                        continuation));
    }

    private static void write(
            Context context, Image image, ImageWriter.Factory imageWriter, Continuation<Void> continuation)
            throws Throwable {
        ImageWriter imageWriter2=imageWriter.create(image.width(), image.height(), image.dimensions());
        Continuation<Void> continuation2=Continuations.finallyBlock(imageWriter2::close, continuation);
        try {
            ImageWriter.write(context, image, imageWriter2, continuation2);
        }
        catch (Throwable throwable) {
            continuation2.failed(throwable);
        }
    }
}
