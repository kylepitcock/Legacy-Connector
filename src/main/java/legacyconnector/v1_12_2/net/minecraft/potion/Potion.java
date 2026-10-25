package legacyconnector.v1_12_2.net.minecraft.potion;

import legacyconnector.v1_12_2.net.minecraftforge.registries.IForgeRegistryEntry;

public class Potion extends IForgeRegistryEntry.Impl<Potion> {
    public static final legacyconnector.v1_12_2.net.minecraft.util.registry.RegistryNamespaced<legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation, Potion> REGISTRY = org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.view(org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.POTIONS, "speed");
    private final boolean isBadEffect;
    private final int liquidColor;
    private String name = "";

    protected Potion(boolean isBadEffect, int liquidColor) {
        this.isBadEffect = isBadEffect;
        this.liquidColor = liquidColor;
    }

    protected Potion setPotionName(String name) {
        this.name = name;
        return this;
    }

    public String getName() {
        return name;
    }

    public boolean isBadEffect() {
        return isBadEffect;
    }

    public int getLiquidColor() {
        return liquidColor;
    }

    public boolean isInstant() {
        return false;
    }
}
