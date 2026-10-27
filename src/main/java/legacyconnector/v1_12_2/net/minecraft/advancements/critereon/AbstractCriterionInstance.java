package legacyconnector.v1_12_2.net.minecraft.advancements.critereon;

import legacyconnector.v1_12_2.net.minecraft.advancements.ICriterionInstance;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** The base of mods' criterion instances: it only knows its trigger's id. */
public class AbstractCriterionInstance implements ICriterionInstance {
    private final ResourceLocation criterion;

    public AbstractCriterionInstance(ResourceLocation criterionIn) {
        this.criterion = criterionIn;
    }

    @Override
    public ResourceLocation getId() {
        return criterion;
    }

    @Override
    public String toString() {
        return "AbstractCriterionInstance{criterion=" + criterion + '}';
    }
}
