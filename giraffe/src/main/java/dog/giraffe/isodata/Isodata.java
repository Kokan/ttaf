package dog.giraffe.isodata;

import dog.giraffe.Clusters;
import dog.giraffe.Context;
import dog.giraffe.MeanDouble;
import dog.giraffe.Sum;
import dog.giraffe.kmeans.CannotSelectInitialCentersException;
import dog.giraffe.kmeans.InitialCenters;
import dog.giraffe.kmeans.ReplaceEmptyCluster;
import dog.giraffe.points.Distance;
import dog.giraffe.points.Mean;
import dog.giraffe.points.Points;
import dog.giraffe.points.Variance;
import dog.giraffe.points.Vector;
import dog.giraffe.threads.AsyncSupplier;
import dog.giraffe.threads.Continuation;
import dog.giraffe.threads.Continuations;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class Isodata<P extends Points> {
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
    private final List<List<Mean>> means;
    private final P points;
    private final List<Points> points2;
    private final ReplaceEmptyCluster<P> replaceEmptyCluster;
    private final List<Sum> sums;
    private final Sum sum;
    private double prevError;

    private final Map<String, Double> stats;

    private void increment(String name) {
        double new_value = stats.containsKey(name) ? stats.get(name) + 1 : 1;

        stats.put(name, new_value);
    }

    private void stats_set(String name, Double value) {
        stats.put(name, value);
    }

    private void reset(String name) {
        stats.put(name, 0.0);
    }

    public Map<String, Double> getStats() {
        return stats;
    }

    public void printStats() {
        System.out.println("Isodata stats:");
        System.out.println("=========================");
        for (Map.Entry<String, Double> stat : stats.entrySet()) {
            System.out.println(stat.getKey() + ": " + stat.getValue());
        }
    }

    public static class Comp {
       private int maxInd(Vector self) {
          double max = self.coordinate(0);
          int maxid = 0;
          for (int i=1;i<self.dimensions();++i) {
            if (max < self.coordinate(i)) {
              max = self.coordinate(i);
              maxid = i;
            }
          }
          return maxid;
       }
         
       public Double max(Vector self) {
          return self.coordinate(maxInd(self));
       }

       public Vector maxVec(Vector self) {
          Vector v = new Vector(self.dimensions());

          int maxid = maxInd(self);
          v.coordinate(maxid, self.coordinate(maxid));

          return v;
       }
    }

    private final Comp max = new Comp();

    private Isodata(
            int N_c,
            int K,
            double theta_N,
            double lumping,
            int L,
            double std_deviation,
            Context context,
            double errorLimit,
            int maxIterations,
            List<List<Mean>> means,
            P points,
            List<Points> points2,
            ReplaceEmptyCluster<P> replaceEmptyCluster,
            List<Sum> sums,
            Sum sum){
        this.N_c=N_c;
        this.K=K;
        this.L=L;
        this.theta_N=(int)(theta_N * points.size());
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
        this.sum=sum;
        this.prevError=0.0;
        this.stats=new HashMap<>();
    }

    public static <P extends Points> void cluster(
            int N_c,
            int K,
            double theta_N,
            double lumping,
            int L,
            double std_deviation,
            Context context,
            Continuation<Clusters> continuation,
            double errorLimit,
            InitialCenters<P> initialCenters,
            int maxIterations,
            P points,
            ReplaceEmptyCluster<P> replaceEmptyCluster) throws Throwable {
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
        List<Points> points2=points.split(context.executor().threads());
        List<List<Mean>> means=new ArrayList<>(points2.size());
        List<Sum> sums=new ArrayList<>(points2.size());
        for (Points points3: points2) {
            List<Mean> means2=new ArrayList<>(N_c);
            for (int ii=N_c; 0<ii; --ii) {
                means2.add(points.mean().create((means.isEmpty()?points:points3).size(), context.sum()));
            }
            means.add(Collections.unmodifiableList(means2));
        }

        for (int i=0;i<2*K+1;++i) {
            sums.add(context.sum().create(10));
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
                                    N_c, //number of cluster
                                    K,   //desrired cluster
                                    theta_N,
                                    lumping,
                                    L,
                                    std_deviation,
                                    context,
                                    errorLimit,
                                    maxIterations,
                                    Collections.unmodifiableList(means),
                                    points,
                                    points2,
                                    replaceEmptyCluster,
                                    Collections.unmodifiableList(sums),
                                    context.sum().create(points.size())
                            );
                            isodata.start(p, Continuations.map((res,cont)->{
                                               List<Vector> cl = new ArrayList<>();
                                               for (Vector c : res.keySet()) {
                                                  cl.add(c);
                                               }
                                               cont.completed(Clusters.create(cl, error));
                                             },continuation), error, 0); },
                        continuation));
    }

    public void start(Clusterss p, Continuation<Map<Vector,List<Vector>>> continuation, double error, int iteration) throws Throwable {
                distribute(p, continuation, error, 0);
    }

    // Step 2, distribute points between cluster centers
    public void distribute(Clusterss p, Continuation<Map<Vector,List<Vector>>> continuation, double error, int iteration) throws Throwable {
        context.checkStopped();
        double err = sum.sum();
        double err_bot = prevError*errorLimit;
        if ((maxIterations<=iteration) || (err > 0 && prevError > 0 && (err > err_bot))) {
            Map<Vector,List<Vector>> a=new HashMap<>();
            for (Vector center : p.getCenters()) {
                a.put(center,new ArrayList<>());
            }
            stats_set("error", err);
            stats_set("iteration", (double)iteration);
            stats_set("number_of_cluster", (double)a.size());
            printStats();
            continuation.completed(a);
            return;
        }

        increment("distribute");

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
                    Vector center=nearestCenter(centers, point);
                    errorSum.add(Distance.distance(center, point));
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

         increment("discard_sample");

         List<Cluster> clusters2=new ArrayList<>();
         int discarded=0;
         for (Vector v : p.getCenters()) {
             Cluster cluster=p.get(v);
             if (cluster.points.size() >= theta_N || (N_c-discarded) <= 2) {
                clusters2.add(cluster);
             } else {
                ++discarded;
                increment("discarded_cluster");
             }
         }
         if (discarded > 0) {
            N_c=clusters2.size();
            p.clusters.clear();
            p.clusters.addAll(clusters2);
         }

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
      return filter.subList(0,Math.min(n,filter.size()));
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


         increment("lumping");

         double dist[][] = new double[p.clusters.size()][p.clusters.size()];
         List<Map.Entry<Integer,Integer>> map = new ArrayList<>();
         for (int i=0;i<p.clusters.size();++i) {
              dist[i][i] = 0; //not lumped
         }
         for (int i=0;i<p.clusters.size();++i) {
             for (int j=i+1;j<p.clusters.size();++j) {
                 double d_ij= Distance.distance(p.clusters.get(i).center, p.clusters.get(j).center);
                 dist[i][j] = d_ij;
                 if (i!=j && d_ij < lumping) {
                     map.add(new SimpleEntry<>(i,j));
                 }
             }
         }

         List<Cluster> new_clusters = new ArrayList<>();
         int lumpped=0;
         for (Map.Entry<Integer, Integer> pair : nthSmallest(map, dist, L)) {
             int i = pair.getKey();
             int j = pair.getValue();

             if (dist[i][i] == 1 || dist[j][j] == 1) continue;//either of them lumped we skip
             if (N_c-lumpped <= 2) continue;//we should keep at least two clusters

             new_clusters.add(merge_cluster(p.clusters.get(i), p.clusters.get(j)));
             dist[i][i] = dist[j][j] = 1;
             increment("lumpped_cluster");
             ++lumpped;
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
        increment("split_cluster");
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

                        increment("splitted_cluster");

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

        increment("update_centers");

        List<Cluster> clusters = Collections.unmodifiableList(p.clusters);
        List<Vector> points = Collections.unmodifiableList(p.points);

        List<AsyncSupplier<Map.Entry<Vector, Vector>>> forks=new ArrayList<>(clusters.size());
        for (Cluster cluster : clusters) {
            forks.add((continuation2)->{
                Mean mean=this.points.mean().create(cluster.points.size(), context.sum());
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

        for (Sum sum : sums) {
           sum.clear();
        }

        List<AsyncSupplier<Void>> forks=new ArrayList<>(clusters.size());
        Integer ind=0;
        for (int i=0;i<clusters.size();++i) {//Cluster cluster : clusters) {
            Cluster cluster = clusters.get(i);
            Sum s = sums.get(i);
            forks.add((continuation2)->{
                MeanDouble mean = new MeanDouble(cluster.points.size(), context.sum());
                for (Vector point: cluster.points) {
                    double distance = Distance.distance(cluster.center, point);
                    mean=mean.add(distance);
                    s.add(distance);
                }
                cluster.avg_dist = mean.mean();
                continuation2.completed(null);
            });
        }
        Continuations.forkJoin(
                forks,
                Continuations.map(
                        (results, continuation2)->{
                            prevError = sum.sum();

                            sum.clear();
                            for (Sum s : sums) {
                                sum.add(s.sum());
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

        List<AsyncSupplier<Void>> forks=new ArrayList<>(clusters.size());
        for (Cluster cluster : clusters) {
            forks.add((continuation2)->{
                Variance dev = this.points.variance().create(16, cluster.center, context.sum());
                for (Vector point: cluster.points) {
                    dev.add(point);
                }
                cluster.std_dev = dev.variance();
                continuation2.completed(null);
            });
        }
        Continuations.forkJoin(
                forks,
                Continuations.map(
                        (results, continuation2)->{
                            split_cluster(p, continuation2, error, iteration);
                        },
                        continuation),
                context.executor());
    }

    public static Vector nearestCenter(Iterable<Vector> centers, Vector point) {
        Vector bestCenter=null;
        double bestDistance=Double.POSITIVE_INFINITY;
        for (Vector cc: centers) {
            double dd=Distance.distance(cc, point);
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
