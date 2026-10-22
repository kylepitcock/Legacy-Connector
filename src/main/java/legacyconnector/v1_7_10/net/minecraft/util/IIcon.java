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
}
