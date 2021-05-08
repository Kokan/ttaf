package dog.giraffe.image.transform;

import dog.giraffe.Context;
import dog.giraffe.util.Pair;
import dog.giraffe.points.Sum;
import dog.giraffe.image.Image;
import dog.giraffe.points.FloatArrayPoints;
import dog.giraffe.points.MutablePoints;
import dog.giraffe.threads.Continuation;
import dog.giraffe.threads.Continuations;
import java.util.Arrays;
import java.util.Map;

/**
 * Normalizes the range of the components of an image.
 */
public abstract class Normalize extends Image.Transform {
    private static class Deviation extends Normalize {
        private static final class Result {
            private final Sum[] moments1;
            private final Sum[] moments2;
            private final int points;

            public Result(Sum[] moments1, Sum[] moments2, int points) {
                this.moments1=moments1;
                this.moments2=moments2;
                this.points=points;
            }
        }

        private final double sigma;

        public Deviation(Image image, Mask mask, double sigma) {
            super(image, mask);
            this.sigma=sigma;
        }

        @Override
        public void log(Map<String, Object> log) {
            log.put("type", "normalized-deviation");
            log.put("sigma", sigma);
        }

        @Override
        protected void prepareImpl2(
                Context context, Continuation<Pair<double[], double[]>> continuation) throws Throwable {
            Continuations.<Result>forkJoin(
                    (from, to)->(continuation2)->{
                        Sum[] moments1=new Sum[image.dimensions()];
                        Sum[] moments2=new Sum[image.dimensions()];
                        int points=0;
                        for (int dd=0; image.dimensions()>dd; ++dd) {
                            moments1[dd]=context.sum().create((to-from)*image.width());
                            moments2[dd]=context.sum().create((to-from)*image.width());
                        }
                        MutablePoints line=image.createPoints(image.dimensions(), image.width());
                        line.size(image.width());
                        Reader reader=image.reader();
                        for (int yy=from; to>yy; ++yy) {
                            reader.setNormalizedLineTo(yy, line, 0);
                            for (int xx=0; image.width()>xx; ++xx) {
                                if (mask.visible(xx, yy)) {
                                    ++points;
                                    for (int dd=0; image.dimensions()>dd; ++dd) {
                                        double cc=line.getNormalized(dd, xx);
                                        moments1[dd].add(cc);
                                        moments2[dd].add(cc*cc);
                                    }
                                }
                            }
                        }
                        continuation2.completed(new Result(moments1, moments2, points));
                    },
                    0,
                    image.height(),
                    Continuations.map(
                            (result, continuation2)->{
                                double[] lengths=new double[image.dimensions()];
                                double[] minimums=new double[image.dimensions()];
                                double points=0.0;
                                for (Result result2: result) {
                                    points+=result2.points;
                                }
                                if (0.0>=points) {
                                    Arrays.fill(lengths, 1.0);
                                }
                                else {
                                    for (int dd=0; image.dimensions()>dd; ++dd) {
                                        Sum sum=context.sum().create(image.height()*image.width());
                                        for (Result result2: result) {
                                            result2.moments1[dd].addTo(sum);
                                        }
                                        double moment1=sum.sum();
                                        sum.clear();
                                        for (Result result2: result) {
                                            result2.moments2[dd].addTo(sum);
                                        }
                                        double moment2=sum.sum();
                                        double mean=moment1/points;
                                        double deviation=Math.sqrt(moment2*points-moment1*moment1)/points;
                                        minimums[dd]=Math.max(0.0, mean-sigma*deviation);
                                        double max=Math.min(1.0, mean+sigma*deviation);
                                        lengths[dd]=(max<=minimums[dd])?1.0:(max-minimums[dd]);
                                    }
                                }
                                continuation2.completed(new Pair<>(minimums, lengths));
                            },
                            continuation),
                    context.executor());
        }
    }

    private static class MinMax extends Normalize {
        public MinMax(Image image, Mask mask) {
            super(image, mask);
        }

        @Override
        public void log(Map<String, Object> log) {
            log.put("type", "normalized-min-max");
        }

        @Override
        protected void prepareImpl2(
                Context context, Continuation<Pair<double[], double[]>> continuation) throws Throwable {
            Continuations.<Pair<double[], double[]>>forkJoin(
                    (from, to)->(continuation2)->{
                        double[] maximums=new double[image.dimensions()];
                        double[] minimums=new double[image.dimensions()];
                        Arrays.fill(maximums, 0.0);
                        Arrays.fill(minimums, 1.0);
                        MutablePoints line=image.createPoints(image.dimensions(), image.width());
                        line.size(image.width());
                        Reader reader=image.reader();
                        for (int yy=from; to>yy; ++yy) {
                            reader.setNormalizedLineTo(yy, line, 0);
                            for (int xx=0; image.width()>xx; ++xx) {
                                if (mask.visible(xx, yy)) {
                                    for (int dd=0; image.dimensions()>dd; ++dd) {
                                        double cc=line.getNormalized(dd, xx);
                                        maximums[dd]=Math.max(maximums[dd], cc);
                                        minimums[dd]=Math.min(minimums[dd], cc);
                                    }
                                }
                            }
                        }
                        continuation2.completed(new Pair<>(maximums, minimums));
                    },
                    0,
                    image.height(),
                    Continuations.map(
                            (result, continuation2)->{
                                double[] lengths=new double[image.dimensions()];
                                double[] maximums=new double[image.dimensions()];
                                double[] minimums=new double[image.dimensions()];
                                for (int dd=0; image.dimensions()>dd; ++dd) {
                                    maximums[dd]=0.0;
                                    minimums[dd]=1.0;
                                    for (Pair<double[], double[]> pair: result) {
                                        maximums[dd]=Math.max(maximums[dd], pair.first[dd]);
                                        minimums[dd]=Math.min(minimums[dd], pair.second[dd]);
                                    }
                                    lengths[dd]=(maximums[dd]<=minimums[dd])?1.0:(maximums[dd]-minimums[dd]);
                                }
                                continuation2.completed(new Pair<>(minimums, lengths));
                            },
                            continuation),
                    context.executor());
        }
    }

    private double[] lengths;
    protected final Mask mask;
    private double[] minimums;

    private Normalize(Image image, Mask mask) {
        super(image);
        this.mask=mask;
    }


    /**
     * Creates a new {@link Normalize} instance. The input range [mean-sigma*deviation, mean+sigma*deviation]
     * is mapped to the output range [0, 1].
     *
     * @param mask pixels masked out are not considered in the input range
     */
    public static Image createDeviation(Image image, Mask mask, double sigma) {
        return new Deviation(image, mask, sigma);
    }

    /**
     * Creates a new {@link Normalize} instance. The input range [min component value, max component value]
     * is mapped to the output range [0, 1].
     *
     * @param mask pixels masked out are not considered in the input range
     */
    public static Image createMinMax(Image image, Mask mask) {
        return new MinMax(image, mask);
    }

    @Override
    public MutablePoints createPoints(int dimensions, int expectedSize) {
        return new FloatArrayPoints(dimensions, expectedSize);
    }

    @Override
    protected void prepareImpl(Context context, Continuation<Dimensions> continuation) throws Throwable {
        prepareImpl2(
                context,
                Continuations.map(
                        (result, continuation2)->{
                            lengths=result.second;
                            minimums=result.first;
                            continuation2.completed(new Dimensions(image));
                        },
                        continuation));
    }

    protected abstract void prepareImpl2(
            Context context, Continuation<Pair<double[], double[]>> continuation) throws Throwable;

    @Override
    public Reader reader() throws Throwable {
        return new TransformReader() {
            @Override
            protected void setNormalizedLineToTransform(MutablePoints points, int offset) {
                for (int xx=0; width()>xx; ++xx, ++offset) {
                    for (int dd=0; dimensions()>dd; ++dd) {
                        points.setNormalized(
                                dd,
                                offset,
                                Math.max(0.0, Math.min(1.0,
                                        (line.getNormalized(dd, xx)-minimums[dd])/lengths[dd])));
                    }
                }
            }
        };
    }
}
