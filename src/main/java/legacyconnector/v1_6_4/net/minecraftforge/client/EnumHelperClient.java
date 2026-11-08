package legacyconnector.v1_6_4.net.minecraftforge.client;

import legacyconnector.v1_7_10.net.minecraft.item.EnumRarity;
import legacyconnector.v1_7_10.net.minecraft.util.EnumChatFormatting;
import legacyconnector.v1_7_10.net.minecraftforge.common.util.EnumHelper;

/** Forge 1.6.4 {@code net.minecraftforge.client.EnumHelperClient}: adds constants to client-side enums (1.7.10 merged it into EnumHelper). */
public final class EnumHelperClient {
    private EnumHelperClient() {}

    /** Adds an item rarity; {@code color} is the chat colour index 0-15 (black ... white) of its name. */
    public static EnumRarity addRarity(String name, int color, String displayName) {
        EnumChatFormatting[] all = EnumChatFormatting.values();
        EnumChatFormatting format = color >= 0 && color < 16 && color < all.length ? all[color] : EnumChatFormatting.WHITE;
        return EnumHelper.addRarity(name, format, displayName);
    }
}
