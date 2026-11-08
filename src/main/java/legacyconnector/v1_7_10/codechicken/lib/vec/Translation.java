package legacyconnector.v1_7_10.codechicken.lib.vec;

import legacyconnector.v1_7_10.org.lwjgl.opengl.GL11;

public class Translation extends Transformation {
    public Vector3 vec;

    public Translation(Vector3 vec) {
        this.vec = vec;
    }

    public Translation(double x, double y, double z) {
        this(new Vector3(x, y, z));
    }

    public void apply(Vector3 v) {
        v.add(vec);
    }

    public void applyN(Vector3 normal) {
    }

    public void apply(Matrix4 mat) {
        mat.translate(vec);
    }

    public Transformation at(Vector3 point) {
        return this;
    }

    public Transformation inverse() {
        return new Translation(-vec.x, -vec.y, -vec.z);
    }

    public Transformation merge(Transformation next) {
        return next instanceof Translation t ? new Translation(vec.copy().add(t.vec)) : null;
    }

    public boolean isRedundant() {
        return vec.isZero();
    }

    public void glApply() {
        GL11.glTranslated(vec.x, vec.y, vec.z);
    }

    public String toString() {
        return "Translation(" + vec + ")";
    }
}
