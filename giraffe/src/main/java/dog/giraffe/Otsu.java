package dog.giraffe;

import dog.giraffe.points.Points;
import dog.giraffe.points.Vector;
import dog.giraffe.threads.AsyncSupplier;
import dog.giraffe.threads.Continuation;
import dog.giraffe.threads.Continuations;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public abstract class Otsu {
    private static class Circular extends Otsu {
        public Circular(int bins, int clusters, Context context, Points points) {
            super(bins, clusters, context, points);
        }

        @Override
        protected void cluster(Histogram histogram, Continuation<Clusters> continuation) throws Throwable {
            List<AsyncSupplier<Thresholds>> forks=new ArrayList<>(bins);
            for (int tt=0; bins>tt; ++tt) {
                int uu=tt;
                forks.add((continuation2)->threshold(histogram.shift(uu), continuation2));
            }
            Continuations.forkJoin(
                    forks,
                    Continuations.map(
                            (thresholds, continuation2)->{
                                Pair<Thresholds, Integer> best=Thresholds.best(thresholds);
                                Thresholds thresholds2=best.first;
                                if (0!=thresholds2.thresholds[0]) {
                                    throw new IllegalStateException();
                                }
                                if (bins!=thresholds2.thresholds[clusters]) {
                                    throw new IllegalStateException();
                                }
                                int shift=best.second;
                                int si=Arrays.binarySearch(thresholds2.thresholds, shift);
                                List<List<Vector>> centers=new ArrayList<>(clusters);
                                if (0<=si) {
                                    for (int cc=0; clusters>cc; ++cc) {
                                        if (thresholds2.thresholds[cc]>=thresholds2.thresholds[cc+1]) {
                                            throw new IllegalStateException();
                                        }
                                        if (si>cc) {
                                            centers.add(center(
                                                    thresholds2.thresholds[cc]-shift+bins,
                                                    thresholds2.thresholds[cc+1]-shift+bins));
                                        }
                                        else {
                                            centers.add(center(
                                                    thresholds2.thresholds[cc]-shift,
                                                    thresholds2.thresholds[cc+1]-shift));
                                        }
                                    }
                                }
                                else {
                                    si=-si-2;
                                    for (int cc=0; clusters>cc; ++cc) {
                                        if (thresholds2.thresholds[cc]>=thresholds2.thresholds[cc+1]) {
                                            throw new IllegalStateException();
                                        }
                                        if (si>cc) {
                                            centers.add(center(
                                                    thresholds2.thresholds[cc]-shift+bins,
                                                    thresholds2.thresholds[cc+1]-shift+bins));
                                        }
                                        else if (si<cc) {
                                            centers.add(center(
                                                    thresholds2.thresholds[cc]-shift,
                                                    thresholds2.thresholds[cc+1]-shift));
                                        }
                                        else {
                                            List<Vector> center=new ArrayList<>(4);
                                            center.addAll(center(thresholds2.thresholds[cc]-shift+bins, bins));
                                            center.addAll(center(0, thresholds2.thresholds[cc+1]-shift));
                                            centers.add(Collections.unmodifiableList(center));
                                        }
                                    }
                                }
                                continuation2.completed(
                                        new Clusters(Collections.unmodifiableList(centers), thresholds2.error));
                            },
                            continuation),
                    context.executor());
        }
    }

    private static class Histogram {
        private final int bins;
        private final int[] counts;
        private final int[] countSums;
        private final double[] moment1Sums;
        private final double[] moment2Sums;

        public Histogram(int bins, int[] counts, int[] countSums, double[] moment1Sums, double[] moment2Sums) {
            this.bins=bins;
            this.counts=counts;
            this.countSums=countSums;
            this.moment1Sums=moment1Sums;
            this.moment2Sums=moment2Sums;
        }

        public static Histogram create(List<PreHistogram> histograms) {
            int bins=histograms.get(0).bins;
            int[] counts=new int[bins];
            double[] moment1s=new double[bins];
            double[] moment2s=new double[bins];
            for (PreHistogram histogram: histograms) {
                for (int bb=0; bins>bb; ++bb) {
                    counts[bb]+=histogram.counts[bb];
                    moment1s[bb]+=histogram.moment1s[bb].sum();
                    moment2s[bb]+=histogram.moment2s[bb].sum();
                }
            }
            int[] countSums=new int[bins+1];
            double[] moment1Sums=new double[bins+1];
            double[] moment2Sums=new double[bins+1];
            for (int bb=0; bins>bb; ++bb) {
                countSums[bb+1]=countSums[bb]+counts[bb];
                moment1Sums[bb+1]=moment1Sums[bb]+moment1s[bb];
                moment2Sums[bb+1]=moment2Sums[bb]+moment2s[bb];
            }
            return new Histogram(bins, counts, countSums, moment1Sums, moment2Sums);
        }

        /**
         * Rotates counts right by shift index.
         */
        public Histogram shift(int shift) {
            if ((0>shift) || (bins<=shift)) {
                throw new IllegalArgumentException(String.format("invalid shift: %1$d. bins: %2$d", shift, bins));
            }
            int[] counts=new int[bins];
            System.arraycopy(this.counts, 0, counts, shift, bins-shift);
            System.arraycopy(this.counts, bins-shift, counts, 0, shift);
            int[] countSums=new int[bins+1];
            double[] moment1Sums=new double[bins+1];
            double[] moment2Sums=new double[bins+1];
            for (int bb=0; bins>bb; ++bb) {
                countSums[bb+1]=countSums[bb]+counts[bb];
                moment1Sums[bb+1]=moment1Sums[bb]+counts[bb]*bb;
                moment2Sums[bb+1]=moment2Sums[bb]+counts[bb]*bb*bb;
            }
            return new Histogram(bins, counts, countSums, moment1Sums, moment2Sums);
        }

        public double variance(int from, int to) {
            if (from==to) {
                throw new IllegalStateException("empty cluster");
            }
            long count=countSums[to]-countSums[from];
            if (0>=count) {
                return 0.0;
            }
            double moment1=moment1Sums[to]-moment1Sums[from];
            double moment2=moment2Sums[to]-moment2Sums[from];
            double variance=moment2/count-moment1*moment1/(count*count);
            if (!Double.isFinite(variance)) {
                throw new IllegalStateException();
            }
            return Math.max(0.0, variance);
        }
    }

    private static class Linear extends Otsu {
        private Linear(int bins, int clusters, Context context, Points points) {
            super(bins, clusters, context, points);
        }

        @Override
        protected void cluster(Histogram histogram, Continuation<Clusters> continuation) throws Throwable {
            threshold(histogram, Continuations.map(this::thresholds, continuation));
        }

        private void thresholds(Thresholds thresholds, Continuation<Clusters> continuation) throws Throwable {
            List<List<Vector>> centers=new ArrayList<>(clusters);
            if (0!=thresholds.thresholds[0]) {
                throw new IllegalStateException();
            }
            if (bins!=thresholds.thresholds[clusters]) {
                throw new IllegalStateException();
            }
            for (int cc=0; clusters>cc; ++cc) {
                if (thresholds.thresholds[cc]>=thresholds.thresholds[cc+1]) {
                    throw new IllegalStateException();
                }
                centers.add(center(thresholds.thresholds[cc], thresholds.thresholds[cc+1]));
            }
            continuation.completed(new Clusters(Collections.unmodifiableList(centers), thresholds.error));
        }
    }

    private static class PreHistogram {
        private final int bins;
        private final int[] counts;
        private final Sum[] moment1s;
        private final Sum[] moment2s;

        public PreHistogram(int bins, int[] counts, Sum[] moment1s, Sum[] moment2s) {
            this.bins=bins;
            this.counts=counts;
            this.moment1s=moment1s;
            this.moment2s=moment2s;
        }

        public PreHistogram(int bins, int expectedSize, Sum.Factory sum) {
            this(bins, new int[bins], new Sum[bins], new Sum[bins]);
            for (int ii=0; bins>ii; ++ii) {
                moment1s[ii]=sum.create(expectedSize);
                moment2s[ii]=sum.create(expectedSize);
            }
        }
    }

    private static class Thresholds {
        private final double error;
        private final int[] thresholds;

        public Thresholds(double error, int[] thresholds) {
            this.error=error;
            this.thresholds=thresholds;
        }

        public static Pair<Thresholds, Integer> best(List<Thresholds> thresholds) {
            int bestIndex=-1;
            Thresholds bestThresholds=null;
            for (int ii=0; thresholds.size()>ii; ++ii) {
                Thresholds thresholds2=thresholds.get(ii);
                if ((null==bestThresholds)
                        || (bestThresholds.error>thresholds2.error)) {
                    bestIndex=ii;
                    bestThresholds=thresholds2;
                }
            }
            Objects.requireNonNull(bestThresholds, "bestThresholds");
            return new Pair<>(bestThresholds, bestIndex);
        }

        public static Thresholds better(Thresholds best, double error, int[] thresholds) {
            return ((null==best) || (best.error>error))
                    ?new Thresholds(error, Arrays.copyOf(thresholds, thresholds.length))
                    :best;
        }
    }

    protected final int bins;
    protected final int clusters;
    protected final Context context;
    protected double[] endpoints;
    protected final Points points;

    private Otsu(int bins, int clusters, Context context, Points points) {
        this.bins=bins;
        this.clusters=clusters;
        this.context=context;
        this.points=points;
    }

    protected List<Vector> center(int from, int to) {
        double te=endpoints[to];
        return List.of(
                new Vector(new double[]{endpoints[from]}),
                new Vector(new double[]{te-Math.ulp(te)}));
    }

    public static void circular(
            Context context, int bins, int clusters, Points points, Continuation<Clusters> continuation)
            throws Throwable {
        new Circular(bins, clusters, context, points)
                .otsu(continuation);
    }

    protected abstract void cluster(Histogram histogram, Continuation<Clusters> continuation) throws Throwable;

    private void histograms(List<PreHistogram> histograms, Continuation<Clusters> continuation) throws Throwable {
        cluster(Histogram.create(histograms), continuation);
    }

    protected void minMax(List<Pair<Double, Double>> minMaxes, Continuation<Clusters> continuation) throws Throwable {
        double max=points.minValue();
        double min=points.maxValue();
        for (Pair<Double, Double> pair: minMaxes) {
            max=Math.max(max, pair.second);
            min=Math.min(min, pair.first);
        }
        max+=Math.ulp(max);
        endpoints=new double[bins+1];
        for (int ii=0; bins>=ii; ++ii) {
            endpoints[ii]=((bins-ii)*min+ii*max)/bins;
        }
        for (int ii=0; bins>ii; ++ii) {
            endpoints[ii+1]=Math.max(endpoints[ii+1], endpoints[ii]+Math.ulp(endpoints[ii]));
        }
        Continuations.IntForks<PreHistogram> forks=(from, to)->new AsyncSupplier<>() {
            private int bin(double value) {
                if (endpoints[1]>value) {
                    return 0;
                }
                if (endpoints[bins-1]<=value) {
                    return bins-1;
                }
                int bin=(int)(bins*(value-endpoints[0])/(endpoints[bins]-endpoints[0]));
                if (0>bin) {
                    return 0;
                }
                if (bins<=bin) {
                    return bins-1;
                }
                if (endpoints[bin]>value) {
                    while ((0<bin) && (endpoints[bin]>value)) {
                        --bin;
                    }
                }
                else if (endpoints[bin+1]<=value) {
                    while ((bins-1>bin) && (endpoints[bin+1]<=value)) {
                        ++bin;
                    }
                }
                return bin;
            }

            @Override
            public void get(Continuation<PreHistogram> continuation2) throws Throwable {
                PreHistogram histogram=new PreHistogram(bins, points.size()/bins+1, context.sum());
                for (int ii=from; to>ii; ++ii) {
                    double vv=points.get(0, ii);
                    int bin=bin(vv);
                    ++histogram.counts[bin];
                    histogram.moment1s[bin].add(vv);
                    histogram.moment2s[bin].add(vv*vv);
                }
                continuation2.completed(histogram);
            }
        };
        Continuations.forkJoin(
                forks,
                0,
                points.size(),
                Continuations.map(this::histograms, continuation),
                context.executor());
    }

    public static void linear(
            Context context, int bins, int clusters, Points points, Continuation<Clusters> continuation)
            throws Throwable {
        new Linear(bins, clusters, context, points)
                .otsu(continuation);
    }

    public void otsu(Continuation<Clusters> continuation) throws Throwable {
        if (2>clusters) {
            throw new RuntimeException(String.format("not enough clusters required: %1$d", clusters));
        }
        if (bins<clusters) {
            throw new RuntimeException(String.format(
                    "fewer bins than clusters. bins %1$d, clusters: %2$d", bins, clusters));
        }
        if (1!=points.dimensions()) {
            throw new RuntimeException(String.format(
                    "Otsu's method only works on single dimensional data. dim.: %1$d", points.dimensions()));
        }
        Continuations.forkJoin(
                (from, to)->(continuation2)->{
                    double max=points.minValue();
                    double min=points.maxValue();
                    for (int ii=from; to>ii; ++ii) {
                        double vv=points.get(0, ii);
                        max=Math.max(max, vv);
                        min=Math.min(min, vv);
                    }
                    continuation2.completed(new Pair<>(min, max));
                },
                0,
                points.size(),
                Continuations.map(this::minMax, continuation),
                context.executor());
    }

    protected void threshold(Histogram histogram, Continuation<Thresholds> continuation) throws Throwable {
        int[] thresholds=new int[clusters+1];
        thresholds[clusters]=bins;
        int firstBin=1;
        int lastBin=bins-clusters+1;
        List<AsyncSupplier<Thresholds>> forks=new ArrayList<>(lastBin-firstBin+1);
        for (int tt=firstBin; lastBin>=tt; ++tt) {
            int uu=tt;
            forks.add((continuation2)->{
                int[] thresholds2=Arrays.copyOf(thresholds, thresholds.length);
                thresholds2[1]=uu;
                Thresholds best2=threshold(
                        histogram,
                        null,
                        histogram.variance(0, uu),
                        thresholds2,
                        1);
                continuation2.completed(best2);
            });
        }
        Continuations.forkJoin(
                forks,
                Continuations.map(
                        (bests, continuation2)->continuation2.completed(Thresholds.best(bests).first),
                        continuation),
                context.executor());
    }

    protected Thresholds threshold(
            Histogram histogram, Thresholds best, double error, int[] thresholds, int selectedThresholds) {
        int remainingThreshold=clusters-1-selectedThresholds;
        if (0>=remainingThreshold) {
            return Thresholds.better(
                    best,
                    error+histogram.variance(thresholds[clusters-1], thresholds[clusters]),
                    thresholds);
        }
        int firstBin=thresholds[selectedThresholds]+1;
        int lastBin=bins-remainingThreshold;
        if (firstBin>lastBin) {
            throw new IllegalStateException("empty cluster");
        }
        for (int tt=firstBin; lastBin>=tt; ++tt) {
            thresholds[selectedThresholds+1]=tt;
            best=threshold(
                    histogram,
                    best,
                    error+histogram.variance(thresholds[selectedThresholds], thresholds[selectedThresholds+1]),
                    thresholds,
                    selectedThresholds+1);
        }
        return best;
    }
}
