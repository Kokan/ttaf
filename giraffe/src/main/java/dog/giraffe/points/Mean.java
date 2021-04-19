package dog.giraffe.points;

import dog.giraffe.EmptySetException;
import dog.giraffe.Sum;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Mean {
    public static class Factory {
        private final int dimensions;

        public Factory(int dimensions) {
            this.dimensions=dimensions;
        }

        public Mean create(int expectedAddends, Sum.Factory sumFactory) {
            List<Sum> sums=new ArrayList<>(dimensions);
            for (int dd=dimensions; 0<dd; --dd) {
                sums.add(sumFactory.create(expectedAddends));
            }
            return new Mean(Collections.unmodifiableList(sums));
        }
    }

    int addends;
    final List<Sum> sums;

    private Mean(List<Sum> sums) {
        this.sums=sums;
    }

    public void add(Vector addend) {
        ++addends;
        for (int dd=0; sums.size()>dd; ++dd) {
            sums.get(dd).add(addend.coordinate(dd));
        }
    }

    public void addAll(int addends, Vector sum) {
        this.addends+=addends;
        for (int dd=0; sums.size()>dd; ++dd) {
            sums.get(dd).add(sum.coordinate(dd));
        }
    }

    public void addTo(Mean mean) {
        mean.addends+=addends;
        for (int dd=0; sums.size()>dd; ++dd) {
            sums.get(dd).addTo(mean.sums.get(dd));
        }
    }

    public void clear() {
        addends=0;
        for (Sum sum: sums) {
            sum.clear();
        }
    }

    public Vector mean() {
        if (0>=addends) {
            throw new EmptySetException();
        }
        Vector mean=new Vector(sums.size());
        for (int dd=0; sums.size()>dd; ++dd) {
            mean.coordinate(dd, sums.get(dd).sum()/addends);
        }
        return mean;
    }
}
