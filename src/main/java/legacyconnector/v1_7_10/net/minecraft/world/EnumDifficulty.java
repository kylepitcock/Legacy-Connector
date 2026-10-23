package legacyconnector.v1_7_10.net.minecraft.world;

/** The four difficulties of 1.7.10, with the ids mods compare (0 peaceful .. 3 hard). */
public enum EnumDifficulty {
    PEACEFUL(0, "options.difficulty.peaceful"),
    EASY(1, "options.difficulty.easy"),
    NORMAL(2, "options.difficulty.normal"),
    HARD(3, "options.difficulty.hard");

    private final int difficultyId;
    private final String difficultyResourceKey;

    EnumDifficulty(int id, String key) {
        this.difficultyId = id;
        this.difficultyResourceKey = key;
    }

    public int getDifficultyId() {
        return difficultyId;
    }

    public static EnumDifficulty getDifficultyEnum(int id) {
        for (EnumDifficulty d : values()) if (d.difficultyId == id) return d;
        return NORMAL;
    }

    public String getDifficultyResourceKey() {
        return difficultyResourceKey;
    }

    public static EnumDifficulty mcc$of(net.minecraft.world.Difficulty modern) {
        return getDifficultyEnum(modern.getId());
    }
}
