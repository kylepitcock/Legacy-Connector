package legacyconnector.v1_7_10.codechicken.lib.vec;

import java.util.ArrayList;
import java.util.List;

/** Several transformations applied one after the other. */
public class TransformationList extends Transformation {
    private final ArrayList<Transformation> transformations = new ArrayList<>();
    private Matrix4 mat;

    public TransformationList(Transformation... transforms) {
        ArrayList<Transformation> all = new ArrayList<>();
        for (Transformation t : transforms) {
            if (t instanceof TransformationList l) all.addAll(l.transformations);
            else all.add(t);
        }
        for (Transformation t : all) {
            if (t.isRedundant()) continue;
            if (!transformations.isEmpty()) {
                Transformation merged = transformations.get(transformations.size() - 1).merge(t);
                if (merged != null) {
                    transformations.set(transformations.size() - 1, merged);
                    if (merged.isRedundant()) transformations.remove(transformations.size() - 1);
                    continue;
                }
            }
            transformations.add(t);
        }
    }

    public Matrix4 compile() {
        if (mat == null) {
            Matrix4 m = new Matrix4();
            for (int i = transformations.size() - 1; i >= 0; i--) transformations.get(i).apply(m);
            mat = m;
        }
        return mat;
    }

    public List<Transformation> list() {
        return transformations;
    }

    public void apply(Vector3 vec) {
        for (Transformation t : transformations) t.apply(vec);
    }

    public void applyN(Vector3 normal) {
        for (Transformation t : transformations) t.applyN(normal);
    }

    /** The matrix form: the combined transformation multiplied in, so it acts on vectors in list order. */
    public void apply(Matrix4 m) {
        m.multiply(compile());
    }

    public Transformation with(Transformation t) {
        return t.isRedundant() ? this : new TransformationList(this, t);
    }

    public Transformation inverse() {
        Transformation[] inv = new Transformation[transformations.size()];
        for (int i = 0; i < inv.length; i++) inv[i] = transformations.get(inv.length - 1 - i).inverse();
        return new TransformationList(inv);
    }

    public boolean isRedundant() {
        return transformations.isEmpty();
    }

    public void glApply() {
        for (Transformation t : transformations) t.glApply();
    }

    public String toString() {
        return transformations.toString();
    }
}
