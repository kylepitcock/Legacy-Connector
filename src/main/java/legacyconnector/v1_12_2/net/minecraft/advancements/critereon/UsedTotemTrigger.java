package legacyconnector.v1_12_2.net.minecraft.advancements.critereon;

import legacyconnector.v1_12_2.net.minecraft.advancements.ICriterionTrigger;
import legacyconnector.v1_12_2.net.minecraft.advancements.PlayerAdvancements;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/**
 * Vanilla's used_totem trigger. Mods fire it themselves ({@code CriteriaTriggers.USED_TOTEM.trigger(...)}); it fires the modern trigger, whose
 * listeners are the modern advancements. Listeners added here are not used (vanilla advancements listen to the modern trigger).
 */
public class UsedTotemTrigger implements ICriterionTrigger<UsedTotemTrigger.Instance> {
    private static final ResourceLocation ID = new ResourceLocation("used_totem");

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

    public void trigger(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP player, legacyconnector.v1_12_2.net.minecraft.item.ItemStack item) {
        var sp = player == null ? null : player.mcc$serverPlayer();
        if (sp == null) return;
        net.minecraft.advancements.CriteriaTriggers.USED_TOTEM.trigger(sp, org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.toModern(item));
    }

    public static class Instance extends AbstractCriterionInstance {
        public Instance() {
            super(ID);
        }
    }
}
