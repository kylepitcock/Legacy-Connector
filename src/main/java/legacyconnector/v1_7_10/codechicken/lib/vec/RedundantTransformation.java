package legacyconnector.v1_7_10.codechicken.lib.vec;

/** The identity. */
public class RedundantTransformation extends Transformation {
    public void apply(Vector3 vec) {
    }

    public void applyN(Vector3 normal) {
    }

    public void apply(Matrix4 mat) {
    }

    public Transformation at(Vector3 point) {
        return this;
    }

    public Transformation with(Transformation t) {
        return t;
    }

    public Transformation inverse() {
        return this;
    }

    public Transformation merge(Transformation next) {
        return next;
    }

    public boolean isRedundant() {
        return true;
    }

    public void glApply() {
    }

    public String toString() {
        return "Nothing()";
    }
}
