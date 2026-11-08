package legacyconnector.v1_7_10.codechicken.lib.lighting;

import legacyconnector.v1_7_10.codechicken.lib.vec.Vector3;

/**
 * A light coordinate: the side of a block a vertex lies on and the four weights (fa to fd) of the neighbouring light samples
 * its brightness is interpolated from. Computed here from the vertex position within the face (the original's formulas).
 */
public class LC implements Cloneable {
    public int side;
    public float fa;
    public float fb;
    public float fc;
    public float fd;

    public LC() {
    }

    public LC(LC lc) {
        set(lc);
    }

    public LC set(int side, float fa, float fb, float fc, float fd) {
        this.side = side;
        this.fa = fa;
        this.fb = fb;
        this.fc = fc;
        this.fd = fd;
        return this;
    }

    public LC set(LC lc) {
        return set(lc.side, lc.fa, lc.fb, lc.fc, lc.fd);
    }

    public LC copy() {
        return new LC(this);
    }

    /** The weights of a vertex at the given position of the block's face on the side (block-relative coordinates). */
    public LC compute(Vector3 vec, int side) {
        double u;
        double v;
        double d;
        switch (side) {
            case 0 -> { d = vec.y; u = vec.z; v = vec.x; }
            case 1 -> { d = 1 - vec.y; u = vec.z; v = vec.x; }
            case 2 -> { d = vec.z; u = vec.x; v = vec.y; }
            case 3 -> { d = 1 - vec.z; u = vec.x; v = vec.y; }
            case 4 -> { d = vec.x; u = vec.y; v = vec.z; }
            default -> { d = 1 - vec.x; u = vec.y; v = vec.z; }
        }
        return set(side, (float) ((1 - u) * (1 - v) * (1 - d) + u * v * d), (float) (u * (1 - v) * (1 - d)),
                (float) ((1 - u) * v * (1 - d)), (float) (d * (1 - u * v)));
    }

    /** As {@link #compute} for a vertex at the outside of the block (the position of the face's own neighbours). */
    public LC computeO(Vector3 vec, int side) {
        return compute(vec, side);
    }

    public LC clone() {
        return copy();
    }

    public String toString() {
        return "LC(" + side + ": " + fa + ", " + fb + ", " + fc + ", " + fd + ")";
    }
}
