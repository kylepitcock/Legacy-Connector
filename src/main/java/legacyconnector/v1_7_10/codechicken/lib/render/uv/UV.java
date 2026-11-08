package legacyconnector.v1_7_10.codechicken.lib.render.uv;

/** A texture coordinate; {@code tex} selects the texture of a multi-texture transformation (for blocks, the side). */
public class UV {
    public double u;
    public double v;
    public int tex;

    public UV() {
    }

    public UV(double u, double v) {
        this(u, v, 0);
    }

    public UV(double u, double v, int tex) {
        this.u = u;
        this.v = v;
        this.tex = tex;
    }

    public UV(UV uv) {
        this(uv.u, uv.v, uv.tex);
    }

    public UV set(double u, double v, int tex) {
        this.u = u;
        this.v = v;
        this.tex = tex;
        return this;
    }

    public UV set(UV uv) {
        return set(uv.u, uv.v, uv.tex);
    }

    public UV copy() {
        return new UV(this);
    }

    public UV apply(UVTransformation t) {
        t.apply(this);
        return this;
    }

    public String toString() {
        return "UV(" + u + ", " + v + ", " + tex + ")";
    }
}
