package dog.giraffe;

import java.util.Objects;

public class Otsu2 {
    private final int bins;
    private final double[] centers;
    private final Context context;
    private final int[] frequencies;
    private final int[] frequencySums;
    private double max=Double.NEGATIVE_INFINITY;
    private double min=Double.POSITIVE_INFINITY;
    private final Sum sum;
    private final Iterable<Double> values;

    private Otsu2(int bins, Context context, Sum.Factory sumFactory, Iterable<Double> values) {
        if (2>bins) {
            throw new IllegalArgumentException(Integer.toString(bins));
        }
        this.bins=bins;
        this.context=context;
        this.values=Objects.requireNonNull(values);
        centers=new double[bins];
        frequencies=new int[bins];
        frequencySums=new int[bins+1];
        sum=sumFactory.create(bins);
    }

    private double moment0(int from, int to) {
        return 1.0*(frequencySums[to]-frequencySums[from])/frequencySums[bins];
    }

    private double moment1(int from, int to) {
        sum.clear();
        for (int ii=from; to>ii; ++ii) {
            sum.add(centers[ii]*frequencies[ii]);
        }
        return sum.sum()/(frequencySums[to]-frequencySums[from]);
    }

    private double moment2(int from, int to) {
        sum.clear();
        for (int ii=from; to>ii; ++ii) {
            sum.add(Doubles.square(centers[ii])*frequencies[ii]);
        }
        return sum.sum()/(frequencySums[to]-frequencySums[from]);
    }

    private double threshold() throws Throwable {
        for (double value: values) {
            Doubles.checkFinite(value);
            max=Math.max(max, value);
            min=Math.min(min, value);
        }
        if (!Double.isFinite(max)) {
            throw new IllegalArgumentException("no values");
        }
        for (int ii=0; bins>ii; ++ii) {
            centers[ii]=(min*(2*(bins-ii)-1)+max*(2*ii+1))/(2*bins);
        }
        for (double value: values) {
            Doubles.checkFinite(value);
            int bin;
            if (max<=value) {
                bin=bins-1;
            }
            else if (min>=value) {
                bin=0;
            }
            else {
                bin=(int)(bins*(value-min)/(max-min));
            }
            ++frequencies[bin];
        }
        for (int ii=1; bins>=ii; ++ii) {
            frequencySums[ii]=frequencySums[ii-1]+frequencies[ii-1];
        }
        int bestThreshold=0;
        double bestVariance=Double.POSITIVE_INFINITY;
        for (int tt=0; bins>=tt; ++tt) {
            context.checkStopped();
            double vv=moment0(0, tt)*(moment2(0, tt)-Doubles.square(moment1(0, tt)))
                    +moment0(tt, bins)*(moment2(tt, bins)-Doubles.square(moment1(tt, bins)));
            if (vv<bestVariance) {
                bestThreshold=tt;
                bestVariance=vv;
            }
        }
        return (min*(bins-bestThreshold)+max*bestThreshold)/bins;
    }

    public static double threshold(
            int bins, Context context, Sum.Factory sumFactory, Iterable<Double> values) throws Throwable {
        return new Otsu2(bins, context, sumFactory, values)
                .threshold();
    }
}
