package legacyconnector.v1_12_2.net.minecraft.advancements.critereon;

import legacyconnector.v1_12_2.net.minecraft.advancements.ICriterionTrigger;
import legacyconnector.v1_12_2.net.minecraft.advancements.PlayerAdvancements;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/**
 * Vanilla's changed_dimension trigger. Listeners added here are not used: the modern advancements listen to the modern trigger, which {@code trigger} fires.
 */
public class ChangeDimensionTrigger implements ICriterionTrigger<ChangeDimensionTrigger.Instance> {
    private static final ResourceLocation ID = new ResourceLocation("changed_dimension");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public void addListener(PlayerAdvancements playerAdvancementsIn, ICriterionTrigger.Listener<Instance> listener) {
    }

    @Override
    public void removeListener(PlayerAdvancements playerAdvancementsIn, ICriterionTrigger.Listener<Instance> listener) {
    }

    @Override
    public void removeAllListeners(PlayerAdvancements playerAdvancementsIn) {
    }

    @Override
    public Instance deserializeInstance(com.google.gson.JsonObject json, com.google.gson.JsonDeserializationContext context) {
        return new Instance();
    }

    public void trigger(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP player,
            legacyconnector.v1_12_2.net.minecraft.world.DimensionType from, legacyconnector.v1_12_2.net.minecraft.world.DimensionType to) {
        var sp = player == null ? null : player.mcc$serverPlayer();
        if (sp == null || from == null || to == null) return;
        net.minecraft.advancements.CriteriaTriggers.CHANGED_DIMENSION.trigger(sp,
                org.kylepitcock.legacyconnector.v1_12_2.dimension.Dimensions1122.levelKey(from.getId()),
                org.kylepitcock.legacyconnector.v1_12_2.dimension.Dimensions1122.levelKey(to.getId()));
    }

    public static class Instance extends AbstractCriterionInstance {
        public Instance() {
            super(ID);
        }
    }
}
