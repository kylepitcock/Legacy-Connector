package legacyconnector.v1_12_2.net.minecraftforge.common;

import legacyconnector.v1_12_2.net.minecraft.util.text.TextFormatting;

/** An item rarity: the colour of the item's name (vanilla's EnumRarity, or a mod's own). */
public interface IRarity {
    TextFormatting getColor();

    String getName();
}
