package legacyconnector.v1_7_10.net.minecraft.nbt;

/** Parses the textual NBT of commands and item definitions. */
public class JsonToNBT {
    public static NBTBase func_150315_a(String text) throws NBTException {
        try {
            return NBTBase.mcc$wrap(net.minecraft.nbt.TagParser.parseTag(text), null);
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            throw new NBTException(e.getMessage());
        }
    }
}
