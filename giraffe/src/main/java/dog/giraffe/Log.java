package dog.giraffe;

import dog.giraffe.cluster.Clusters;
import dog.giraffe.image.Image;
import dog.giraffe.image.ImageWriter;
import dog.giraffe.points.Vector;
import dog.giraffe.util.Lists;
import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Supertype of classes that can log metadata.
 */
@FunctionalInterface
public interface Log {
    /**
     * Writes metadata to the map log.
     */
    void log(Map<String, Object> log) throws Throwable;

    /**
     * Writes coordinates and selected colors of clusters to the metadata.
     */
    static void logClusters(Clusters clusters, Map<Vector, Vector> colorMap, Map<String, Object> log) {
        log.put("clusters", clusters.centers.size());
        for (int ii=0; clusters.centers.size()>ii; ++ii) {
            List<Vector> center=clusters.centers.get(ii);
            List<List<Double>> center2=new ArrayList<>(center.size());
            for (Vector vector: center) {
                center2.add(Lists.toList(vector));
            }
            log.put(String.format("clusters-center%1$02d", ii), Collections.unmodifiableList(center2));
            log.put(String.format("clusters-color%1$02d", ii), Lists.toList(colorMap.get(center.get(0))));
        }
        clusters.stats.forEach((key, value) -> log.put("stats-"+key, value));
        log.put("clusters-stats-size", clusters.stats.size());
        log.put("clusters-error", clusters.error);
    }

    /**
     * Writes the time took to complete clustering to the metadata.
     */
    static void logElapsedTime(Instant start, Instant end, Map<String, Object> log) {
        log.put("elapsed-time", Duration.between(start, end));
    }

    /**
     * Prepends a name to metadata keys.
     */
    static void logField(String name, Log field, Map<String, Object> log) throws Throwable {
        Map<String, Object> temp=new LinkedHashMap<>();
        field.log(temp);
        temp.forEach((key, value)->log.put(name+"-"+key, value));
    }

    /**
     * Writes metadata of an {@link dog.giraffe.image.Image Image} generator DAG.
     */
    static void logImages(Image image, Map<String, Object> log) throws Throwable {
        logImages(image, log, new IdentityHashMap<>(), new IdentityHashMap<>());
    }

    private static void logImages(
            Image image, Map<String, Object> log, Map<Image, String> names, Map<Image, Void> seen) throws Throwable {
        if (seen.containsKey(image)) {
            return;
        }
        seen.put(image, null);
        List<Image> dependencies=image.dependencies();
        for (Image dependency: dependencies) {
            logImages(dependency, log, names, seen);
        }
        String name=String.format("image%1$02d", names.size());
        names.put(image, name);
        for (int ii=0; dependencies.size()>ii; ++ii) {
            log.put(String.format("%1$s-dependency%2$02d", name, ii), names.get(dependencies.get(ii)));
        }
        logField(name, image, log);
    }

    /**
     * Writers the metadata of an {@link dog.giraffe.image.ImageWriter ImageWtiter}.
     */
    static void logWriter(ImageWriter writer, Map<String, Object> log) throws Throwable {
        logField("writer", writer, log);
    }

    /**
     * Writes metadata to the file specified by path.
     * Each key-value pair will produce a line of text.
     */
    static void write(Path path, Map<String, Object> log) throws Throwable {
        try (OutputStream stream=Files.newOutputStream(path)) {
            write(stream, log);
        }
    }

    /**
     * Writes metadata to the stream.
     * Each key-value pair will produce a line of text.
     */
    static void write(OutputStream stream, Map<String, Object> log) throws Throwable {
        try (OutputStream bos=new BufferedOutputStream(stream);
                Writer wr=new OutputStreamWriter(bos, StandardCharsets.UTF_8);
                PrintWriter pw=new PrintWriter(wr)) {
            String newLine=System.getProperty("line.separator");
            for (Map.Entry<String, Object> entry: log.entrySet()) {
                pw.write(entry.getKey());
                pw.write(": ");
                pw.write(String.valueOf(entry.getValue()));
                pw.write(newLine);
            }
        }
    }
}
