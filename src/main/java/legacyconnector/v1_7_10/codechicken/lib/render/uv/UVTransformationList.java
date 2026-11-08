package legacyconnector.v1_7_10.codechicken.lib.render.uv;

import java.util.ArrayList;

/** Several texture transformations one after the other. */
public class UVTransformationList extends UVTransformation {
    private final ArrayList<UVTransformation> list = new ArrayList<>();

    public UVTransformationList(UVTransformation... transforms) {
        for (UVTransformation t : transforms) {
            if (t instanceof UVTransformationList l) list.addAll(l.list);
            else if (!t.isRedundant()) list.add(t);
        }
    }

    public void apply(UV uv) {
        for (UVTransformation t : list) t.apply(uv);
    }

    public UVTransformation with(UVTransformation t) {
        return new UVTransformationList(this, t);
    }

    public UVTransformation inverse() {
        UVTransformation[] inv = new UVTransformation[list.size()];
        for (int i = 0; i < inv.length; i++) inv[i] = list.get(inv.length - 1 - i).inverse();
        return new UVTransformationList(inv);
    }

    public boolean isRedundant() {
        return list.isEmpty();
    }
}
