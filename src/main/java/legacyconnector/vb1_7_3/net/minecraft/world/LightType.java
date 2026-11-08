package legacyconnector.vb1_7_3.net.minecraft.world;

/** Beta 1.7.3 {@code EnumSkyBlock}: the two light channels. */
public enum LightType {
    SKY(15),
    BLOCK(0);

    public final int defaultValue;

    LightType(int defaultValue) { this.defaultValue = defaultValue; }
}
