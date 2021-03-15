package dog.giraffe.points;

import dog.giraffe.QuickSort;
import dog.giraffe.Sum;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.function.DoubleBinaryOperator;
import java.util.function.Function;

public abstract class KDTree<P extends L2Points<P> & QuickSort.Swap> extends L2Points<KDTree<P>> {
    private static final DoubleBinaryOperator ADD=Double::sum;
    private static final DoubleBinaryOperator MAX=Math::max;
    private static final DoubleBinaryOperator MIN=Math::min;

    private static class Branch<P extends L2Points<P> & QuickSort.Swap> extends KDTree<P> {
        private final KDTree<P> left;
        private final KDTree<P> right;

        public Branch(KDTree<P> left, KDTree<P> right) {
            super(
                    left.dimensions,
                    perform(MAX, left.max, right.max),
                    perform(MIN, left.min, right.min),
                    left.size()+right.size(),
                    perform(ADD, left.sum, right.sum));
            this.left=left;
            this.right=right;
        }

        @Override
        public <C> void classify(
                Function<C, double[]> centerPoint, List<C> centers,
                Classification<C, Distance, Mean, KDTree<P>, double[]> classification) {
            centers=filterCenters(centerPoint, centers);
            left.classify(centerPoint, centers, classification);
            right.classify(centerPoint, centers, classification);
        }

        @Override
        public double get(int dimension, int index) {
            return (left.size()>index)
                    ?left.get(dimension, index)
                    :right.get(dimension, index-left.size());
        }

        private static double[] perform(DoubleBinaryOperator operator, double[] value0, double[] value1) {
            double[] result=new double[value0.length];
            for (int dd=0; result.length>dd; ++dd) {
                result[dd]=operator.applyAsDouble(value0[dd], value1[dd]);
            }
            return result;
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        protected boolean split(Deque<KDTree<P>> deque) {
            deque.addFirst(right);
            deque.addFirst(left);
            return true;
        }
    }

    private static class Leaf<P extends L2Points<P> & QuickSort.Swap> extends KDTree<P> {
        private final int offset;
        private final P points;

        public Leaf(int offset, P points, int size, List<Sum> sums) {
            super(points.dimensions(),
                    points.perform(Double.NEGATIVE_INFINITY, offset, MAX, size),
                    points.perform(Double.POSITIVE_INFINITY, offset, MIN, size),
                    size,
                    points.sum(offset, size, sums));
            this.offset=offset;
            this.points=points;
        }

        @Override
        public <C> void classify(
                Function<C, double[]> centerPoint, List<C> centers,
                Classification<C, Distance, Mean, KDTree<P>, double[]> classification) {
            centers=filterCenters(centerPoint, centers);
            if (1==centers.size()) {
                classification.nearestCenter(centers.get(0), this);
            }
            else {
                super.classify(centerPoint, centers, classification);
            }
        }

        @Override
        public double get(int dimension, int index) {
            return points.get(dimension, offset+index);
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        protected boolean split(Deque<KDTree<P>> deque) {
            return false;
        }
    }

    private final double[] max;
    private final double[] mean;
    private final double[] min;
    private final double[] sum;
    protected final int size;

    private KDTree(int dimensions, double[] max, double[] min, int size, double[] sum) {
        super(dimensions);
        this.max=max;
        this.min=min;
        this.size=size;
        this.sum=sum;
        mean=Arrays.copyOf(sum, sum.length);
        for (int dd=0; dimensions>dd; ++dd) {
            mean[dd]/=size;
        }
    }

    @Override
    public void addAllTo(Mean mean) {
        mean.addAll(size(), sum);
    }

    public static <P extends L2Points<P> & QuickSort.Swap> KDTree<P> create(
            int maxLeafSize, P points, Sum.Factory sum) {
        List<Sum> sums=new ArrayList<>(points.dimensions());
        for (int dd=0; points.dimensions()>dd; ++dd) {
            sums.add(sum.create(maxLeafSize));
        }
        return create(0, maxLeafSize, points, sums, points.size());
    }

    private static <P extends L2Points<P> & QuickSort.Swap> KDTree<P> create(
            int from, int maxLeafSize, P points, List<Sum> sums, int to) {
        if ((2>maxLeafSize)
                || (maxLeafSize>=to-from)) {
            return new Leaf<>(from, points, to-from, sums);
        }
        int widestDimension=points.widestDimension(from, to);
        int middle=QuickSort.medianSplit(
                (index0, index1)->
                        Double.compare(points.get(widestDimension, index0), points.get(widestDimension, index1)),
                from,
                points,
                to);
        return new Branch<>(
                create(from, maxLeafSize, points, sums, middle),
                create(middle, maxLeafSize, points, sums, to));
    }

    protected <C> List<C> filterCenters(Function<C, double[]> centerPoint, List<C> centers) {
        if (1>=centers.size()) {
            return centers;
        }
        int nc=0;
        double nd=Double.POSITIVE_INFINITY;
        for (int cc=0; centers.size()>cc; ++cc) {
            double di=distance().distance(centerPoint.apply(centers.get(cc)), mean);
            if (nd>di) {
                nc=cc;
                nd=di;
            }
        }
        List<C> centers2=new ArrayList<>(centers.size());
        centers2.add(centers.get(nc));
        double[] nc2=centerPoint.apply(centers2.get(0));
        double[] ex=new double[dimensions];
        for (int cc=0; centers.size()>cc; ++cc) {
            if (cc==nc) {
                continue;
            }
            C cc2=centers.get(cc);
            double[] cc3=centerPoint.apply(cc2);
            for (int dd=0; dimensions>dd; ++dd) {
                ex[dd]=((cc3[dd]>nc2[dd])?max:min)[dd];
            }
            if (nd>=distance().distance(cc3, ex)) {
                centers2.add(cc2);
            }
        }
        return Collections.unmodifiableList(centers2);
    }

    @Override
    public KDTree<P> self() {
        return this;
    }

    protected abstract boolean split(Deque<KDTree<P>> deque);

    @Override
    public List<KDTree<P>> split(int parts) {
        if (2>parts) {
            return Collections.singletonList(this);
        }
        int maxSize=size()/(4*parts);
        Deque<KDTree<P>> deque=new ArrayDeque<>();
        List<KDTree<P>> trees=new ArrayList<>();
        deque.addFirst(this);
        while (!deque.isEmpty()) {
            KDTree<P> tree=deque.removeFirst();
            if ((tree.size()<=maxSize)
                    || (!tree.split(deque))) {
                trees.add(tree);
            }
        }
        if (trees.size()<=parts) {
            return Collections.unmodifiableList(new ArrayList<>(trees));
        }
        List<KDTree<P>> trees2=new ArrayList<>(parts);
        for (int ii=0; parts>ii; ++ii) {
            trees2.add(split(ii*trees.size()/parts, (ii+1)*trees.size()/parts, trees));
        }
        return Collections.unmodifiableList(new ArrayList<>(trees2));
    }

    private KDTree<P> split(int from, int to, List<KDTree<P>> trees) {
        if (2>to-from) {
            return trees.get(from);
        }
        int middle=(from+to)/2;
        return new Branch<>(
                split(from, middle, trees),
                split(middle, to, trees));
    }
}
