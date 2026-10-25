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
}
