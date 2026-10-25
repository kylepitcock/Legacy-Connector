package legacyconnector.v1_12_2.net.minecraft.nbt;

/** Parses the textual NBT of commands and item definitions. */
public class JsonToNBT {
    public static NBTTagCompound getTagFromJson(String text) throws NBTException {
        try {
            return new NBTTagCompound(net.minecraft.nbt.TagParser.parseTag(text));
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            throw new NBTException(e.getMessage());
        }
    }
}
