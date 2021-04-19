package dog.giraffe.image;

import dog.giraffe.Context;
import dog.giraffe.points.L2Points;
import dog.giraffe.threads.AsyncSupplier;
import dog.giraffe.threads.Continuation;
import dog.giraffe.threads.Continuations;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public interface ImageTransform {
    int dimensions();

    static ImageTransform noop() {
        return new ImageTransform() {
            @Override
            public int dimensions() {
                return 0;
            }

            @Override
            public <P extends L2Points.Mutable<P>> void prepare(ImageReader<P> imageReader) {
            }

            @Override
            public <P extends L2Points.Mutable<P>> void prepare(
                    Context context, ImageReader<P> imageReader, Continuation<Void> continuation) throws Throwable {
                continuation.completed(null);
            }

            @Override
            public <P extends L2Points.Mutable<P>> void prepare(
                    Context context, ImageReader<P> imageReader, P inputLine) {
            }

            @Override
            public boolean prepareLines() {
                return false;
            }

            @Override
            public <P extends L2Points.Mutable<P>> void write(
                    Context context, P inputLine, ImageWriter.Line outputLine, int dimension) {
            }
        };
    }

    <P extends L2Points.Mutable<P>> void prepare(ImageReader<P> imageReader) throws Throwable;

    <P extends L2Points.Mutable<P>>
    void prepare(Context context, ImageReader<P> imageReader, Continuation<Void> continuation) throws Throwable;

    /**
     * Multiple lines may run in parallel.
     */
    <P extends L2Points.Mutable<P>> void prepare(
            Context context, ImageReader<P> imageReader, P inputLine) throws Throwable;

    boolean prepareLines();

    static <T> void run(
            Context context, ImageReader.Factory reader, ImageWriter.Factory<T> writer,
            List<ImageTransform> transforms, Continuation<T> continuation) throws Throwable {
        reader.run(
                context,
                new ImageReader.ReadProcess<>() {
                    private <P extends L2Points.Mutable<P>> void prepare(
                            Context context, ImageReader<P> imageReader, int index, Continuation<T> continuation)
                            throws Throwable {
                        if (transforms.size()<=index) {
                            int dimensions=0;
                            for (ImageTransform transform: transforms) {
                                dimensions+=transform.dimensions();
                            }
                            writer.<Void>run(
                                    context,
                                    imageReader.width(),
                                    imageReader.height(),
                                    dimensions,
                                    (context2, imageWriter, continuation2)->
                                            write(context2, imageReader, imageWriter, continuation2),
                                    Continuations.map(
                                            (input, continuation2)->continuation2.completed(input.first),
                                            continuation));
                        }
                        else {
                            transforms.get(index).prepare(
                                    context,
                                    imageReader,
                                    Continuations.async(
                                            Continuations.map(
                                                    (input, continuation2)->prepare(
                                                            context, imageReader, index+1, continuation2),
                                                    continuation),
                                            context.executor()));
                        }
                    }

                    private <P extends L2Points.Mutable<P>> void prepareLines(
                            Context context, ImageReader<P> imageReader, Continuation<T> continuation)
                            throws Throwable {
                        int threads=Math.max(1, Math.min(context.executor().threads(), imageReader.height()));
                        List<ImageTransform> prepareTransforms=transforms.stream()
                                .filter(ImageTransform::prepareLines)
                                .collect(Collectors.toList());
                        List<AsyncSupplier<Void>> forks=new ArrayList<>(threads);
                        for (int tt=0; threads>tt; ++tt) {
                            int from=tt*imageReader.height()/threads;
                            int to=(tt+1)*imageReader.height()/threads;
                            forks.add((continuation2)->{
                                P inputLine=imageReader.createPoints(imageReader.width());
                                for (int yy=from; to>yy; ++yy) {
                                    inputLine.clear();
                                    imageReader.addLineTo(yy, inputLine);
                                    for (ImageTransform transform: prepareTransforms) {
                                        transform.prepare(context, imageReader, inputLine);
                                    }
                                }
                                continuation2.completed(null);
                            });
                        }
                        Continuations.forkJoin(
                                forks,
                                Continuations.map(
                                        (result, continuation2)->prepare(context, imageReader, 0, continuation2),
                                        continuation),
                                context.executor());
                    }

                    @Override
                    public <P extends L2Points.Mutable<P>> void run(
                            Context context, ImageReader<P> imageReader, Continuation<T> continuation)
                            throws Throwable {
                        boolean prepareLines=false;
                        for (ImageTransform transform: transforms) {
                            transform.prepare(imageReader);
                            if (transform.prepareLines()) {
                                prepareLines=true;
                            }
                        }
                        if (prepareLines) {
                            prepareLines(context, imageReader, continuation);
                        }
                        else {
                            prepare(context, imageReader, 0, continuation);
                        }
                    }

                    private <P extends L2Points.Mutable<P>> void write(
                            Context context, ImageReader<P> imageReader, ImageWriter imageWriter,
                            Continuation<Void> continuation) throws Throwable {
                        int threads=Math.max(1, Math.min(context.executor().threads(), imageReader.height()));
                        List<AsyncSupplier<Void>> forks=new ArrayList<>(threads);
                        for (int tt=0; threads>tt; ++tt) {
                            int from=tt*imageReader.height()/threads;
                            int to=(tt+1)*imageReader.height()/threads;
                            forks.add((continuation2)->{
                                P inputLine=imageReader.createPoints(imageReader.width());
                                for (int yy=from; to>yy; ++yy) {
                                    inputLine.clear();
                                    imageReader.addLineTo(yy, inputLine);
                                    ImageWriter.Line outputLine=imageWriter.getLine(yy);
                                    for (int dd=0, tt1=0; transforms.size()>tt1; ++tt1) {
                                        ImageTransform transform=transforms.get(tt1);
                                        transform.write(context, inputLine, outputLine, dd);
                                        dd+=transform.dimensions();
                                    }
                                    outputLine.write();
                                }
                                continuation2.completed(null);
                            });
                        }
                        Continuations.forkJoin(
                                forks,
                                Continuations.map(
                                        (result, continuation2)->continuation2.completed(null),
                                        continuation),
                                context.executor());
                    }
                },
                continuation);
    }

    /**
     * Multiple lines may run in parallel.
     */
    <P extends L2Points.Mutable<P>>
    void write(Context context, P inputLine, ImageWriter.Line outputLine, int dimension) throws Throwable;
}
