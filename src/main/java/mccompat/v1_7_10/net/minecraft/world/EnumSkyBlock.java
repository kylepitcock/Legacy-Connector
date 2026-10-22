package mccompat.v1_7_10.net.minecraft.world;

public enum EnumSkyBlock {
    Sky(15), Block(0);

    public final int defaultLightValue;

    EnumSkyBlock(int defaultLightValue) {
        this.defaultLightValue = defaultLightValue;
    }
}
