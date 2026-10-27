package legacyconnector.v1_12_2.net.minecraft.world;

/** The local difficulty of a position (the same formula in both versions). */
public class DifficultyInstance {
    private final EnumDifficulty worldDifficulty;
    private final float additionalDifficulty;

    public DifficultyInstance(EnumDifficulty worldDifficulty, long worldTime, long chunkInhabitedTime, float moonPhaseFactor) {
        this.worldDifficulty = worldDifficulty;
        this.additionalDifficulty = new net.minecraft.world.DifficultyInstance(worldDifficulty.mcc$modern(), worldTime, chunkInhabitedTime, moonPhaseFactor)
                .getEffectiveDifficulty();
    }

    private DifficultyInstance(net.minecraft.world.DifficultyInstance modern) {
        this.worldDifficulty = EnumDifficulty.mcc$of(modern.getDifficulty());
        this.additionalDifficulty = modern.getEffectiveDifficulty();
    }

    public static DifficultyInstance mcc$of(net.minecraft.world.DifficultyInstance modern) {
        return new DifficultyInstance(modern);
    }

    public EnumDifficulty getDifficulty() {
        return worldDifficulty;
    }

    public float getAdditionalDifficulty() {
        return additionalDifficulty;
    }

    public boolean isHarderThan(float value) {
        return additionalDifficulty > value;
    }

    public float getClampedAdditionalDifficulty() {
        if (additionalDifficulty < 2.0F) return 0.0F;
        return additionalDifficulty > 4.0F ? 1.0F : (additionalDifficulty - 2.0F) / 2.0F;
    }
}
