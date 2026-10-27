package legacyconnector.v1_12_2.net.minecraft.advancements.critereon;

import legacyconnector.v1_12_2.net.minecraft.advancements.ICriterionTrigger;
import legacyconnector.v1_12_2.net.minecraft.advancements.PlayerAdvancements;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/**
 * Vanilla's enter_block trigger. Mods fire it themselves ({@code CriteriaTriggers.ENTER_BLOCK.trigger(...)}); it fires the modern trigger, whose
 * listeners are the modern advancements. Listeners added here are not used (vanilla advancements listen to the modern trigger).
 */
public class EnterBlockTrigger implements ICriterionTrigger<EnterBlockTrigger.Instance> {
    private static final ResourceLocation ID = new ResourceLocation("enter_block");

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

    public void trigger(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP player, legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        var sp = player == null ? null : player.mcc$serverPlayer();
        if (sp == null || state == null) return;
        net.minecraft.advancements.CriteriaTriggers.ENTER_BLOCK.trigger(sp, org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122.toModern(state));
    }

    public static class Instance extends AbstractCriterionInstance {
        public Instance() {
            super(ID);
        }
    }
}
