package legacyconnector.v1_6_4.net.minecraft.src.forge;

import legacyconnector.v1_7_10.net.minecraft.item.EnumRarity;

/** Forge 3.x {@code forge.EnumHelperClient} (a mod's {@code addRarity} call): the same helper later versions kept in {@code net.minecraftforge.client}. */
public final class EnumHelperClient {
    private EnumHelperClient() {}

    /** Adds an item rarity; {@code color} is the chat colour index 0-15 (black ... white) of its name. */
    public static EnumRarity addRarity(String name, int color, String displayName) {
        return legacyconnector.v1_6_4.net.minecraftforge.client.EnumHelperClient.addRarity(name, color, displayName);
    }
}
