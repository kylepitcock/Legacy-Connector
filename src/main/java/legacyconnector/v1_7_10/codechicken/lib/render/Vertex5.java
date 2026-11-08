package legacyconnector.v1_7_10.codechicken.lib.render;

import legacyconnector.v1_7_10.codechicken.lib.render.uv.UV;
import legacyconnector.v1_7_10.codechicken.lib.render.uv.UVTransformation;
import legacyconnector.v1_7_10.codechicken.lib.vec.Transformation;
import legacyconnector.v1_7_10.codechicken.lib.vec.Vector3;

/** A vertex: position and texture coordinate. */
public class Vertex5 {
    public Vector3 vec;
    public UV uv;

    public Vertex5() {
        this(new Vector3(), new UV());
    }

    public Vertex5(Vector3 vec, UV uv) {
        this.vec = vec;
        this.uv = uv;
    }

    public Vertex5(Vector3 vec, double u, double v) {
        this(vec, new UV(u, v));
    }

    public Vertex5(Vector3 vec, double u, double v, int tex) {
        this(vec, new UV(u, v, tex));
    }

    public Vertex5(double x, double y, double z, double u, double v) {
        this(new Vector3(x, y, z), new UV(u, v));
    }

    public Vertex5(double x, double y, double z, double u, double v, int tex) {
        this(new Vector3(x, y, z), new UV(u, v, tex));
    }

    public Vertex5(Vertex5 v) {
        this(v.vec.copy(), v.uv.copy());
    }

    public Vertex5 set(double x, double y, double z, double u, double v) {
        vec.set(x, y, z);
        uv.set(u, v, uv.tex);
        return this;
    }

    public Vertex5 set(double x, double y, double z, double u, double v, int tex) {
        vec.set(x, y, z);
        uv.set(u, v, tex);
        return this;
    }

    public Vertex5 set(Vertex5 v) {
        vec.set(v.vec);
        uv.set(v.uv);
        return this;
    }

    public Vertex5 copy() {
        return new Vertex5(this);
    }

    public Vertex5 apply(Transformation t) {
        vec.apply(t);
        return this;
    }

    public Vertex5 apply(UVTransformation t) {
        uv.apply(t);
        return this;
    }

    public String toString() {
        return "Vertex5(" + vec + ", " + uv + ")";
    }
}
