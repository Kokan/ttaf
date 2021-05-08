package dog.giraffe.image.transform;

import java.util.List;

/**
 * Describes masks that can be applied to clustering algorithms to disregard data points.
 */
public interface Mask {
    /**
     * Creates a mask that allows all points to be clustered.
     */
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

    /**
     * Creates the intersection of all the masks. Any point masked out by any mask will be masked out in the
     * new mask too.
     */
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

    /**
     * Creates a new mask that enables point (xx,yy) when it is in the solution
     * of cx*xx+cy*yy+cc &gt;= 0.0
     */
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

    /**
     * Creates a new mask the enables points which are on the left of the half-plane
     * specified be the (x1,y1)-&gt;(x2,y2) vector.
     */
    static Mask halfPlane(double x1, double y1, double x2, double y2) {
        double dx=x2-x1;
        double dy=y2-y1;
        return halfPlane(dy, -dx, y1*dx-x1*dy);
    }

    /**
     * Return whether the point (xx, yy) visible through this mask.
     */
    boolean visible(double xx, double yy);
}
