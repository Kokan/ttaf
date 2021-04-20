package dog.giraffe.image;

import dog.giraffe.Context;
import dog.giraffe.points.MutablePoints;
import dog.giraffe.threads.AsyncSupplier;
import dog.giraffe.threads.Continuation;
import dog.giraffe.threads.Continuations;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public interface ImageTransform {
    interface Closeable {
        void close() throws Throwable;

        static void closeAll(List<? extends Closeable> list, Throwable throwable) throws Throwable {
            while (!list.isEmpty()) {
                Closeable closeable=list.remove(list.size()-1);
                try {
                    closeable.close();
                }
                catch (Throwable throwable2) {
                    if (null==throwable) {
                        throwable=throwable2;
                    }
                    else {
                        throwable.addSuppressed(throwable2);
                    }
                }
            }
            if (null!=throwable) {
                throw throwable;
            }
        }
    }

    interface PrepareLine extends Closeable {
        void prepare(Context context, ImageReader imageReader, MutablePoints inputLine) throws Throwable;
    }

    interface Write extends Closeable {
        void write(
                Context context, MutablePoints inputLine, ImageWriter.Line outputLine, int dimension) throws Throwable;
    }

    int dimensions();

    static ImageTransform noop() {
        return new ImageTransform() {
            private final Write write=new Write() {
                @Override
                public void close() {
                }

                @Override
                public void write(
                        Context context, MutablePoints inputLine, ImageWriter.Line outputLine, int dimension) {
                }
            };

            @Override
            public int dimensions() {
                return 0;
            }

            @Override
            public void prepare(ImageReader imageReader) {
            }

            @Override
            public void prepare(
                    Context context, ImageReader imageReader, Continuation<Void> continuation) throws Throwable {
                continuation.completed(null);
            }

            @Override
            public PrepareLine prepareLine(Context context, ImageReader imageReader) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean prepareLines() {
                return false;
            }

            @Override
            public Write write(Context context, ImageReader imageReader, ImageWriter imageWriter, int dimension) {
                return write;
            }
        };
    }

    void prepare(ImageReader imageReader) throws Throwable;

    void prepare(Context context, ImageReader imageReader, Continuation<Void> continuation) throws Throwable;

    /**
     * Multiple lines may run in parallel.
     */
    PrepareLine prepareLine(Context context, ImageReader imageReader) throws Throwable;

    boolean prepareLines();

    static <T> void run(
            Context context, ImageReader.Factory reader, ImageWriter.Factory<T> writer,
            List<ImageTransform> transforms, Continuation<T> continuation) throws Throwable {
        reader.run(
                context,
                new ImageReader.ReadProcess<>() {
                    private void prepare(
                            Context context, ImageReader imageReader, int index, Continuation<T> continuation)
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

                    private void prepareLines(
                            Context context, ImageReader imageReader, Continuation<T> continuation) throws Throwable {
                        int threads=Math.max(1, Math.min(context.executor().threads(), imageReader.height()));
                        List<ImageTransform> prepareTransforms=transforms.stream()
                                .filter(ImageTransform::prepareLines)
                                .collect(Collectors.toList());
                        List<AsyncSupplier<Void>> forks=new ArrayList<>(threads);
                        for (int tt=0; threads>tt; ++tt) {
                            int from=tt*imageReader.height()/threads;
                            int to=(tt+1)*imageReader.height()/threads;
                            forks.add((continuation2)->{
                                Throwable throwable=null;
                                List<PrepareLine> prepareLines=new ArrayList<>(prepareTransforms.size());
                                try {
                                    for (ImageTransform transform: prepareTransforms) {
                                        prepareLines.add(transform.prepareLine(context, imageReader));
                                    }
                                    MutablePoints inputLine=imageReader.createPoints(imageReader.width());
                                    for (int yy=from; to>yy; ++yy) {
                                        inputLine.clear();
                                        imageReader.addLineTo(yy, inputLine);
                                        for (PrepareLine prepareLine: prepareLines) {
                                            prepareLine.prepare(context, imageReader, inputLine);
                                        }
                                    }
                                }
                                catch (Throwable throwable2) {
                                    throwable=throwable2;
                                }
                                Closeable.closeAll(prepareLines, throwable);
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
                    public void run(
                            Context context, ImageReader imageReader, Continuation<T> continuation) throws Throwable {
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

                    private void write(
                            Context context, ImageReader imageReader, ImageWriter imageWriter,
                            Continuation<Void> continuation) throws Throwable {
                        int threads=Math.max(1, Math.min(context.executor().threads(), imageReader.height()));
                        List<AsyncSupplier<Void>> forks=new ArrayList<>(threads);
                        for (int tt=0; threads>tt; ++tt) {
                            int from=tt*imageReader.height()/threads;
                            int to=(tt+1)*imageReader.height()/threads;
                            forks.add((continuation2)->{
                                Throwable throwable=null;
                                List<Write> writes=new ArrayList<>(transforms.size());
                                try {
                                    for (int dd=0, tr=0; transforms.size()>tr; ++tr) {
                                        ImageTransform transform=transforms.get(tr);
                                        writes.add(transform.write(context, imageReader, imageWriter, dd));
                                        dd+=transform.dimensions();
                                    }
                                    MutablePoints inputLine=imageReader.createPoints(imageReader.width());
                                    for (int yy=from; to>yy; ++yy) {
                                        inputLine.clear();
                                        imageReader.addLineTo(yy, inputLine);
                                        ImageWriter.Line outputLine=imageWriter.getLine(yy);
                                        for (int dd=0, tr=0; transforms.size()>tr; ++tr) {
                                            ImageTransform transform=transforms.get(tr);
                                            writes.get(tr).write(context, inputLine, outputLine, dd);
                                            dd+=transform.dimensions();
                                        }
                                        outputLine.write();
                                    }
                                }
                                catch (Throwable throwable2) {
                                    throwable=throwable2;
                                }
                                Closeable.closeAll(writes, throwable);
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
    Write write(Context context, ImageReader imageReader, ImageWriter imageWriter, int dimension) throws Throwable;
}
