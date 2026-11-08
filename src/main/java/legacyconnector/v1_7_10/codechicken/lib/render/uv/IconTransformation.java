package legacyconnector.v1_7_10.codechicken.lib.render.uv;

import legacyconnector.v1_7_10.net.minecraft.util.IIcon;

/** Maps 0 to 1 texture coordinates onto an icon of the texture atlas. */
public class IconTransformation extends UVTransformation {
    public IIcon icon;

    public IconTransformation(IIcon icon) {
        this.icon = icon;
    }

    public void apply(UV uv) {
        uv.u = icon.getInterpolatedU(uv.u * 16);
        uv.v = icon.getInterpolatedV(uv.v * 16);
    }

    public UVTransformation at(UV point) {
        return this;
    }

    public UVTransformation inverse() {
        throw new UnsupportedOperationException("IconTransformation has no inverse");
    }

    public boolean isRedundant() {
        return false;
    }
}
