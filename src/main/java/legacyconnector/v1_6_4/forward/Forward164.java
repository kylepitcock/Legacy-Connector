package legacyconnector.v1_6_4.forward;

import legacyconnector.v1_7_10.net.minecraft.nbt.NBTBase;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagList;

/**
 * Static helpers the 1.6.4 loader (loader/.../ForwardCalls, rules in forward/1.6.4/member-manual.tsv) calls for 1.6.4 API that has no
 * one-to-one 1.7.10 member: the receiver becomes the first argument. They live in the 1.6.4 shim namespace because the class does not
 * exist in 1.7.10.
 */
public final class Forward164 {
    private Forward164() {}

    /**
     * 1.6.4 {@code NBTTagCompound.getTagList(name)}: the list stored under the key whatever its element type (1.7.10 asks for the
     * type and returns an empty list on a mismatch). A missing key gives an empty list, as in 1.6.4.
     */
    public static NBTTagList getTagList(NBTTagCompound tag, String key) {
        NBTBase value = tag.getTag(key);
        return value instanceof NBTTagList list ? list : new NBTTagList();
    }

    /** 1.6.4 {@code NBTTagList.tagAt(i)}: the element of any type (1.7.10 only has typed getters). */
    public static NBTBase tagAt(NBTTagList list, int index) {
        if (index < 0 || index >= list.tagCount()) return null;
        return NBTBase.mcc$wrap(list.mcc$modern().get(index), list);
    }

    /** 1.6.4 {@code FMLPreInitializationEvent.getModLog()}: a {@code java.util.logging.Logger} (1.7.10 returns a log4j one). */
    public static java.util.logging.Logger getModLog(legacyconnector.v1_7_10.cpw.mods.fml.common.event.FMLPreInitializationEvent event) {
        String id = event.getModMetadata() != null ? event.getModMetadata().modId : null;
        return java.util.logging.Logger.getLogger(id != null ? id : "ForgeModLoader");
    }
}
