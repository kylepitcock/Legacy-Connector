package legacyconnector.v1_6_4.facade.net.minecraft.nbt;

/** Facade of the 1.7.10 shim's {@code NBTBase} for 1.4.7 to 1.6.4 mods. See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class NBTBase {
    private NBTBase() {}

    private static final String[] NAMES = {"TAG_End", "TAG_Byte", "TAG_Short", "TAG_Int", "TAG_Long", "TAG_Float", "TAG_Double", "TAG_Byte_Array",
            "TAG_String", "TAG_List", "TAG_Compound", "TAG_Int_Array"};

    /** {@code getTagName(type)}: the name of a tag type id as the legacy NBT dumps printed it. */
    public static String getTagName(byte type) {
        return type >= 0 && type < NAMES.length ? NAMES[type] : "UNKNOWN";
    }
}
