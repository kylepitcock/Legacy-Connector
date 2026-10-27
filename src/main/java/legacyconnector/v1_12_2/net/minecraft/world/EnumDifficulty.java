package legacyconnector.v1_12_2.net.minecraft.world;

/** 1.12.2 difficulty; ids and keys match the modern {@link net.minecraft.world.Difficulty}. */
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

    public String getTranslationKey() {
        return translationKey;
    }

    public static EnumDifficulty byId(int id) {
        return ID_MAPPING[Math.floorMod(id, ID_MAPPING.length)];
    }

    public static EnumDifficulty mcc$of(net.minecraft.world.Difficulty difficulty) {
        return difficulty == null ? NORMAL : byId(difficulty.getId());
    }

    public net.minecraft.world.Difficulty mcc$modern() {
        return net.minecraft.world.Difficulty.byId(id);
    }
}
