package legacyconnector.v1_12_2.net.minecraft.inventory;

public enum EntityEquipmentSlot {
    MAINHAND(Type.HAND, 0, 0, "mainhand"),
    OFFHAND(Type.HAND, 1, 5, "offhand"),
    FEET(Type.ARMOR, 0, 1, "feet"),
    LEGS(Type.ARMOR, 1, 2, "legs"),
    CHEST(Type.ARMOR, 2, 3, "chest"),
    HEAD(Type.ARMOR, 3, 4, "head");

    private final Type slotType;
    private final int index;
    private final int slotIndex;
    private final String name;

    EntityEquipmentSlot(Type slotType, int index, int slotIndex, String name) {
        this.slotType = slotType;
        this.index = index;
        this.slotIndex = slotIndex;
        this.name = name;
    }

    public Type getSlotType() {
        return slotType;
    }

    public int getIndex() {
        return index;
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    public String getName() {
        return name;
    }

    public static EntityEquipmentSlot fromString(String targetName) {
        for (EntityEquipmentSlot s : values()) if (s.name.equals(targetName)) return s;
        throw new IllegalArgumentException("Invalid slot '" + targetName + "'");
    }

    public enum Type {
        HAND, ARMOR
    }
}
