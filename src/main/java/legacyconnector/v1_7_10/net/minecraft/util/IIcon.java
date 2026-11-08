package legacyconnector.v1_7_10.net.minecraft.util;

public interface IIcon {
    int getIconWidth();

    int getIconHeight();

    float getMinU();

    float getMaxU();

    float getInterpolatedU(double u);

    float getMinV();

    float getMaxV();

    float getInterpolatedV(double v);

    String getIconName();

    /** 1.5.2/1.6.4 {@code Icon.getSheetWidth()}: the atlas width in pixels, from the icon's size and the part of the sheet its UVs span. */
    default int getSheetWidth() {
        float span = getMaxU() - getMinU();
        return span > 0 ? Math.round(getIconWidth() / span) : getIconWidth();
    }

    default int getSheetHeight() {
        float span = getMaxV() - getMinV();
        return span > 0 ? Math.round(getIconHeight() / span) : getIconHeight();
    }

    /** 1.5.2/1.6.4 {@code Icon.getOriginX()}: the icon's left edge on its sheet, in pixels. */
    default int getOriginX() {
        return Math.round(getMinU() * getSheetWidth());
    }

    default int getOriginY() {
        return Math.round(getMinV() * getSheetHeight());
    }

    /** The modern sprite id the shim draws this icon with (what {@link #getIconName} returns unless an icon reports a legacy name). */
    default String mcc$modernName() {
        return getIconName();
    }
}
