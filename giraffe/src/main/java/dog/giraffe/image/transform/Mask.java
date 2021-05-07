package dog.giraffe.image.transform;

import java.util.List;

public interface Mask {
    static Mask all() {
        return new Mask() {
            @Override
            public String toString() {
                return "all";
            }

            @Override
            public boolean visible(double xx, double yy) {
                return true;
            }
        };
    }

    static Mask and(List<Mask> masks) {
        if (masks.isEmpty()) {
            return Mask.all();
        }
        if (1==masks.size()) {
            return masks.get(0);
        }
        return new Mask() {
            @Override
            public String toString() {
                return "and("+masks+")";
            }

            @Override
            public boolean visible(double xx, double yy) {
                for (Mask mask: masks) {
                    if (!mask.visible(xx, yy)) {
                        return false;
                    }
                }
                return true;
            }
        };
    }

    static Mask halfPlane(double cx, double cy, double cc) {
        return new Mask() {
            @Override
            public String toString() {
                return "halfPlane(cx: "+cx+", cy: "+cy+", cc: "+cc+")";
            }

            @Override
            public boolean visible(double xx, double yy) {
                return cx*xx+cy*yy+cc>=0.0;
            }
        };
    }

    static Mask halfPlane(double x1, double y1, double x2, double y2) {
        double dx=x2-x1;
        double dy=y2-y1;
        return halfPlane(dy, -dx, y1*dx-x1*dy);
    }

    boolean visible(double xx, double yy);
}
