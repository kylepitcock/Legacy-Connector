package legacyconnector.v1_12_2.net.minecraft.world;

/** 1.12.2 difficulty; the same four levels with the same ids as the modern {@link net.minecraft.world.Difficulty}. */
public enum EnumDifficulty {
    PEACEFUL(0, "options.difficulty.peaceful"),
    EASY(1, "options.difficulty.easy"),
    NORMAL(2, "options.difficulty.normal"),
    HARD(3, "options.difficulty.hard");

    private static final EnumDifficulty[] ID_MAPPING = values();
    private final int id;
    private final String translationKey;

    EnumDifficulty(int id, String translationKey) {
        this.id = id;
        this.translationKey = translationKey;
    }

    public int getId() {
        return id;
    }

    public static EnumDifficulty byId(int id) {
        return ID_MAPPING[Math.floorMod(id, ID_MAPPING.length)];
    }

    public static EnumDifficulty getDifficultyEnum(int id) {
        return byId(id);
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public String getDifficultyResourceKey() {
        return translationKey;
    }

    public static EnumDifficulty mcc$of(net.minecraft.world.Difficulty d) {
        return d == null ? NORMAL : byId(d.getId());
    }

    public net.minecraft.world.Difficulty mcc$modern() {
        return net.minecraft.world.Difficulty.byId(id);
    }

    /** Forge's older name of {@link #getId}. */
    public int getDifficultyId() {
        return getId();
    }
}
