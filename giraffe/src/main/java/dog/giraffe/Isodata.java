package dog.giraffe;

import dog.giraffe.threads.AsyncSupplier;
import dog.giraffe.threads.Continuation;
import dog.giraffe.threads.Continuations;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Collections;
import java.util.Comparator;

public class Isodata<T extends Arith<T>> {
    private int N_c;
    private final int K;
    private final int L;
    private final int theta_N;
    private double lumping;
    private final double std_deviation;
    private final Context context;
    private final Distance<T> distance;
    private final MaxComponent<Double,T> max;
    private final double errorLimit;
    private final int maxIterations;
    private final VectorMean.Factory<T> meanFactory;
    private final VectorStdDeviation.Factory<T> devFactory;
    //private final List<T> points;
    private final Sum.Factory sumFactory;

    public static class Points<T extends Arith<T>> {
      public final List<Cluster<T>> clusters;
      public final List<T> points;
      public Optional<Double> avg_dist;

      public Points(List<Cluster<T>> clusters, List<T> points) {
         this.clusters = clusters;
         this.points = points;
      }

      public List<T> getCenters() {
         List<T> centers = new ArrayList<>();
         for (Cluster<T> cluster : clusters) {
             centers.add(cluster.center);
         }
         return centers;
      }

      public Cluster<T> get(T center) throws Throwable {
         for (Cluster<T> cluster : clusters) {
             if (cluster.center.equals(center)) {
                return cluster;
             }
         }
         throw new Exception();
      }
    }

    public static class Cluster<T extends Arith<T>> {
      public List<T> points;
      public T center;

      public Optional<Double> avg_dist;
      public Optional<T> std_dev;

      public Cluster(List<T> points, T center) {
        this.points = points;
        this.center = center;
      }
    }

    private Isodata(
            int N_c,
            int K,
            Context context,
            Distance<T> distance,
            MaxComponent<Double,T> max,
            double errorLimit,
            int theta_N,
            double lumping,
            int L,
            double std_deviation,
            int maxIterations,
            VectorMean.Factory<T> meanFactory,
            VectorStdDeviation.Factory<T> devFactory,
            List<T> points,
            Sum.Factory sumFactory) {
        this.N_c=N_c;
        this.K=K;
        this.L=L;
        this.theta_N=theta_N;
        this.lumping=lumping;
        this.std_deviation=std_deviation;
        this.context=context;
        this.distance=distance;
        this.max=max;
        this.errorLimit=errorLimit;
        this.maxIterations=maxIterations;
        this.meanFactory=meanFactory;
        this.devFactory=devFactory;
        //this.points=points;
        this.sumFactory=sumFactory;
    }

    public static <T extends Arith<T>> void cluster(
            int N_c, int K, Context context, Continuation<List<T>> continuation, Distance<T> distance, MaxComponent<Double,T> max, double errorLimit,
            int maxIterations, VectorMean.Factory<T> meanFactory, VectorStdDeviation.Factory<T> devFactory, Sum.Factory sumFactory, Iterable<T> values)
            throws Throwable {
        if (2>N_c) {
            throw new IllegalStateException(Integer.toString(N_c));
        }
        Objects.requireNonNull(values);
        List<T> points=new ArrayList<>();
        for (T value: values) {
            points.add(value);
        }
        if (points.size()<=N_c) {
            throw new RuntimeException("too few data points");
        }
        List<Cluster<T>> clusters = new ArrayList<>(N_c);
        for (int ii=maxIterations*N_c; N_c>clusters.size(); --ii) {
            if (0>=ii) {
                throw new RuntimeException("cannot select initial cluster centers");
            }
            T randomPoint = points.get(context.random().nextInt(points.size()));
            clusters.add(new Cluster<>(new ArrayList<>(), randomPoint));
        }
        Points<T> p = new Points<>(clusters, points);
        double error=Double.POSITIVE_INFINITY;
        new Isodata<>(N_c, K, context, distance, max, errorLimit, (int)(points.size()*0.005), 0.5, 5, 0.001, maxIterations, meanFactory, devFactory, points, sumFactory)
                .start(p, continuation, error, 0);
    }

    // Step 1, parameters
    // K = number of cluster centers desired
    // theta_N = parameter against which the number of sample in a cluster domain is compared
    // theta_s = standard deviation parameter
    // theta_c = lumping parameter
    // L = maximum number pair of cluster centers which can be lumped
    // I = number of iteration allowed
    public void start(Points<T> p, Continuation<List<T>> continuation, double error, int iteration) throws Throwable {
                distribute(p, Continuations.map((res,cont)->{ 
                               cont.completed(new ArrayList<>(res.keySet()));
                           },continuation), error, 0);
    }

    // Step 2, distribute points between cluster centers
    public void distribute(Points<T> p, Continuation<Map<T,List<T>>> continuation, double error, int iteration) throws Throwable {
        context.checkStopped();
        if (maxIterations<=iteration || iteration==10) {
            Map<T,List<T>> a=new HashMap<>();
            for (T center : p.getCenters()) {
                a.put(center,new ArrayList<>());
            }
            continuation.completed(a);
            return;
        }
        List<T> points = Collections.unmodifiableList(p.points);
        List<T> centers = p.getCenters();
        for (T point : points) {
            T center=nearestCenter(centers, distance, point);
            p.get(center).points.add(point);
       
        }
        discard_sample(p, continuation, error, iteration);
    }

    // Step 3, discard samples that has fewer then theta_N number of points
    public void discard_sample(Points<T> p, Continuation<Map<T,List<T>>> continuation, double error, int iteration)
            throws Throwable {
         List<Cluster<T>> clusters2=new ArrayList<>();
         boolean discarded=false;
         for (T v : p.getCenters()) {
             Cluster<T> cluster=p.get(v);
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

    private List<Pair<Integer,Integer>> nthSmallest(List<Pair<Integer,Integer>> filter, double dist[][], int n) {
       filter.sort(new Comparator<Pair<Integer,Integer>>() {
                          @Override
                          public int compare(Pair<Integer,Integer> m1, Pair<Integer,Integer> m2) {
                                  if (dist[m1.first][m1.second] < dist[m2.first][m2.second]) return -1;
                                  if (dist[m1.first][m1.second] > dist[m2.first][m2.second]) return +1;
                                  return 0;
                           }});
      return filter;
    }

    private Cluster<T> merge_cluster(Cluster<T> z1, Cluster<T> z2) {
         int N_z1 = Math.max(1,z1.points.size());
         int N_z2 = Math.max(1,z2.points.size());
         T new_center = z1.center.mul(N_z1).addition(z2.center.mul(N_z2)).div(N_z1+N_z2);
         List<T> points = z1.points;      
         points.addAll(z2.points);
         Cluster<T> z_3 = new Cluster<>(points, new_center);

         return z_3;
    }
   

    // Step 11, Step 12, finding pairwise center distance and lumping clusters
    public void lumping(Points<T> p, Continuation<Map<T,List<T>>> continuation, double error, int iteration)
            throws Throwable {
         double dist[][] = new double[p.clusters.size()][p.clusters.size()];
         List<Pair<Integer,Integer>> map = new ArrayList<>();
         for (int i=0;i<p.clusters.size();++i) {
              dist[i][i] = 0; //not lumped
         }
         for (int i=0;i<p.clusters.size();++i) {
             for (int j=i+1;j<p.clusters.size();++j) {
                 double d_ij= distance.distance(p.clusters.get(i).center, p.clusters.get(j).center);
                 dist[i][j] = d_ij;
                 if (i!=j && d_ij < lumping) {
                     map.add(new Pair<>(i,j));
                 }
             }
         }

         nthSmallest(map, dist, L);

         List<Cluster<T>> new_clusters = new ArrayList<>();
         for (Pair<Integer, Integer> pair : map) {
             int i = pair.first;
             int j = pair.second;

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
    public void split_cluster(Points<T> p, Continuation<Map<T,List<T>>> continuation, double error, int iteration)
            throws Throwable {
        boolean split=false;
        List<Cluster<T>> new_clusters = new ArrayList<>();
        List<Cluster<T>> rm_clusters = new ArrayList<>();
        for (Cluster<T> cluster : p.clusters) {
                double ro_max = max.max(cluster.std_dev.get());
                final T ro_maxVec = max.maxVec(cluster.std_dev.get());
                if ((ro_max > std_deviation) &&
                    ((cluster.avg_dist.get() > p.avg_dist.get()) && (cluster.points.size() > 2 * (theta_N + 1)) ||
                     (p.clusters.size() <= K/2))) {
                        split=true;
                        Cluster<T> z_plus = new Cluster<>(new ArrayList<>(), cluster.center.addition(ro_maxVec));
                        Cluster<T> z_minus = new Cluster<>(new ArrayList<>(), cluster.center.subtract(ro_maxVec));

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
    public void update_centers(Points<T> p, Continuation<Map<T,List<T>>> continuation, double error, int iteration) throws Throwable {
        List<Cluster<T>> clusters = Collections.unmodifiableList(p.clusters);
        List<T> points = Collections.unmodifiableList(p.points);
        for (Cluster<T> cluster: clusters) {
                if (cluster.points.size() > 0) {
                VectorMean<T> mean=meanFactory.create(points.size());
                for (T point: cluster.points) {
                    mean=mean.add(point);
                }
                cluster.center = mean.mean();
                }
        }
        avg_distance(p, continuation, error, iteration);
    }

    //Step 5, compute avg distance D_j
    public void avg_distance(Points<T> p, Continuation<Map<T,List<T>>> continuation, double error, int iteration) throws Throwable {
        List<Cluster<T>> clusters = Collections.unmodifiableList(p.clusters);
        for (Cluster<T> cluster : clusters) {
                VectorMean<Double> mean = new MeanDouble(cluster.points.size(), sumFactory);
                for (T point: cluster.points) {
                    mean=mean.add(distance.distance(cluster.center, point));
                }
                cluster.avg_dist = Optional.of(mean.mean());
        }
        all_avg_distance(p, continuation, error, iteration);
    }

    //Step 6, compute overall avg distance D
    public void all_avg_distance(Points<T> p, Continuation<Map<T,List<T>>> continuation, double error, int iteration) throws Throwable {
        double all_avg_distance = 0.0;

        for (Cluster<T> cluster : p.clusters) {
           all_avg_distance += cluster.points.size() * cluster.avg_dist.get();
        }
        all_avg_distance /= p.points.size();
        p.avg_dist = Optional.of(all_avg_distance);

       select_next_action(p, continuation, error, iteration);       
    }

    //Step 7, select next action
    public void select_next_action(Points<T> p, Continuation<Map<T,List<T>>> continuation, double error, int iteration) throws Throwable {
       if (iteration + 1 == maxIterations) {
          lumping = 0;
          //theta_c = 0;
          //TODO: goto Step 11
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
    public void std_deviation(Points<T> p, Continuation<Map<T,List<T>>> continuation, double error, int iteration) throws Throwable {
        for (Cluster<T> cluster : p.clusters) {
                VectorStdDeviation<T> dev = devFactory.create(cluster.center, 0);
                for (T point: cluster.points) {
                    dev=dev.add(point);
                }
                cluster.std_dev = Optional.of(dev.deviation());
                
        }
        //TODO: goto Step 9/Step 10
        split_cluster(p, continuation, error, iteration);
        //distribute(p, continuation, error, iteration+1);
    }

    public static <T> T nearestCenter(Iterable<T> centers, Distance<T> distance, T point) {
        T bestCenter=null;
        double bestDistance=Double.POSITIVE_INFINITY;
        for (T cc: centers) {
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
