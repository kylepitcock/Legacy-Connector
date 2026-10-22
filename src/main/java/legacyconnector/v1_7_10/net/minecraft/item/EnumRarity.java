package legacyconnector.v1_7_10.net.minecraft.item;

import legacyconnector.v1_7_10.net.minecraft.util.EnumChatFormatting;

public enum EnumRarity {
    common(EnumChatFormatting.WHITE, "Common"),
    uncommon(EnumChatFormatting.YELLOW, "Uncommon"),
    rare(EnumChatFormatting.AQUA, "Rare"),
    epic(EnumChatFormatting.LIGHT_PURPLE, "Epic");

    public final EnumChatFormatting rarityColor;
    public final String rarityName;

    EnumRarity(EnumChatFormatting color, String name) {
        this.rarityColor = color;
        this.rarityName = name;
    }

    public net.minecraft.world.item.Rarity mcc$modern() {
        return switch (this) {
            case common -> net.minecraft.world.item.Rarity.COMMON;
            case uncommon -> net.minecraft.world.item.Rarity.UNCOMMON;
            case rare -> net.minecraft.world.item.Rarity.RARE;
            case epic -> net.minecraft.world.item.Rarity.EPIC;
        };
    }
}
