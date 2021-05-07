package dog.giraffe.points;

import dog.giraffe.Context;
import dog.giraffe.Doubles;
import dog.giraffe.QuickSort;
import dog.giraffe.Sum;
import dog.giraffe.kmeans.InitialCenters;
import dog.giraffe.kmeans.ReplaceEmptyCluster;
import dog.giraffe.threads.Continuation;
import dog.giraffe.threads.Continuations;
import dog.giraffe.threads.Function;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.DoubleBinaryOperator;

public abstract class KDTree extends Points {
    private static class NearestCenter {
        public Vector center;
        public double distance;
    }

    private static final DoubleBinaryOperator ADD=Double::sum;
    private static final DoubleBinaryOperator MAX=Math::max;
    private static final DoubleBinaryOperator MIN=Math::min;

    private static class Branch extends KDTree {
        private final KDTree left;
        private final double maxValue;
        private final double minValue;
        private final KDTree right;

        public Branch(KDTree left, KDTree right) {
            super(
                    left.dimensions,
                    perform(MAX, left.max, right.max),
                    perform(MIN, left.min, right.min),
                    left.size()+right.size(),
                    perform(ADD, left.sum, right.sum),
                    perform(ADD, left.sum2, right.sum2));
            this.left=left;
            this.right=right;
            maxValue=left.maxValue();
            minValue=left.minValue();
        }

        @Override
        public <C> void classify(
                Function<C, Vector> centerPoint, List<C> centers, Classification<C> classification) throws Throwable {
            centers=filterCenters(centerPoint, centers);
            left.classify(centerPoint, centers, classification);
            right.classify(centerPoint, centers, classification);
        }

        @Override
        public void forEach(ForEach forEach) {
            left.forEach(forEach);
            right.forEach(forEach);
        }

        @Override
        public double get(int dimension, int index) {
            return (left.size()>index)
                    ?left.get(dimension, index)
                    :right.get(dimension, index-left.size());
        }

        @Override
        public double getNormalized(int dimension, int index) {
            return (left.size()>index)
                    ?left.getNormalized(dimension, index)
                    :right.getNormalized(dimension, index-left.size());
        }

        @Override
        public double maxValue() {
            return maxValue;
        }

        @Override
        public double minValue() {
            return minValue;
        }

        @Override
        protected void nearestCenter(NearestCenter nearestCenter, Vector point) {
            double leftHeuristic=nearestCenterHeuristic(left, point);
            double rightHeuristic=nearestCenterHeuristic(right, point);
            if (leftHeuristic<=rightHeuristic) {
                left.nearestCenter(nearestCenter, point);
                if (nearestCenter.distance<=rightHeuristic) {
                    return;
                }
                Vector leftCenter=nearestCenter.center;
                double leftDistance=nearestCenter.distance;
                right.nearestCenter(nearestCenter, point);
                if (leftDistance<nearestCenter.distance) {
                    nearestCenter.center=leftCenter;
                    nearestCenter.distance=leftDistance;
                }
            }
            else {
                right.nearestCenter(nearestCenter, point);
                if (nearestCenter.distance<=leftHeuristic) {
                    return;
                }
                Vector rightCenter=nearestCenter.center;
                double rightDistance=nearestCenter.distance;
                left.nearestCenter(nearestCenter, point);
                if (rightDistance<nearestCenter.distance) {
                    nearestCenter.center=rightCenter;
                    nearestCenter.distance=rightDistance;
                }
            }
        }

        private double nearestCenterHeuristic(KDTree tree, Vector point) {
            double distance=0.0;
            for (int dd=0; dimensions>dd; ++dd) {
                double c0=point.coordinate(dd);
                double c1=Math.max(tree.min.coordinate(dd), Math.min(tree.max.coordinate(dd), point.coordinate(dd)));
                distance+=Doubles.square(c0-c1);
            }
            return distance;
        }

        private static Vector perform(DoubleBinaryOperator operator, Vector value0, Vector value1) {
            Vector result=new Vector(value0.dimensions());
            for (int dd=0; result.dimensions()>dd; ++dd) {
                result.coordinate(dd, operator.applyAsDouble(value0.coordinate(dd), value1.coordinate(dd)));
            }
            return result;
        }

        @Override
        protected boolean split(Deque<KDTree> deque) {
            deque.addFirst(right);
            deque.addFirst(left);
            return true;
        }
    }

    private static class Leaf extends KDTree {
        private final MutablePoints points;

        public Leaf(MutablePoints points, Sum.Factory sumFactory) {
            super(
                    points.dimensions(),
                    points.perform(Double.NEGATIVE_INFINITY, MAX),
                    points.perform(Double.POSITIVE_INFINITY, MIN),
                    points.size(),
                    points.sum(Doubles.IDENTITY, sums(points, sumFactory)),
                    points.sum(Doubles.SQUARE, sums(points, sumFactory)));
            this.points=points;
        }

        @Override
        public <C> void classify(
                Function<C, Vector> centerPoint, List<C> centers, Classification<C> classification) throws Throwable {
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
            return points.get(dimension, index);
        }

        @Override
        public double getNormalized(int dimension, int index) {
            return points.getNormalized(dimension, index);
        }

        @Override
        public double maxValue() {
            return points.maxValue();
        }

        @Override
        public double minValue() {
            return points.minValue();
        }

        @Override
        protected void nearestCenter(NearestCenter nearestCenter, Vector point) {
            Vector nc=null;
            double nd=Double.POSITIVE_INFINITY;
            for (int oo=0, ss=size; 0<ss; ++oo, --ss) {
                Vector cc=points.get(oo);
                double dd=Distance.distance(cc, point);
                if (nd>dd) {
                    nc=cc;
                    nd=dd;
                }
            }
            nearestCenter.center=nc;
            nearestCenter.distance=nd;
        }

        @Override
        protected boolean split(Deque<KDTree> deque) {
            return false;
        }

        private static List<Sum> sums(Points points, Sum.Factory sumFactory) {
            List<Sum> sums=new ArrayList<>(points.dimensions());
            for (int dd=0; points.dimensions()>dd; ++dd) {
                sums.add(sumFactory.create(points.size()));
            }
            return sums;
        }
    }

    private final Vector max;
    private final Vector mean;
    private final Vector min;
    private final Vector sum;
    private final Vector sum2;
    protected final int size;
    private final double variance;

    private KDTree(int dimensions, Vector max, Vector min, int size, Vector sum, Vector sum2) {
        super(dimensions);
        this.max=max;
        this.min=min;
        this.size=size;
        this.sum=sum;
        this.sum2=sum2;
        mean=new Vector(dimensions);
        double variance2=0.0;
        for (int dd=0; dimensions>dd; ++dd) {
            double s1=sum.coordinate(dd);
            mean.coordinate(dd, s1/size);
            variance2=sum2.coordinate(dd)-s1*s1/size;
        }
        variance=variance2/size;
    }

    @Override
    public void addAllTo(Mean mean) {
        mean.addAll(size(), sum);
    }

    public static KDTree create(int maxLeafSize, MutablePoints points, Sum.Factory sumFactory) {
        if (1>maxLeafSize) {
            throw new IllegalArgumentException(Integer.toString(maxLeafSize));
        }
        if (maxLeafSize>=points.size()) {
            return new Leaf(points, sumFactory);
        }
        int widestDimension=points.widestDimension();
        int middle=QuickSort.medianSplit(
                (index0, index1)->
                        Double.compare(points.get(widestDimension, index0), points.get(widestDimension, index1)),
                0,
                points,
                points.size());
        return new Branch(
                create(maxLeafSize, points.subPoints(0, middle), sumFactory),
                create(maxLeafSize, points.subPoints(middle, points.size()), sumFactory));
    }

    public static void create(
            Context context, int maxLeafSize, MutablePoints points, Continuation<KDTree> continuation)
            throws Throwable {
        if (1>maxLeafSize) {
            throw new IllegalArgumentException(Integer.toString(maxLeafSize));
        }
        if (maxLeafSize>=points.size()) {
            continuation.completed(new Leaf(points, context.sum()));
            return;
        }
        int widestDimension=points.widestDimension();
        int middle=QuickSort.medianSplit(
                (index0, index1)->
                        Double.compare(points.get(widestDimension, index0), points.get(widestDimension, index1)),
                0,
                points,
                points.size());
        Continuations.<KDTree>forkJoin(
                List.of(
                        (continuation2)->create(
                                context, maxLeafSize, points.subPoints(0, middle), continuation2),
                        (continuation2)->create(
                                context, maxLeafSize, points.subPoints(middle, points.size()), continuation2)),
                Continuations.map(
                        (result, continuation2)->continuation2.completed(new Branch(result.get(0), result.get(1))),
                        continuation),
                context.executor());
    }

    protected <C> List<C> filterCenters(Function<C, Vector> centerPoint, List<C> centers) throws Throwable {
        if (1>=centers.size()) {
            return centers;
        }
        int nc=0;
        double nd=Double.POSITIVE_INFINITY;
        for (int cc=0; centers.size()>cc; ++cc) {
            double di=Distance.distance(centerPoint.apply(centers.get(cc)), mean);
            if (nd>di) {
                nc=cc;
                nd=di;
            }
        }
        List<C> centers2=new ArrayList<>(centers.size());
        centers2.add(centers.get(nc));
        Vector nc2=centerPoint.apply(centers2.get(0));
        Vector ex=new Vector(dimensions);
        for (int cc=0; centers.size()>cc; ++cc) {
            if (cc==nc) {
                continue;
            }
            C cc2=centers.get(cc);
            Vector cc3=centerPoint.apply(cc2);
            for (int dd=0; dimensions>dd; ++dd) {
                ex.coordinate(dd, ((cc3.coordinate(dd)>nc2.coordinate(dd))?max:min).coordinate(dd));
            }
            if (Distance.distance(nc2, ex)>Distance.distance(cc3, ex)) {
                centers2.add(cc2);
            }
        }
        return Collections.unmodifiableList(centers2);
    }

    public static InitialCenters<KDTree> initialCenters(boolean notNear) {
        ReplaceEmptyCluster<KDTree> fallback=ReplaceEmptyCluster.farthest(notNear);
        return new InitialCenters<>() {
            @Override
            public void initialCenters(
                    int clusters, Context context, int maxIterations, KDTree points, List<Points> points2,
                    Continuation<List<Vector>> continuation) throws Throwable {
                Deque<KDTree> deque=new ArrayDeque<>(2);
                PriorityQueue<KDTree> queue=new PriorityQueue<>(
                        clusters, Comparator.<KDTree>comparingDouble((tree)->tree.variance).reversed());
                queue.add(points);
                while (queue.size()<clusters) {
                    KDTree tree=queue.remove();
                    if (tree.split(deque)) {
                        while (!deque.isEmpty()) {
                            queue.add(deque.removeFirst());
                        }
                    }
                    else {
                        queue.add(tree);
                        break;
                    }
                }
                Set<Vector> centers=new HashSet<>(queue.size());
                for (KDTree tree: queue) {
                    centers.add(tree.mean);
                }
                InitialCenters.newCenters(
                        centers,
                        clusters,
                        context,
                        continuation,
                        maxIterations,
                        points,
                        points2,
                        fallback,
                        fallback);
            }

            @Override
            public void log(Map<String, Object> log) {
                log.put("type", "kd-tree");
                log.put("not-near", notNear);
            }
        };
    }

    public static Function<Vector, Vector> nearestCenter(List<Vector> centers, Sum.Factory sum) {
        NearestCenter nearestCenter=new NearestCenter();
        KDTree tree=KDTree.create(1, new VectorList(new ArrayList<>(centers)), sum);
        return (point)->{
            tree.nearestCenter(nearestCenter, point);
            return nearestCenter.center;
        };
    }

    public static Function<Vector, Vector> nearestCenter2(List<Vector> centers, Sum.Factory sum) {
        return (16>centers.size())
                ?Distance.nearestCenter(centers)
                :KDTree.nearestCenter(centers, sum);
    }

    protected abstract void nearestCenter(NearestCenter nearestCenter, Vector point);

    @Override
    public int size() {
        return size;
    }

    protected abstract boolean split(Deque<KDTree> deque);

    @Override
    public List<Points> split(int parts) {
        if (2>parts) {
            return Collections.singletonList(this);
        }
        int maxSize=size()/(4*parts);
        Deque<KDTree> deque=new ArrayDeque<>();
        List<KDTree> trees=new ArrayList<>();
        deque.addFirst(this);
        while (!deque.isEmpty()) {
            KDTree tree=deque.removeFirst();
            if ((tree.size()<=maxSize)
                    || (!tree.split(deque))) {
                trees.add(tree);
            }
        }
        if (trees.size()<=parts) {
            return List.copyOf(trees);
        }
        List<KDTree> trees2=new ArrayList<>(parts);
        for (int ii=0; parts>ii; ++ii) {
            trees2.add(split(ii*trees.size()/parts, (ii+1)*trees.size()/parts, trees));
        }
        return List.copyOf(trees2);
    }

    private KDTree split(int from, int to, List<KDTree> trees) {
        if (2>to-from) {
            return trees.get(from);
        }
        int middle=(from+to)/2;
        return new Branch(
                split(from, middle, trees),
                split(middle, to, trees));
    }
}
