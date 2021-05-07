package dog.giraffe.gui;

import dog.giraffe.ClusterColors;
import dog.giraffe.ClusteringStrategy;
import dog.giraffe.gui.model.HalfPlane;
import dog.giraffe.gui.model.Transform;
import dog.giraffe.image.Image;
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
import dog.giraffe.threads.Function;
import dog.giraffe.threads.Supplier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class Outputs {
    public static final String CARD_CLUSTER="cluster";
    public static final String CARD_EMPTY="empty";
    public static final String CARD_NORMALIZE_DEVIATION="normalize-deviation";
    public static final String CARD_NORMALIZED_HYPER_HUE="normalized-hyper-hue";
    public static final String CARD_SELECT="select";


    public static final Transform.Visitor<String> TRANSFORM_CARDS=new Transform.Visitor<>() {
        @Override
        public String cluster(Transform.Cluster cluster) {
            return CARD_CLUSTER;
        }

        @Override
        public String hue() {
            return CARD_EMPTY;
        }

        @Override
        public String hyperHue() {
            return CARD_EMPTY;
        }

        @Override
        public String intensity() {
            return CARD_EMPTY;
        }

        @Override
        public String normalizeDeviation(Transform.NormalizeDeviation normalizeDeviation) {
            return CARD_NORMALIZE_DEVIATION;
        }

        @Override
        public String normalizeMinMax() {
            return CARD_EMPTY;
        }

        @Override
        public String normalizedDifferenceVegetationIndex() {
            return CARD_EMPTY;
        }

        @Override
        public String normalizedHyperHue(Transform.NormalizedHyperHue normalizedHyperHue) {
            return CARD_NORMALIZED_HYPER_HUE;
        }

        @Override
        public String select(Transform.Select select) {
            return CARD_SELECT;
        }
    };

    public static final NavigableMap<String, Supplier<Transform>> TRANSFORM_FACTORIES
            =Collections.unmodifiableNavigableMap(new TreeMap<String, Supplier<Transform>>(Map.ofEntries(
            Map.entry("cluster", Transform.Cluster::create),
            Map.entry("hue", Transform.Hue::new),
            Map.entry("hyper hue", Transform.HyperHue::new),
            Map.entry("intensity", Transform.Intensity::new),
            Map.entry("normalize deviation", Transform.NormalizeDeviation::create),
            Map.entry("normalize min/max", Transform.NormalizeMinMax::new),
            Map.entry(
                    "normalized difference vegetation index",
                    Transform.NormalizedDifferenceVegetationIndex::new),
            Map.entry("normalized hyper hue", Transform.NormalizedHyperHue::create),
            Map.entry("select", Transform.Select::create))));

    public static final Transform.Visitor<String> TRANSFORM_TO_STRING=new Transform.Visitor<>() {
        @Override
        public String cluster(Transform.Cluster cluster) {
            return cluster.type.name+" "+cluster.algorithm.name;
        }

        @Override
        public String hue() {
            return "hue";
        }

        @Override
        public String hyperHue() {
            return "hyper hue";
        }

        @Override
        public String intensity() {
            return "intensity";
        }

        @Override
        public String normalizeDeviation(Transform.NormalizeDeviation normalizeDeviation) {
            return "normalize deviation, sigma="+normalizeDeviation.sigma;
        }

        @Override
        public String normalizeMinMax() {
            return "normalize min/max";
        }

        @Override
        public String normalizedDifferenceVegetationIndex() {
            return "normalized difference vegetation index";
        }

        @Override
        public String normalizedHyperHue(Transform.NormalizedHyperHue normalizedHyperHue) {
            return "normalized hyper hue, max.zero="+normalizedHyperHue.maxZero;
        }

        @Override
        public String select(Transform.Select select) {
            StringBuilder sb=new StringBuilder();
            sb.append("select ");
            for (int ii=0; select.selectedChannels.size()>ii; ++ii) {
                if (0<ii) {
                    sb.append(",");
                }
                sb.append(select.selectedChannels.get(ii));
            }
            return sb.toString();
        }
    };

    private Outputs() {
    }

    public static Mask mask(HalfPlane halfPlane) {
        return Mask.halfPlane(halfPlane.x1, halfPlane.y1, halfPlane.x2, halfPlane.y2);
    }

    public static Mask mask(List<HalfPlane> mask) {
        List<Mask> masks=new ArrayList<>(mask.size());
        mask.forEach((mask2)->masks.add(mask(mask2)));
        return Mask.and(masks);
    }

    public static Function<Image, Image> transformToImageMap(Mask mask, Transform transform) {
        return transform.visit(new Transform.Visitor<>() {
            @Override
            public Function<Image, Image> cluster(Transform.Cluster cluster) {
                return (image)->{
                    List<InitialCenters<KDTree>> initialCenters=new ArrayList<>();
                    if (cluster.initialCentersMeanAndFarthest) {
                        initialCenters.add(InitialCenters.meanAndFarthest(false));
                    }
                    if (cluster.initialCentersKDTree) {
                        initialCenters.add(KDTree.initialCenters(false));
                    }
                    for (int ii=cluster.initialCentersRandom; 0<ii; --ii) {
                        initialCenters.add(InitialCenters.random());
                    }

                    List<ReplaceEmptyCluster<KDTree>> replaceEmptyClusters=new ArrayList<>();
                    if (cluster.replaceEmptyClustersFarthest) {
                        replaceEmptyClusters.add(ReplaceEmptyCluster.farthest(false));
                    }
                    for (int ii=cluster.replaceEmptyClustersRandom; 0<ii; --ii) {
                        replaceEmptyClusters.add(ReplaceEmptyCluster.random());
                    }

                    ClusteringStrategy<KDTree> strategy;
                    if (Transform.Cluster.Algorithm.ISODATA.equals(cluster.algorithm)) {
                        if (initialCenters.isEmpty()) {
                            throw new RuntimeException("no strategy to select initial centers");
                        }
                        if (replaceEmptyClusters.isEmpty()) {
                            throw new RuntimeException("no strategy to select replacement for empty clusters");
                        }
                        List<ClusteringStrategy<KDTree>> strategies=new ArrayList<>();
                        initialCenters.forEach((init)->replaceEmptyClusters.forEach((replace)->
                                strategies.add(ClusteringStrategy.isodata(
                                        cluster.minClusters,
                                        cluster.maxClusters,
                                        cluster.errorLimit,
                                        cluster.maxIterations,
                                        cluster.theta_N,
                                        cluster.lumping,
                                        cluster.L,
                                        cluster.std_deviation,
                                        init,
                                        replace))));
                        strategy=ClusteringStrategy.best(strategies);
                    }
                    else {
                        Function<Integer, ClusteringStrategy<KDTree>> strategyGenerator;
                        switch (cluster.algorithm) {
                            case K_MEANS:
                                if (initialCenters.isEmpty()) {
                                    throw new RuntimeException("no strategy to select initial centers");
                                }
                                if (replaceEmptyClusters.isEmpty()) {
                                    throw new RuntimeException("no strategy to select replacement for empty clusters");
                                }
                                strategyGenerator=(clusters)->{
                                    List<ClusteringStrategy<KDTree>> strategies=new ArrayList<>();
                                    initialCenters.forEach((init)->replaceEmptyClusters.forEach((replace)->
                                            strategies.add(ClusteringStrategy.kMeans(
                                                    clusters,
                                                    cluster.errorLimit,
                                                    init,
                                                    cluster.maxIterations,
                                                    replace))));
                                    return ClusteringStrategy.best(strategies);
                                };
                                break;
                            case OTSU:
                                strategyGenerator=(clusters)->ClusteringStrategy.otsuLinear(cluster.bins, clusters);
                                break;
                            case OTSU_CIRCULAR:
                                strategyGenerator=(clusters)->ClusteringStrategy.otsuCircular(cluster.bins, clusters);
                                break;
                            default:
                                throw new RuntimeException("unexpected cluster algorithm "+cluster.algorithm);
                        }
                        strategy=(cluster.minClusters<cluster.maxClusters)
                                ?ClusteringStrategy.elbow(
                                        cluster.errorLimit,
                                        cluster.maxClusters,
                                        cluster.minClusters,
                                        strategyGenerator,
                                        1)
                                :strategyGenerator.apply(cluster.maxClusters);
                    }
                    switch (cluster.type) {
                        case CLUSTER_1:
                            return Cluster1.create(
                                    image, ClusterColors.RGB.falseColor(0, 1, 2), mask, strategy);
                        case CLUSTER_HUE:
                            return Cluster2.createHue(image, mask, strategy);
                        case CLUSTER_HYPER_HUE:
                            return Cluster2.createHyperHue(image, mask, strategy);
                        default:
                            throw new RuntimeException("unexpected cluster type "+cluster.type);
                    }
                };
            }

            @Override
            public Function<Image, Image> hue() {
                return Hue::create;
            }

            @Override
            public Function<Image, Image> hyperHue() {
                return HyperHue::create;
            }

            @Override
            public Function<Image, Image> intensity() {
                return Intensity::create;
            }

            @Override
            public Function<Image, Image> normalizeDeviation(Transform.NormalizeDeviation normalizeDeviation) {
                return (image)->Normalize.createDeviation(image, mask, normalizeDeviation.sigma);
            }

            @Override
            public Function<Image, Image> normalizeMinMax() {
                return (image)->Normalize.createMinMax(image, mask);
            }

            @Override
            public Function<Image, Image> normalizedDifferenceVegetationIndex() {
                return NormalizedDifferenceVegetationIndex::create;
            }

            @Override
            public Function<Image, Image> normalizedHyperHue(Transform.NormalizedHyperHue normalizedHyperHue) {
                return (image)->NormalizedHyperHue.create(image, normalizedHyperHue.maxZero);
            }

            @Override
            public Function<Image, Image> select(Transform.Select select) {
                int[] selectedDimensions=new int[select.selectedChannels.size()];
                for (int ii=0; selectedDimensions.length>ii; ++ii) {
                    selectedDimensions[ii]=select.selectedChannels.get(ii);
                }
                return (image)->Select.create(image, selectedDimensions);
            }
        });
    }
}
