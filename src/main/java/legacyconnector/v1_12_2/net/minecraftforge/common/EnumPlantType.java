package legacyconnector.v1_12_2.net.minecraftforge.common;

/** Where a plant may grow (soil blocks ask the plant for its type); mods add types with {@link #getPlantType}. */
public enum EnumPlantType {
    Plains,
    Desert,
    Beach,
    Cave,
    Water,
    Nether,
    Crop;

    /** The type of that name (any case), added to the enum when there is none. */
    public static synchronized EnumPlantType getPlantType(String name) {
        for (EnumPlantType t : values()) if (t.name().equalsIgnoreCase(name)) return t;
        return org.kylepitcock.legacyconnector.core.LegacyEnums.add(EnumPlantType.class, name);
    }
}
