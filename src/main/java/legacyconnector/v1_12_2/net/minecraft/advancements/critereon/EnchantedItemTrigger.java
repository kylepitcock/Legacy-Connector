package legacyconnector.v1_12_2.net.minecraft.advancements.critereon;

import legacyconnector.v1_12_2.net.minecraft.advancements.ICriterionTrigger;
import legacyconnector.v1_12_2.net.minecraft.advancements.PlayerAdvancements;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/**
 * Vanilla's enchanted_item trigger. Mods fire it themselves ({@code CriteriaTriggers.ENCHANTED_ITEM.trigger(...)}); it fires the modern trigger, whose
 * listeners are the modern advancements. Listeners added here are not used (vanilla advancements listen to the modern trigger).
 */
public class EnchantedItemTrigger implements ICriterionTrigger<EnchantedItemTrigger.Instance> {
    private static final ResourceLocation ID = new ResourceLocation("enchanted_item");

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

    public void trigger(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP player, legacyconnector.v1_12_2.net.minecraft.item.ItemStack item, int levelsSpent) {
        var sp = player == null ? null : player.mcc$serverPlayer();
        if (sp == null) return;
        net.minecraft.advancements.CriteriaTriggers.ENCHANTED_ITEM.trigger(sp, org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.toModern(item), levelsSpent);
    }

    public static class Instance extends AbstractCriterionInstance {
        public Instance() {
            super(ID);
        }
    }
}
