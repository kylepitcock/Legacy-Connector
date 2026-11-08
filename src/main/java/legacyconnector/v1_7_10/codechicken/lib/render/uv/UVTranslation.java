package legacyconnector.v1_7_10.codechicken.lib.render.uv;

public class UVTranslation extends UVTransformation {
    public double du;
    public double dv;

    public UVTranslation(double u, double v) {
        du = u;
        dv = v;
    }

    public void apply(UV uv) {
        uv.u += du;
        uv.v += dv;
    }

    public UVTransformation at(UV point) {
        return this;
    }

    public UVTransformation inverse() {
        return new UVTranslation(-du, -dv);
    }

    public UVTransformation merge(UVTransformation next) {
        return next instanceof UVTranslation t ? new UVTranslation(du + t.du, dv + t.dv) : null;
    }

    public boolean isRedundant() {
        return du == 0 && dv == 0;
    }
}
