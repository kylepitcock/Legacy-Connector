package legacyconnector.v1_7_10.baubles.api.expanded;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_7_10.baubles.api.BaubleType;

/**
 * Baubles Expanded's slot registry. The Curios-backed Baubles view has a fixed layout (0 amulet, 1-2 ring, 3 belt, 4 head, 5 body,
 * 6 charm, 7 trinket), so every standard type is always registered and assigned; a mod's own type name cannot become a Curios slot
 * after the data pack is built, so registering one reports false (what a full registry reports) and it has no slot.
 */
public final class BaubleExpandedSlots {
    public static final String amuletType = "amulet";
    public static final String ringType = "ring";
    public static final String beltType = "belt";
    public static final String trinketType = "trinket";
    public static final String headType = "head";
    public static final String bodyType = "body";
    public static final String charmType = "charm";
    public static final String universalType = "universal";
    public static final String invalidType = "invalid";

    /** Type name of each slot of the Baubles view. */
    private static final String[] SLOT_TYPES = {amuletType, ringType, ringType, beltType, headType, bodyType, charmType, trinketType};

    public static final int slotLimit = SLOT_TYPES.length;

    private BaubleExpandedSlots() {}

    public static boolean isInvalidType(String type) {
        return type == null || !isKnown(type);
    }

    private static boolean isKnown(String type) {
        if (universalType.equals(type)) return true;
        for (String t : SLOT_TYPES) if (t.equals(type)) return true;
        return false;
    }

    public static boolean tryRegisterType(String type) {
        return isKnown(type);
    }

    public static boolean tryAssignSlotOfType(String type) {
        return isKnown(type);
    }

    public static boolean tryAssignSlotsUpToMinimum(String type, int minimum) {
        return isKnown(type) && getIndexesOfAssignedSlotsOfType(type).length >= minimum;
    }

    public static boolean tryUnassignSlotOfType(String type) {
        return false;
    }

    /** Slots of the Baubles view that take {@code type}; all of them for the universal type. */
    public static int[] getIndexesOfAssignedSlotsOfType(String type) {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < SLOT_TYPES.length; i++) {
            if (universalType.equals(type) || SLOT_TYPES[i].equals(type)) out.add(i);
        }
        return out.stream().mapToInt(Integer::intValue).toArray();
    }

    public static String getSlotType(int slot) {
        return slot >= 0 && slot < SLOT_TYPES.length ? SLOT_TYPES[slot] : invalidType;
    }

    public static int getSlotsCurrentlyUsed() {
        return SLOT_TYPES.length;
    }

    public static List<String> getCurrentlyRegisteredTypes() {
        List<String> out = new ArrayList<>();
        for (String t : SLOT_TYPES) if (!out.contains(t)) out.add(t);
        return out;
    }

    public static boolean isTypeRegistered(String type) {
        return isKnown(type) && !universalType.equals(type);
    }

    /** Baubles Expanded's name for a legacy {@link BaubleType}. */
    public static String typeName(BaubleType type) {
        return type == null ? invalidType : type.getTypeName();
    }
}
