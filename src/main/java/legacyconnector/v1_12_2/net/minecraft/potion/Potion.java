package legacyconnector.v1_12_2.net.minecraft.potion;

import legacyconnector.v1_12_2.net.minecraftforge.registries.IForgeRegistryEntry;

public class Potion extends IForgeRegistryEntry.Impl<Potion> {
    public static final legacyconnector.v1_12_2.net.minecraft.util.registry.RegistryNamespaced<legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation, Potion> REGISTRY = org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.view(org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.POTIONS, "speed");
    private final boolean isBadEffect;

    private static void mcc$vanilla() {
        try {
            Class.forName("legacyconnector.v1_12_2.net.minecraft.init.MobEffects", true, Potion.class.getClassLoader());
        } catch (ClassNotFoundException | LinkageError e) {
            // only mod potions then
        }
    }

    public static Potion getPotionFromResourceLocation(String location) {
        mcc$vanilla();
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.POTIONS.getValue(new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation(location));
    }

    public static Potion getPotionById(int id) {
        mcc$vanilla();
        return REGISTRY.getObjectById(id);
    }

    public static int getIdFromPotion(Potion potion) {
        mcc$vanilla();
        return REGISTRY.getIDForObject(potion);
    }
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
