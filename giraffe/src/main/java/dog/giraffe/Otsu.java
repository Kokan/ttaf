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

public class Otsu {
    private static class Histogram {
        public final int[] counts;
        public final Sum[] moments1;
        public final Sum[] moments2;

        public Histogram(int[] counts, Sum[] moments1, Sum[] moments2) {
            this.counts=counts;
            this.moments1=moments1;
            this.moments2=moments2;
        }
    }

    private final int[] binCounts;
    private final double[] binMoments1;
    private final double[] binMoments2;
    private final int bins;
    private final int clusters;
    private final Context context;
    private double[] endpoints;
    private final Points points;

    private Otsu(int bins, int clusters, Context context, Points points) {
        this.bins=bins;
        this.clusters=clusters;
        this.context=context;
        this.points=points;
        binCounts=new int[bins+1];
        binMoments1=new double[bins+1];
        binMoments2=new double[bins+1];
    }

    private void histograms(List<Histogram> histograms, Continuation<Clusters> continuation) throws Throwable {
        int[] counts=new int[bins];
        double[] moments1=new double[bins];
        double[] moments2=new double[bins];
        for (Histogram histogram: histograms) {
            for (int bb=0; bins>bb; ++bb) {
                counts[bb]+=histogram.counts[bb];
                moments1[bb]+=histogram.moments1[bb].sum();
                moments2[bb]+=histogram.moments2[bb].sum();
            }
        }
        for (int bb=0; bins>bb; ++bb) {
            binCounts[bb+1]=binCounts[bb]+counts[bb];
            binMoments1[bb+1]=binMoments1[bb]+moments1[bb];
            binMoments2[bb+1]=binMoments2[bb]+moments2[bb];
        }
        threshold(Continuations.map(this::thresholds, continuation));
    }

    private void minMax(List<Pair<Double, Double>> minMaxes, Continuation<Clusters> continuation) throws Throwable {
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
        Continuations.IntForks<Histogram> forks=(from, to)->new AsyncSupplier<>() {
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
            public void get(Continuation<Histogram> continuation2) throws Throwable {
                int[] counts=new int[bins];
                Sum[] moments1=new Sum[bins];
                Sum[] moments2=new Sum[bins];
                for (int bb=0; bins>bb; ++bb) {
                    moments1[bb]=context.sum().create(points.size()/bins+1);
                    moments2[bb]=context.sum().create(points.size()/bins+1);
                }
                for (int ii=from; to>ii; ++ii) {
                    double vv=points.get(0, ii);
                    int bin=bin(vv);
                    ++counts[bin];
                    moments1[bin].add(vv);
                    moments2[bin].add(vv*vv);
                }
                continuation2.completed(new Histogram(counts, moments1, moments2));
            }
        };
        Continuations.forkJoin(
                forks,
                0,
                points.size(),
                Continuations.map(this::histograms, continuation),
                context.executor());
    }

    public static void otsu(
            Context context, int bins, int clusters, Points points, Continuation<Clusters> continuation)
            throws Throwable {
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
                Continuations.map(new Otsu(bins, clusters, context, points)::minMax, continuation),
                context.executor());
    }

    private void threshold(Continuation<Pair<int[], Double>> continuation) throws Throwable {
        int[] thresholds=new int[clusters+1];
        thresholds[clusters]=bins;
        Pair<int[], Double> best=new Pair<>(null, Double.POSITIVE_INFINITY);
        int firstBin=1;
        int lastBin=bins-clusters+1;
        List<AsyncSupplier<Pair<int[], Double>>> forks=new ArrayList<>(lastBin-firstBin+1);
        for (int tt=firstBin; lastBin>=tt; ++tt) {
            int uu=tt;
            forks.add((continuation2)->{
                int[] thresholds2=Arrays.copyOf(thresholds, thresholds.length);
                thresholds2[1]=uu;
                Pair<int[], Double> best2=threshold(
                        best,
                        variance(0, uu),
                        thresholds2,
                        1);
                continuation2.completed(best2);
            });
        }
        Continuations.forkJoin(
                forks,
                Continuations.map(
                        (bests, continuation2)->{
                            Pair<int[], Double> best2=best;
                            for (Pair<int[], Double> best3: bests) {
                                if (best2.second>best3.second) {
                                    best2=best3;
                                }
                            }
                            continuation2.completed(best2);
                        },
                        continuation),
                context.executor());
    }

    private Pair<int[], Double> threshold(
            Pair<int[], Double> best, double error, int[] thresholds, int selectedThresholds) {
        int remainingThreshold=clusters-1-selectedThresholds;
        if (0>=remainingThreshold) {
            thresholds[clusters]=bins;
            error+=variance(thresholds[clusters-1], thresholds[clusters]);
            return (best.second<=error)
                    ?best
                    :new Pair<>(Arrays.copyOf(thresholds, thresholds.length), error);
        }
        int firstBin=thresholds[selectedThresholds]+1;
        int lastBin=bins-remainingThreshold;
        if (firstBin>lastBin) {
            throw new IllegalStateException("empty cluster");
        }
        for (int tt=firstBin; lastBin>=tt; ++tt) {
            thresholds[selectedThresholds+1]=tt;
            best=threshold(
                    best,
                    error+variance(thresholds[selectedThresholds], thresholds[selectedThresholds+1]),
                    thresholds,
                    selectedThresholds+1);
        }
        return best;
    }

    private void thresholds(Pair<int[], Double> thresholds, Continuation<Clusters> continuation) throws Throwable {
        List<List<Vector>> centers=new ArrayList<>(clusters);
        if (0!=thresholds.first[0]) {
            throw new IllegalStateException();
        }
        if (bins!=thresholds.first[clusters]) {
            throw new IllegalStateException();
        }
        for (int cc=0; clusters>cc; ++cc) {
            if (thresholds.first[cc]>=thresholds.first[cc+1]) {
                throw new IllegalStateException();
            }
            double te=endpoints[thresholds.first[cc+1]];
            centers.add(List.of(
                    new Vector(new double[]{endpoints[thresholds.first[cc]]}),
                    new Vector(new double[]{te-Math.ulp(te)})));
        }
        continuation.completed(new Clusters(Collections.unmodifiableList(centers), thresholds.second));
    }

    private double variance(int from, int to) {
        if (from==to) {
            throw new IllegalStateException("empty cluster");
        }
        long count=binCounts[to]-binCounts[from];
        double moment1=binMoments1[to]-binMoments1[from];
        double moment2=binMoments2[to]-binMoments2[from];
        double variance=moment2/count-moment1*moment1/(count*count);
        if (0.0>variance) {
            throw new IllegalStateException();
        }
        return variance;
    }
}
