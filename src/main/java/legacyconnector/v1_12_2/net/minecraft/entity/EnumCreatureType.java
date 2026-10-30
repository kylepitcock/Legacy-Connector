package legacyconnector.v1_12_2.net.minecraft.entity;

public enum EnumCreatureType {
    MONSTER(70, false, false),
    CREATURE(10, true, true),
    AMBIENT(15, true, false),
    WATER_CREATURE(5, true, false);

    private final int maxNumberOfCreature;
    private final boolean isPeaceful;
    private final boolean isAnimal;

    EnumCreatureType(int max, boolean peaceful, boolean animal) {
        this.maxNumberOfCreature = max;
        this.isPeaceful = peaceful;
        this.isAnimal = animal;
    }

    public int getMaxNumberOfCreature() {
        return maxNumberOfCreature;
    }

    public boolean getPeacefulCreature() {
        return isPeaceful;
    }

    public boolean getAnimal() {
        return isAnimal;
    }

    /** The creature class of the types {@code EnumHelper.addCreatureType} added. */
    private static final java.util.Map<EnumCreatureType, Class<?>> mcc$ADDED_CLASSES = new java.util.concurrent.ConcurrentHashMap<>();

    /** Records the creature class of an added type. */
    public static void mcc$setCreatureClass(EnumCreatureType type, Class<?> creatureClass) {
        if (type != null && creatureClass != null) mcc$ADDED_CLASSES.put(type, creatureClass);
    }

    /** The legacy class a creature of this type is an instance of (Forge's spawn-cap check). */
    public Class<?> getCreatureClass() {
        Class<?> added = mcc$ADDED_CLASSES.get(this);
        if (added != null) return added;
        return switch (name()) {
            case "MONSTER" -> legacyconnector.v1_12_2.net.minecraft.entity.monster.IMob.class;
            case "CREATURE" -> legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityAnimal.class;
            case "AMBIENT" -> legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityAmbientCreature.class;
            case "WATER_CREATURE" -> legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityWaterMob.class;
            default -> Object.class;
        };
    }
}
