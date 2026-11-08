package legacyconnector.v1_7_10.codechicken.lib.render.uv;

public class UVScale extends UVTransformation {
    public double su;
    public double sv;

    public UVScale(double scale) {
        this(scale, scale);
    }

    public UVScale(double u, double v) {
        su = u;
        sv = v;
    }

    public void apply(UV uv) {
        uv.u *= su;
        uv.v *= sv;
    }

    public UVTransformation inverse() {
        return new UVScale(1 / su, 1 / sv);
    }

    public UVTransformation merge(UVTransformation next) {
        return next instanceof UVScale s ? new UVScale(su * s.su, sv * s.sv) : null;
    }

    public boolean isRedundant() {
        return su == 1 && sv == 1;
    }
}
