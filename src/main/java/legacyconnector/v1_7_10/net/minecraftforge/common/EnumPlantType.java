package legacyconnector.v1_7_10.net.minecraftforge.common;

/** Where a plant can grow (Forge). */
public enum EnumPlantType {
    Plains, Desert, Beach, Cave, Water, Nether, Crop;

    public static EnumPlantType getPlantType(String name) {
        for (EnumPlantType t : values()) if (t.name().equalsIgnoreCase(name)) return t;
        return Plains;
    }
}
