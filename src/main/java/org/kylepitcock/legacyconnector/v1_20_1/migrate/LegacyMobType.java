package org.kylepitcock.legacyconnector.v1_20_1.migrate;

/**
 * Stands in for {@code net.minecraft.world.entity.MobType} (1.20.1), which 1.21.1 replaced by entity type tags ({@code #minecraft:undead}...). The constants keep their
 * identity so {@code == MobType.UNDEAD} checks work; {@link MiscCompat#getMobType} answers from the tags.
 */
public final class LegacyMobType {
    public static final LegacyMobType UNDEFINED = new LegacyMobType("undefined");
    public static final LegacyMobType UNDEAD = new LegacyMobType("undead");
    public static final LegacyMobType ARTHROPOD = new LegacyMobType("arthropod");
    public static final LegacyMobType ILLAGER = new LegacyMobType("illager");
    public static final LegacyMobType WATER = new LegacyMobType("water");

    private final String name;

    public LegacyMobType() {
        this("custom");
    }

    private LegacyMobType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "MobType[" + name + "]";
    }
}
