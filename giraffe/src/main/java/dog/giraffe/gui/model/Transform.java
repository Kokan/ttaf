package dog.giraffe.gui.model;

import java.util.ArrayList;
import java.util.List;

public interface Transform {
    class Cluster implements Transform {
        public enum Algorithm {
            ISODATA("isodata"),
            K_MEANS("k-means"),
            OTSU("Otsu"),
            OTSU_CIRCULAR("Otsu-circular");

            public final String name;

            Algorithm(String name) {
                this.name=name;
            }

            @Override
            public String toString() {
                return name;
            }
        }

        public enum Type {
            CLUSTER_1("cluster"),
            CLUSTER_HUE("cluster-hue"),
            CLUSTER_HYPER_HUE("cluster-hyper-hue");

            public final String name;

            Type(String name) {
                this.name=name;
            }

            @Override
            public String toString() {
                return name;
            }
        }

        public Cluster.Algorithm algorithm;
        public int bins;
        public double errorLimit;
        public boolean initialCentersKDTree;
        public boolean initialCentersMeanAndFarthest;
        public int initialCentersRandom;
        public int maxClusters;
        public int maxIterations;
        public int minClusters;
        public boolean replaceEmptyClustersFarthest;
        public int replaceEmptyClustersRandom;
        public Cluster.Type type;

        public static Cluster create() {
            Cluster cluster=new Cluster();
            cluster.algorithm=Algorithm.K_MEANS;
            cluster.bins=32;
            cluster.errorLimit=0.95;
            cluster.initialCentersMeanAndFarthest=true;
            cluster.maxClusters=10;
            cluster.maxIterations=10000;
            cluster.minClusters=2;
            cluster.replaceEmptyClustersFarthest=true;
            cluster.type=Type.CLUSTER_1;
            return cluster;
        }

        @Override
        public void fix() {
            algorithm=Model.fix(algorithm, Algorithm.K_MEANS);
            type=Model.fix(type, Type.CLUSTER_1);
        }

        @Override
        public <R> R visit(Visitor<R> visitor) {
            return visitor.cluster(this);
        }
    }

    class Hue implements Transform {
        @Override
        public <R> R visit(Visitor<R> visitor) {
            return visitor.hue(this);
        }
    }

    class HyperHue implements Transform {
        @Override
        public <R> R visit(Visitor<R> visitor) {
            return visitor.hyperHue(this);
        }
    }

    class Intensity implements Transform {
        @Override
        public <R> R visit(Visitor<R> visitor) {
            return visitor.intensity(this);
        }
    }

    class NormalizeDeviation implements Transform {
        public double sigma;

        public static NormalizeDeviation create() {
            NormalizeDeviation normalizeDeviation=new NormalizeDeviation();
            normalizeDeviation.sigma=3.0;
            return normalizeDeviation;
        }

        @Override
        public <R> R visit(Visitor<R> visitor) {
            return visitor.normalizeDeviation(this);
        }
    }

    class NormalizeMinMax implements Transform {
        @Override
        public <R> R visit(Visitor<R> visitor) {
            return visitor.normalizeMinMax(this);
        }
    }

    class NormalizedDifferenceVegetationIndex implements Transform {
        @Override
        public <R> R visit(Visitor<R> visitor) {
            return visitor.normalizedDifferenceVegetationIndex(this);
        }
    }

    class NormalizedHyperHue implements Transform {
        public double maxZero;

        public static NormalizedHyperHue create() {
            NormalizedHyperHue normalizedHyperHue=new NormalizedHyperHue();
            normalizedHyperHue.maxZero=0.01;
            return normalizedHyperHue;
        }

        @Override
        public <R> R visit(Visitor<R> visitor) {
            return visitor.normalizedHyperHue(this);
        }
    }

    class Select implements Transform {
        public List<Integer> selectedChannels;

        public static Select create() {
            Select select=new Select();
            select.selectedChannels=new ArrayList<>();
            select.selectedChannels.add(0);
            select.selectedChannels.add(1);
            select.selectedChannels.add(2);
            return select;
        }

        @Override
        public void fix() {
            selectedChannels=Model.fix(selectedChannels);
        }

        @Override
        public <R> R visit(Visitor<R> visitor) {
            return visitor.select(this);
        }
    }

    interface Visitor<R> {
        R cluster(Cluster cluster);

        R hue(Hue hue);

        R hyperHue(HyperHue hyperHue);

        R intensity(Intensity intensity);

        R normalizeDeviation(NormalizeDeviation normalizeDeviation);

        R normalizeMinMax(NormalizeMinMax normalizeMinMax);

        R normalizedDifferenceVegetationIndex(NormalizedDifferenceVegetationIndex normalizedDifferenceVegetationIndex);

        R normalizedHyperHue(NormalizedHyperHue normalizedHyperHue);

        R select(Select select);
    }

    default void fix() {
    }

    <R> R visit(Visitor<R> visitor);
}
