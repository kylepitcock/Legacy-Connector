package legacyconnector.v1_12_2.net.minecraftforge.common.util;

import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;

/** Forge's JSON helpers for recipe and loot files. */
public class JsonUtils {
    /** An NBT compound written as a string in the JSON ({@code "nbt": "{Damage:2}"}). */
    public static NBTTagCompound readNBT(com.google.gson.JsonObject json, String key) {
        String text = legacyconnector.v1_12_2.net.minecraft.util.JsonUtils.getString(json, key);
        try {
            return legacyconnector.v1_12_2.net.minecraft.nbt.JsonToNBT.getTagFromJson(text);
        } catch (legacyconnector.v1_12_2.net.minecraft.nbt.NBTException e) {
            throw new com.google.gson.JsonSyntaxException("Malformed NBT tag", e);
        }
    }
}
