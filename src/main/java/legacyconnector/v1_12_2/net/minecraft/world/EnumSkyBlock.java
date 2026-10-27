package legacyconnector.v1_12_2.net.minecraft.world;

/** The two light kinds and their default levels. */
public enum EnumSkyBlock {
    SKY(15),
    BLOCK(0);

    public final int defaultLightValue;

    EnumSkyBlock(int defaultLightValue) {
        this.defaultLightValue = defaultLightValue;
    }

    public net.minecraft.world.level.LightLayer mcc$modern() {
        return this == SKY ? net.minecraft.world.level.LightLayer.SKY : net.minecraft.world.level.LightLayer.BLOCK;
    }
}
