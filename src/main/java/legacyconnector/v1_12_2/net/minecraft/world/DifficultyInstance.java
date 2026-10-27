package legacyconnector.v1_12_2.net.minecraft.world;

/** 1.12.2 regional difficulty, computed like vanilla (and the modern {@link net.minecraft.world.DifficultyInstance}). */
public class DifficultyInstance {
    private final EnumDifficulty worldDifficulty;
    private final float additionalDifficulty;

    public DifficultyInstance(EnumDifficulty worldDifficulty, long worldTime, long chunkInhabitedTime, float moonPhaseFactor) {
        this.worldDifficulty = worldDifficulty;
        this.additionalDifficulty = calculateAdditionalDifficulty(worldDifficulty, worldTime, chunkInhabitedTime, moonPhaseFactor);
    }

    public static DifficultyInstance mcc$of(net.minecraft.world.DifficultyInstance modern) {
        if (modern == null) return new DifficultyInstance(EnumDifficulty.NORMAL, 0, 0, 0);
        DifficultyInstance d = new DifficultyInstance(EnumDifficulty.mcc$of(modern.getDifficulty()), 0, 0, 0);
        d.mcc$override = modern.getEffectiveDifficulty();
        return d;
    }

    private float mcc$override = Float.NaN;

    public EnumDifficulty getDifficulty() {
        return worldDifficulty;
    }

    public float getAdditionalDifficulty() {
        return Float.isNaN(mcc$override) ? additionalDifficulty : mcc$override;
    }

    public boolean isHarderThan(float value) {
        return getAdditionalDifficulty() > value;
    }

    public float getClampedAdditionalDifficulty() {
        float f = getAdditionalDifficulty();
        if (f < 2.0F) return 0.0F;
        return f > 4.0F ? 1.0F : (f - 2.0F) / 2.0F;
    }

    private static float calculateAdditionalDifficulty(EnumDifficulty difficulty, long worldTime, long chunkInhabitedTime, float moonPhaseFactor) {
        if (difficulty == EnumDifficulty.PEACEFUL) return 0.0F;
        boolean hard = difficulty == EnumDifficulty.HARD;
        float f = 0.75F;
        float f1 = clamp(((float) worldTime + -72000.0F) / 1440000.0F, 0.0F, 1.0F) * 0.25F;
        f += f1;
        float f2 = 0.0F;
        f2 += clamp((float) chunkInhabitedTime / 3600000.0F, 0.0F, 1.0F) * (hard ? 1.0F : 0.75F);
        f2 += clamp(moonPhaseFactor * 0.25F, 0.0F, f1);
        if (difficulty == EnumDifficulty.EASY) f2 *= 0.5F;
        f += f2;
        return (float) difficulty.getId() * f;
    }

    private static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : Math.min(v, hi);
    }
}
