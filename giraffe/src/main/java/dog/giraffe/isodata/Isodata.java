package dog.giraffe.isodata;

import dog.giraffe.ReplaceEmptyCluster;
import dog.giraffe.CannotSelectInitialCentersException;
import dog.giraffe.InitialCenters;
import dog.giraffe.Clusters;
import dog.giraffe.Context;
import dog.giraffe.points.L2Points;
import dog.giraffe.Distance;
import dog.giraffe.Vector;
import dog.giraffe.Sum;
import dog.giraffe.VectorMean;
import dog.giraffe.points.Points;
import dog.giraffe.threads.AsyncFunction;
import dog.giraffe.threads.AsyncSupplier;
import dog.giraffe.threads.Continuation;
import dog.giraffe.threads.Continuations;
import dog.giraffe.VectorStdDeviation;
import dog.giraffe.MaxComponent;
import dog.giraffe.MeanDouble;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.AbstractMap.SimpleEntry;
import java.util.function.Function;
import java.util.Comparator;

public class Isodata<P extends L2Points<P>> {
    public static class Clusterss {
      public final List<Cluster> clusters;
      public final List<Vector> points;
      public Double avg_dist;

      public Clusterss(List<Cluster> clusters, List<Vector> points) {
         this.clusters = clusters;
         this.points = points;
      }

      public List<Vector> getCenters() {
         List<Vector> centers = new ArrayList<>();
         for (Cluster cluster : clusters) {
             centers.add(cluster.center);
         }
         return centers;
      }

      public Cluster get(Vector center) throws Throwable {
         for (Cluster cluster : clusters) {
             if (cluster.center.equals(center)) {
                return cluster;
             }
         }
         throw new Exception();
      }
    }

    public static class Cluster {
      public List<Vector> points;
      public Vector center;

      public Double avg_dist;
      public Vector std_dev;

      public Cluster(List<Vector> points, Vector center) {
        this.points = points;
        this.center = center;
      }

      public Vector point() {
         return center;
      }
    }

    private final Function<Cluster, Vector> centerPoint=Cluster::point;
    private int N_c;
    private final int K;
    private final int L;
    private final int theta_N;
    private double lumping;
    private final double std_deviation;
    private final Context context;
    private final double errorLimit;
    private final int maxIterations;
    private final List<List<L2Points.Mean>> means;
    private final P points;
    private final List<P> points2;
    private final ReplaceEmptyCluster<L2Points.Distance, L2Points.Mean, P, Vector> replaceEmptyCluster;
    private final List<Sum> sums;

    public static class StdDeviation implements VectorStdDeviation<Vector> {
        public static class Factory implements VectorStdDeviation.Factory<Vector> {
            private final VectorMean.Factory<L2Points.Mean,Vector> meanFactory;
            private final Sum.Factory sumFactory;

            public Factory(VectorMean.Factory<L2Points.Mean,Vector> meanFactory, Sum.Factory sumFactory) {
                 this.meanFactory = meanFactory;
                 this.sumFactory = sumFactory;
            }

            @Override
            public VectorStdDeviation<Vector> create(Vector mean, int addends) {
                   return new StdDeviation(mean, addends, meanFactory, sumFactory);
            }
        }

        private int addends;
        private final Vector meanValue;
        private final VectorMean<L2Points.Mean,Vector> mean;

        public StdDeviation(Vector mean, int addends, VectorMean.Factory<L2Points.Mean,Vector> meanFactory, Sum.Factory sumFactory) {
          this.addends = 0;
          this.meanValue = mean;
          this.mean = meanFactory.create(addends, sumFactory);
        }

        @Override
        public VectorStdDeviation<Vector> add(Vector addend) {
            ++addends;
            mean.add(addend.sub(meanValue).pow());
            return this;
        }

        @Override
        public VectorStdDeviation<Vector> clear() {
            mean.clear();
            addends=0;
            return this;
        }

        @Override
        public Vector mean() {
            return meanValue.sqrt();
        }

        @Override
        public Vector deviation() {
            if (addends==0) throw new RuntimeException("dividing by zero");
            return mean.mean().div(addends).sqrt();
        }
    }

    private final VectorStdDeviation.Factory<Vector> devFactory;

    public static class Comp implements MaxComponent<Double, Vector> {
       private int maxInd(Vector self) {
          Double max = self.coordinate(0);
          int maxid = 0;
          for (int i=1;i<self.dimensions();++i) {
            if (max < self.coordinate(i)) {
              max = self.coordinate(i);
              maxid = i;
            }
          }
          return maxid;
       }
         
       @Override
       public Double max(Vector self) { 
          return self.coordinate(maxInd(self));
       }

       @Override
       public Vector maxVec(Vector self) {
          Vector v = new Vector(self.dimensions());

          int maxid = maxInd(self);
          v.coordinate(maxid, self.coordinate(maxid));

          return v;
       }
    }
    private final MaxComponent<Double,Vector> max = new Comp();

    private Isodata(
            int N_c,
            int K,
            int theta_N,
            double lumping,
            int L,
            double std_deviation,
            Context context,
            double errorLimit,
            int maxIterations,
            List<List<L2Points.Mean>> means,
            P points,
            List<P> points2,
            ReplaceEmptyCluster<L2Points.Distance, L2Points.Mean, P, Vector> replaceEmptyCluster,
            List<Sum> sums){
        this.N_c=N_c;
        this.K=K;
        this.L=L;
        this.theta_N=theta_N;
        this.lumping=lumping;
        this.std_deviation=std_deviation;
        this.context=context;
        this.errorLimit=errorLimit;
        this.maxIterations=maxIterations;
        this.means=means;
        this.points=points;
        this.points2=points2;
        this.replaceEmptyCluster=replaceEmptyCluster;
        this.sums=sums;
        this.devFactory = new StdDeviation.Factory(points.mean(), context.sum());
    }

    public static <P extends L2Points<P>>
    void cluster(
            int N_c,
            int K,
            Context context,
            Continuation<Clusters<Vector>> continuation,
            double errorLimit,
            InitialCenters<L2Points.Distance, L2Points.Mean, P, Vector> initialCenters,
            int maxIterations,
            P points,
            ReplaceEmptyCluster<L2Points.Distance, L2Points.Mean, P, Vector> replaceEmptyCluster) throws Throwable {
        if (0>=N_c) {
            continuation.failed(new IllegalStateException(Integer.toString(N_c)));
            return;
        }
        if (points.size()<N_c) {
            continuation.failed(new CannotSelectInitialCentersException(String.format(
                    "too few data points; N_c: %1$d, data points: %2$d", N_c, points.size())));
            return;
        }
        List<Vector> pointss = new ArrayList<>(points.size());
        List<P> points2=points.split(context.executor().threads());
        List<List<L2Points.Mean>> means=new ArrayList<>(points2.size());
        List<Sum> sums=new ArrayList<>(points2.size());
        for (P points3: points2) {
            List<L2Points.Mean> means2=new ArrayList<>(N_c);
            for (int ii=N_c; 0<ii; --ii) {
                means2.add(points.mean().create((means.isEmpty()?points:points3).size(), context.sum()));
            }
            means.add(Collections.unmodifiableList(means2));
            sums.add(context.sum().create((sums.isEmpty()?points:points3).size()));
        }
        List<Vector> pointlist = new ArrayList<>();
        for (int i = 0;i<points.size(); ++i) {
            pointlist.add(points.get(i));
        }
        initialCenters.initialCenters(
                N_c,
                context,
                maxIterations,
                points,
                points2,
                Continuations.map(
                        (centers, continuation2)->{
                            if (centers.size()!=N_c) {
                                continuation2.failed(new CannotSelectInitialCentersException());
                                return;
                            }
                            double error=Double.POSITIVE_INFINITY;
                            List<Cluster> clusters = new ArrayList<>(points.size());
                            for (Vector center : centers) {
                              clusters.add(new Cluster(new ArrayList<>(), center));
                            }
                            Clusterss p = new Clusterss(clusters, pointlist);
                            Isodata<P> isodata=new Isodata<>(
                                    N_c,
                                    K,
                                    (int)(0.05*points.size()),
                                    0.3,
                                    3,
                                    0.01,
                                    context,
                                    errorLimit,
                                    maxIterations,
                                    Collections.unmodifiableList(means),
                                    points,
                                    points2,
                                    replaceEmptyCluster,
                                    Collections.unmodifiableList(sums)
                            );
                            isodata.start(p, Continuations.map((res,cont)->{
                                               List<Vector> cl = new ArrayList<>();
                                               for (Vector c : res.keySet()) {
                                                  cl.add(c);
                                               }
                                               cont.completed(new Clusters<>(cl, error));
                                             },continuation), error, 0);
                        },
                        continuation));
    }

    public void start(Clusterss p, Continuation<Map<Vector,List<Vector>>> continuation, double error, int iteration) throws Throwable {
                distribute(p, continuation, error, 0);
    }

    // Step 2, distribute points between cluster centers
    public void distribute(Clusterss p, Continuation<Map<Vector,List<Vector>>> continuation, double error, int iteration) throws Throwable {
        context.checkStopped();
        if (maxIterations<=iteration) {
            Map<Vector,List<Vector>> a=new HashMap<>();
            for (Vector center : p.getCenters()) {
                a.put(center,new ArrayList<>());
            }
            continuation.completed(a);
            return;
        }


        List<Vector> points = Collections.unmodifiableList(p.points);
        List<Vector> centers = p.getCenters();


        int threads=Math.max(1, Math.min(points.size(), context.executor().threads()));
        List<AsyncSupplier<Map.Entry<Sum, Map<Vector, List<Vector>>>>> forks=new ArrayList<>(threads);
        for (int tt=0; threads>tt; ++tt) {
            int start=tt*points.size()/threads;
            int end=(tt+1)*points.size()/threads;
            forks.add((continuation2)->{
                Map<Vector, List<Vector>> voronoi=new HashMap<>(centers.size());
                for (Vector center: centers) {
                    voronoi.put(center, new ArrayList<>(end-start));
                }
                Sum errorSum=context.sum().create(end-start);
                for (int ii=start; end>ii; ++ii) {
                    Vector point=points.get(ii);
                    Vector center=nearestCenter(centers, this.points.distance(), point);
                    errorSum.add(this.points.distance().distance(center, point));
                    voronoi.get(center).add(point);
                }
                continuation2.completed(new SimpleEntry<>(errorSum, voronoi));
            });
        }
        Continuations.forkJoin(
                forks,
                Continuations.map(
                        (results, continuation2)->{
                            Sum errorSum=context.sum().create(points.size());
                            for (Vector center: p.getCenters()) {
                                List<Vector> cluster=new ArrayList<>(points.size());
                                p.get(center).points.clear();
                            }
                            for (Map.Entry<Sum, Map<Vector, List<Vector>>> result: results) {
                                for (Map.Entry<Vector, List<Vector>> entry: result.getValue().entrySet()) {
                                    p.get(entry.getKey()).points.addAll(entry.getValue());
                                }
                            }
                            discard_sample(p, continuation2, error, iteration);
                        },
                        continuation),
                context.executor());
    }

    // Step 3, discard samples that has fewer then theta_N number of points
    public void discard_sample(Clusterss p, Continuation<Map<Vector,List<Vector>>> continuation, double error, int iteration)
            throws Throwable {
         List<Cluster> clusters2=new ArrayList<>();
         boolean discarded=false;
         for (Vector v : p.getCenters()) {
             Cluster cluster=p.get(v);
             if (cluster.points.size() >= theta_N) {
                clusters2.add(cluster);
                discarded=true;
             }
         }
         N_c=clusters2.size();
         p.clusters.clear();
         p.clusters.addAll(clusters2);

         update_centers(p, continuation, error, iteration);
    }

    private List<Map.Entry<Integer,Integer>> nthSmallest(List<Map.Entry<Integer,Integer>> filter, double dist[][], int n) {
       filter.sort(new Comparator<Map.Entry<Integer,Integer>>() {
                          @Override
                          public int compare(Map.Entry<Integer,Integer> m1, Map.Entry<Integer,Integer> m2) {
                                  if (dist[m1.getKey()][m1.getValue()] < dist[m2.getKey()][m2.getValue()]) return -1;
                                  if (dist[m1.getKey()][m1.getValue()] > dist[m2.getKey()][m2.getValue()]) return +1;
                                  return 0;
                           }});
      return filter;
    }

    private Cluster merge_cluster(Cluster z1, Cluster z2) {
         int N_z1 = Math.max(1,z1.points.size());
         int N_z2 = Math.max(1,z2.points.size());
         Vector new_center = z1.center.mul(N_z1).add(z2.center.mul(N_z2)).div(N_z1+N_z2);
         List<Vector> points = z1.points;      
         points.addAll(z2.points);
         Cluster z_3 = new Cluster(points, new_center);

         return z_3;
    }
   

    // Step 11, Step 12, finding pairwise center distance and lumping clusters
    public void lumping(Clusterss p, Continuation<Map<Vector,List<Vector>>> continuation, double error, int iteration)
            throws Throwable {
         double dist[][] = new double[p.clusters.size()][p.clusters.size()];
         List<Map.Entry<Integer,Integer>> map = new ArrayList<>();
         for (int i=0;i<p.clusters.size();++i) {
              dist[i][i] = 0; //not lumped
         }
         for (int i=0;i<p.clusters.size();++i) {
             for (int j=i+1;j<p.clusters.size();++j) {
                 double d_ij= points.distance().distance(p.clusters.get(i).center, p.clusters.get(j).center);
                 dist[i][j] = d_ij;
                 if (i!=j && d_ij < lumping) {
                     map.add(new SimpleEntry<>(i,j));
                 }
             }
         }

         nthSmallest(map, dist, L);

         List<Cluster> new_clusters = new ArrayList<>();
         for (Map.Entry<Integer, Integer> pair : map) {
             int i = pair.getKey();
             int j = pair.getValue();

             if (dist[i][i] == 1 || dist[j][j] == 1) continue;//either of them lumped we skip

             new_clusters.add(merge_cluster(p.clusters.get(i), p.clusters.get(j)));
             dist[i][i] = dist[j][j] = 1;
         }

         for (int i=0;i<p.clusters.size();++i) {
              if (dist[i][i] == 0) { //not lumped
                 new_clusters.add(p.clusters.get(i));
              }
         }
         p.clusters.clear();
         p.clusters.addAll(new_clusters);
         N_c=p.clusters.size();

         distribute(p, continuation, error, iteration+1);
    }

    // Step 10, split clusters
    public void split_cluster(Clusterss p, Continuation<Map<Vector,List<Vector>>> continuation, double error, int iteration)
            throws Throwable {
        boolean split=false;
        List<Cluster> new_clusters = new ArrayList<>();
        List<Cluster> rm_clusters = new ArrayList<>();
        for (Cluster cluster : p.clusters) {
                double ro_max = max.max(cluster.std_dev);
                final Vector ro_maxVec = max.maxVec(cluster.std_dev);
                if ((ro_max > std_deviation) &&
                    ((cluster.avg_dist > p.avg_dist) && (cluster.points.size() > 2 * (theta_N + 1)) ||
                     (p.clusters.size() <= K/2))) {
                        split=true;
                        Cluster z_plus = new Cluster(new ArrayList<>(), cluster.center.add(ro_maxVec));
                        Cluster z_minus = new Cluster(new ArrayList<>(), cluster.center.sub(ro_maxVec));

                        rm_clusters.add(cluster);
                        new_clusters.add(z_plus);
                        new_clusters.add(z_minus);

                }
        }
        p.clusters.removeAll(rm_clusters);
        p.clusters.addAll(new_clusters);
        N_c=p.clusters.size();
        if (split) {
           distribute(p, continuation, error, iteration+1);
        }
        else {
           lumping(p, continuation, error, iteration);
        }
    }

    //Step 4, update each cluster centers
    public void update_centers(Clusterss p, Continuation<Map<Vector,List<Vector>>> continuation, double error, int iteration) throws Throwable {
        List<Cluster> clusters = Collections.unmodifiableList(p.clusters);
        List<Vector> points = Collections.unmodifiableList(p.points);

        List<AsyncSupplier<Map.Entry<Vector, Vector>>> forks=new ArrayList<>(clusters.size());
        for (Cluster cluster : clusters) {
            forks.add((continuation2)->{
                VectorMean<L2Points.Mean,Vector> mean=this.points.mean().create(cluster.points.size(), context.sum());
                for (Vector point : cluster.points) {
                    mean.add(point);
                }
                cluster.center = mean.mean();
                continuation2.completed(new SimpleEntry<>(cluster.center, mean.mean()));
            });
        }
        Continuations.forkJoin(
                forks,
                Continuations.map(
                        (results, continuation2)->{
                            for (Map.Entry<Vector, Vector> pair : results) {
                                 p.get(pair.getKey()).center = pair.getValue();
                            }
                            avg_distance(p, continuation2, error, iteration);
                        },
                        continuation),
                context.executor());
    }

    //Step 5, compute avg distance D_j
    public void avg_distance(Clusterss p, Continuation<Map<Vector,List<Vector>>> continuation, double error, int iteration) throws Throwable {
        List<Cluster> clusters = Collections.unmodifiableList(p.clusters);

        List<AsyncSupplier<Map.Entry<Vector, Double>>> forks=new ArrayList<>(clusters.size());
        for (Cluster cluster : clusters) {
            forks.add((continuation2)->{
                MeanDouble mean = new MeanDouble(cluster.points.size(), context.sum());
                for (Vector point: cluster.points) {
                    mean=mean.add(points.distance().distance(cluster.center, point));
                }
                continuation2.completed(new SimpleEntry<>(cluster.center, mean.mean()));
            });
        }
        Continuations.forkJoin(
                forks,
                Continuations.map(
                        (results, continuation2)->{
                            for (Map.Entry<Vector, Double> pair : results) {
                                 p.get(pair.getKey()).avg_dist = pair.getValue();
                            }
                            all_avg_distance(p, continuation2, error, iteration);
                        },
                        continuation),
                context.executor());
    }

    //Step 6, compute overall avg distance L2Points.Distance
    public void all_avg_distance(Clusterss p, Continuation<Map<Vector,List<Vector>>> continuation, double error, int iteration) throws Throwable {
        double all_avg_distance = 0.0;

        for (Cluster cluster : p.clusters) {
           all_avg_distance += cluster.points.size() * cluster.avg_dist;
        }
        all_avg_distance /= p.points.size();
        p.avg_dist = all_avg_distance;

       select_next_action(p, continuation, error, iteration);       
    }

    //Step 7, select next action
    public void select_next_action(Clusterss p, Continuation<Map<Vector,List<Vector>>> continuation, double error, int iteration) throws Throwable {
       if (iteration + 1 == maxIterations) {
          lumping = 0;
          lumping(p, continuation, error, iteration);
          return;
       }
       if (p.clusters.size() <= K/2) {
          std_deviation(p, continuation, error, iteration);
          return;
       }
       if (iteration % 2 == 0 || p.clusters.size() >= 2*K) {
          lumping(p, continuation, error, iteration);
          return;
       }

       std_deviation(p, continuation, error, iteration);
    }

    //Step 8, find std deviation vector
    public void std_deviation(Clusterss p, Continuation<Map<Vector,List<Vector>>> continuation, double error, int iteration) throws Throwable {
        List<Cluster> clusters = Collections.unmodifiableList(p.clusters);

        List<AsyncSupplier<Map.Entry<Vector, Vector>>> forks=new ArrayList<>(clusters.size());
        for (Cluster cluster : clusters) {
            forks.add((continuation2)->{
                VectorStdDeviation<Vector> dev = devFactory.create(cluster.center, 0);
                for (Vector point: cluster.points) {
                    dev=dev.add(point);
                }
                continuation2.completed(new SimpleEntry<>(cluster.center, dev.deviation()));
            });
        }
        Continuations.forkJoin(
                forks,
                Continuations.map(
                        (results, continuation2)->{
                            for (Map.Entry<Vector, Vector> pair : results) {
                                 p.get(pair.getKey()).std_dev = pair.getValue();
                            }
                            split_cluster(p, continuation2, error, iteration);
                        },
                        continuation),
                context.executor());
    }

    public static <Vector> Vector nearestCenter(Iterable<Vector> centers, Distance<Vector> distance, Vector point) {
        Vector bestCenter=null;
        double bestDistance=Double.POSITIVE_INFINITY;
        for (Vector cc: centers) {
            double dd=distance.distance(cc, point);
            if (dd<bestDistance) {
                bestCenter=cc;
                bestDistance=dd;
            }
        }
        if (null==bestCenter) {
            throw new RuntimeException("cannot select nearest center");
        }
        return bestCenter;
    }

}
