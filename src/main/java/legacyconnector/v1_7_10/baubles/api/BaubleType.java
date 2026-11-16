package legacyconnector.v1_7_10.baubles.api;

/**
 * Baubles slot kinds (1.0 and Baubles Expanded). With Curios they map to the "ring", "necklace", "belt", "curio", "head",
 * "body" and "charm" slots; {@link #UNIVERSAL} fits any of them. The first three keep their ordinals.
 */
public enum BaubleType {
    RING("ring", 1, 2),
    AMULET("amulet", 0),
    BELT("belt", 3),
    TRINKET("trinket", 7),
    HEAD("head", 4),
    BODY("body", 5),
    CHARM("charm", 6),
    UNIVERSAL("universal", 0, 1, 2, 3, 4, 5, 6, 7);

    private final String typeName;
    private final int[] validSlots;

    BaubleType(String typeName, int... validSlots) {
        this.typeName = typeName;
        this.validSlots = validSlots;
    }

    /** Inventory slots of the Baubles view this type may be worn in. */
    public int[] getValidSlots() {
        return validSlots.clone();
    }

    /** Baubles Expanded's string name of the slot type ("amulet", "ring", ...). */
    public String getTypeName() {
        return typeName;
    }

    /** True when an item of this type may sit in {@code slot} of the Baubles view. */
    public boolean hasSlot(int slot) {
        for (int s : validSlots) if (s == slot) return true;
        return false;
    }
}
