package legacyconnector.v1_7_10.codechicken.lib.vec;

import legacyconnector.v1_7_10.org.lwjgl.opengl.GL11;

public class Scale extends Transformation {
    public Vector3 factor;

    public Scale(Vector3 factor) {
        this.factor = factor;
    }

    public Scale(double factor) {
        this(new Vector3(factor, factor, factor));
    }

    public Scale(double x, double y, double z) {
        this(new Vector3(x, y, z));
    }

    public void apply(Vector3 v) {
        v.multiply(factor);
    }

    public void applyN(Vector3 normal) {
    }

    public void apply(Matrix4 mat) {
        mat.scale(factor);
    }

    public Transformation inverse() {
        return new Scale(1 / factor.x, 1 / factor.y, 1 / factor.z);
    }

    public Transformation merge(Transformation next) {
        return next instanceof Scale s ? new Scale(factor.copy().multiply(s.factor)) : null;
    }

    public boolean isRedundant() {
        return factor.x == 1 && factor.y == 1 && factor.z == 1;
    }

    public void glApply() {
        GL11.glScaled(factor.x, factor.y, factor.z);
    }

    public String toString() {
        return "Scale(" + factor + ")";
    }
}
