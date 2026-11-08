package legacyconnector.v1_7_10.codechicken.lib.render.uv;

import legacyconnector.v1_7_10.codechicken.lib.vec.ITransformation;

/** A transformation of texture coordinates. */
public abstract class UVTransformation extends ITransformation<UV, UVTransformation> {
    public UVTransformation at(UV point) {
        return new UVTransformationList(new UVTranslation(-point.u, -point.v), this, new UVTranslation(point.u, point.v));
    }

    public UVTransformation with(UVTransformation t) {
        return new UVTransformationList(this, t);
    }

    public UVTransformation merge(UVTransformation next) {
        return null;
    }
}
