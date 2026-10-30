package legacyconnector.v1_12_2.net.minecraft.advancements;

/**
 * One criterion of an advancement: its trigger's instance. For a modern advancement's criterion ({@link #mcc$of}) the instance names the
 * modern trigger (its conditions stay the modern game's).
 */
public class Criterion {
    private final ICriterionInstance criterionInstance;

    public Criterion(ICriterionInstance criterionInstance) {
        this.criterionInstance = criterionInstance;
    }

    public Criterion() {
        this.criterionInstance = null;
    }

    public static Criterion mcc$of(net.minecraft.advancements.Criterion<?> modern) {
        if (modern == null) return new Criterion();
        var key = net.minecraft.core.registries.BuiltInRegistries.TRIGGER_TYPES.getKey(modern.trigger());
        var id = key == null ? new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation("impossible")
                : new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation(key.getNamespace(), key.getPath());
        return new Criterion(new legacyconnector.v1_12_2.net.minecraft.advancements.critereon.AbstractCriterionInstance(id));
    }

    public ICriterionInstance getCriterionInstance() {
        return criterionInstance;
    }
}
