package dog.giraffe;

import dog.giraffe.threads.Consumer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class QuickSortTest {
    private static void permutations(Consumer<List<Integer>> consumer, int size) throws Throwable {
        List<Integer> list=new ArrayList<>(size);
        while (list.size()<size) {
            list.add(null);
        }
        permutations(consumer, list, Collections.unmodifiableList(list), size, 0);
    }

    private static void permutations(
            Consumer<List<Integer>> consumer, List<Integer> list, List<Integer> list2, int size, int value)
            throws Throwable {
        if (size<=value) {
            consumer.accept(list2);
            return;
        }
        for (int ii=0; size>ii; ++ii) {
            if (null==list.get(ii)) {
                list.set(ii, value);
                try {
                    permutations(consumer, list, list2, size, value+1);
                }
                finally {
                    list.set(ii, null);
                }
            }
        }
    }

    @Test
    public void testMedianSplit() throws Throwable {
        for (int ss=0; 8>ss; ++ss) {
            permutations(
                    (permutation)->{
                        for (int divisor: new int[]{1, 2, 3, 4, 8}) {
                            List<Integer> list=new ArrayList<>(permutation);
                            for (int ii=0; list.size()>ii; ++ii) {
                                list.set(ii, list.get(ii)/divisor);
                            }
                            testMedianSplit(list);
                        }
                    },
                    ss);
        }
    }

    private void testMedianSplit(List<Integer> list) {
        int size=list.size();
        List<Integer> copy=new ArrayList<>(list);
        AtomicInteger swaps=new AtomicInteger(0);
        int split=QuickSort.medianSplit(
                (index0, index1)->Integer.compare(list.get(index0), list.get(index1)),
                0,
                (index0, index1)->{
                    list.set(index0, list.set(index1, list.get(index0)));
                    swaps.incrementAndGet();
                },
                size);
        assertEquals(size/2, Math.min(split, size-split));
        int max=Integer.MIN_VALUE;
        for (int ii=0; split>ii; ++ii) {
            max=Math.max(max, list.get(ii));
        }
        int min=Integer.MAX_VALUE;
        for (int ii=split; size>ii; ++ii) {
            min=Math.min(min, list.get(ii));
        }
        assertTrue(max<=min);
        copy.sort(null);
        list.sort(null);
        assertEquals(copy, list);
    }
}
