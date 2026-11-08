package legacyconnector.v1_7_10.codechicken.lib.render.uv;

import legacyconnector.v1_7_10.net.minecraft.util.IIcon;

/** As {@link IconTransformation}, with one icon per texture index (a block's six sides). */
public class MultiIconTransformation extends UVTransformation {
    public IIcon[] icons;

    public MultiIconTransformation(IIcon... icons) {
        this.icons = icons;
    }

    public void apply(UV uv) {
        IIcon icon = icons[uv.tex % icons.length];
        uv.u = icon.getInterpolatedU(uv.u * 16);
        uv.v = icon.getInterpolatedV(uv.v * 16);
    }

    public UVTransformation at(UV point) {
        return this;
    }

    public UVTransformation inverse() {
        throw new UnsupportedOperationException("MultiIconTransformation has no inverse");
    }

    public boolean isRedundant() {
        return false;
    }
}
