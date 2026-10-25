package legacyconnector.v1_7_10.net.minecraft.client.renderer;

import legacyconnector.v1_7_10.net.minecraft.util.IIcon;

/** An icon read mirrored horizontally and/or vertically (doors, beds, pistons use it). */
public class IconFlipped implements IIcon {
    private final IIcon icon;
    private final boolean flipU;
    private final boolean flipV;

    public IconFlipped(IIcon icon, boolean flipU, boolean flipV) {
        this.icon = icon;
        this.flipU = flipU;
        this.flipV = flipV;
    }

    @Override
    public int getIconWidth() {
        return icon.getIconWidth();
    }

    @Override
    public int getIconHeight() {
        return icon.getIconHeight();
    }

    @Override
    public float getMinU() {
        return flipU ? icon.getMaxU() : icon.getMinU();
    }

    @Override
    public float getMaxU() {
        return flipU ? icon.getMinU() : icon.getMaxU();
    }

    @Override
    public float getInterpolatedU(double u) {
        float span = getMaxU() - getMinU();
        return getMinU() + span * ((float) u / 16.0F);
    }

    @Override
    public float getMinV() {
        return flipV ? icon.getMaxV() : icon.getMinV();
    }

    @Override
    public float getMaxV() {
        return flipV ? icon.getMinV() : icon.getMaxV();
    }

    @Override
    public float getInterpolatedV(double v) {
        float span = getMaxV() - getMinV();
        return getMinV() + span * ((float) v / 16.0F);
    }

    @Override
    public String getIconName() {
        return icon.getIconName();
    }
}
