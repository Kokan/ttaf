package dog.giraffe.image;

import dog.giraffe.Context;
import dog.giraffe.Pair;
import dog.giraffe.threads.Block;
import dog.giraffe.threads.Continuation;
import dog.giraffe.threads.Continuations;
import dog.giraffe.threads.SleepProcess;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PrepareImages {
    private class PrepareContinuation implements Continuation<Void> {
        private final Image image;
        private final Block wakeup;

        public PrepareContinuation(Image image, Block wakeup) {
            this.image=image;
            this.wakeup=wakeup;
        }

        @Override
        public void completed(Void result) throws Throwable {
            failed(null);
        }

        @Override
        public void failed(Throwable throwable) throws Throwable {
            synchronized (lock) {
                prepared.addLast(new Pair<>(image, throwable));
            }
            wakeup.run();
        }
    }

    private final Context context;
    private final Map<Image, Set<Image>> dependencies=new HashMap<>();
    private final Map<Image, Set<Image>> dependents=new HashMap<>();
    private final Object lock=new Object();
    private final Deque<Pair<Image, Throwable>> prepared=new ArrayDeque<>();
    private final ArrayDeque<Pair<Image, Throwable>> prepared2=new ArrayDeque<>();
    private int running;
    private Throwable throwable;

    private PrepareImages(Context context) {
        this.context=context;
    }

    private void dependencies(Image image, Set<Image> parents) throws Throwable {
        if (parents.contains(image)) {
            throw new RuntimeException("image cycle detected "+image);
        }
        if (dependencies.containsKey(image)) {
            return;
        }
        parents.add(image);
        try {
            Set<Image> dependencies2=new HashSet<>(image.dependencies());
            if (dependencies2.isEmpty()) {
                dependencies2.add(null);
            }
            dependencies.put(image, dependencies2);
            dependents.put(image, new HashSet<>());
            dependencies(dependencies2, parents);
        }
        finally {
            parents.remove(image);
        }
    }

    private void dependencies(Iterable<Image> images, Set<Image> parents) throws Throwable {
        for (Image image: images) {
            dependencies(image, parents);
        }
    }

    private void prepare(List<Image> images, Continuation<Void> continuation) throws Throwable {
        dependencies.put(null, new HashSet<>(0));
        dependents.put(null, new HashSet<>(0));
        dependencies(images, new HashSet<>());
        dependencies.forEach((dependent, dependencies)->
            dependencies.forEach((dependency)->
                    dependents.get(dependency).add(dependent)));
        prepared.add(new Pair<>(null, null));
        running=1;
        SleepProcess.create(continuation, context.executor(), this::process)
                .run();
    }

    public static void prepareImages(
            Context context, List<Image> images, Continuation<Void> continuation) throws Throwable {
        new PrepareImages(context)
                .prepare(images, continuation);
    }

    private void process(Block wakeup, Block sleep, Continuation<Void> continuation) throws Throwable {
        context.checkStopped();
        synchronized (lock) {
            prepared2.addAll(prepared);
            prepared.clear();
        }
        running-=prepared2.size();
        for (int ii=prepared2.size(); 0<ii; --ii) {
            Pair<Image, Throwable> pair=prepared2.removeFirst();
            prepared2.addLast(pair);
            if (null!=pair.second) {
                if (null==throwable) {
                    throwable=pair.second;
                }
                else {
                    throwable.addSuppressed(pair.second);
                }
            }
        }
        if (null!=throwable) {
            prepared2.clear();
            if (0>=running) {
                continuation.failed(throwable);
            }
            else {
                sleep.run();
            }
            return;
        }
        while (!prepared2.isEmpty()) {
            Image dependency=prepared2.removeFirst().first;
            for (Image dependent: dependents.get(dependency)) {
                Set<Image> dependencies2=dependencies.get(dependent);
                dependencies2.remove(dependency);
                if (dependencies2.isEmpty()) {
                    ++running;
                    Continuation<Void> continuation2
                            =Continuations.singleRun(new PrepareContinuation(dependent, wakeup));
                    try {
                        dependent.prepare(context, continuation2);
                    }
                    catch (Throwable throwable) {
                        continuation2.failed(throwable);
                    }
                }
            }
        }
        if (0>=running) {
            continuation.completed(null);
        }
        else {
            sleep.run();
        }
    }
}
