package legacyconnector.v1_7_10.codechicken.lib.render.uv;

/** A rotation of texture coordinates around the origin, in radians. */
public class UVRotation extends UVTransformation {
    public double angle;

    public UVRotation(double angle) {
        this.angle = angle;
    }

    public void apply(UV uv) {
        double c = Math.cos(angle);
        double s = Math.sin(angle);
        double u = uv.u * c - uv.v * s;
        double v = uv.u * s + uv.v * c;
        uv.u = u;
        uv.v = v;
    }

    public UVTransformation inverse() {
        return new UVRotation(-angle);
    }

    public boolean isRedundant() {
        return angle == 0;
    }
}
