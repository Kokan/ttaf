package dog.giraffe;

import dog.giraffe.image.Image;
import dog.giraffe.image.ImageWriter;
import dog.giraffe.points.Vector;
import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@FunctionalInterface
public interface Log {
    void log(Map<String, Object> log) throws Throwable;

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
        log.put("clusters-error", clusters.error);
    }

    static void logElapsedTime(Instant start, Instant end, Map<String, Object> log) {
        log.put("elapsed-time", Duration.between(start, end));
    }

    static void logField(String name, Log field, Map<String, Object> log) throws Throwable {
        Map<String, Object> temp=new LinkedHashMap<>();
        field.log(temp);
        temp.forEach((key, value)->log.put(name+"-"+key, value));
    }

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

    static void logWriter(ImageWriter writer, Map<String, Object> log) throws Throwable {
        logField("writer", writer, log);
    }

    static void write(String logFile, Map<String, Object> log) throws Throwable {
        if (null==logFile) {
            return;
        }
        try (OutputStream fos=Files.newOutputStream(Paths.get(logFile));
                OutputStream bos=new BufferedOutputStream(fos);
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
