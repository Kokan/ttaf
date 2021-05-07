package dog.giraffe.points;

import dog.giraffe.Doubles;
import dog.giraffe.Sum;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Variance {
    public static class Factory {
        private final int dimensions;

        public Factory(int dimensions) {
            this.dimensions = dimensions;
        }

        public Variance create(int expectedAddends, Vector mean, Sum.Factory sumFactory) {
            List<Sum> sums=new ArrayList<>(dimensions);
            for (int ii=0; ii<dimensions; ++ii) {
                sums.add(sumFactory.create(expectedAddends));
            }
            return new Variance(mean, Collections.unmodifiableList(sums));
        }
    }

    private int addends;
    private final Vector mean;
    private final List<Sum> sums;

    public Variance(Vector mean, List<Sum> sums) {
        this.mean=mean;
        this.sums=sums;
    }

    public void add(Vector addend) {
        ++addends;
        for (int ii=0;ii<addend.dimensions(); ++ii) {
            sums.get(ii).add(Doubles.square(addend.coordinate(ii)-mean.coordinate(ii)));
        }
    }

    public Vector variance() {
        if (addends==0) {
            throw new RuntimeException("division by zero");
        }
        Vector dev=new Vector(sums.size());
        for (int ii=0;ii<sums.size(); ++ii) {
            dev.coordinate(ii, Math.sqrt(sums.get(ii).sum()/addends));
        }
        return dev;
    }
}
