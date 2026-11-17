package legacyconnector.v1_12_2.net.minecraft.advancements.critereon;

import legacyconnector.v1_12_2.net.minecraft.advancements.ICriterionTrigger;
import legacyconnector.v1_12_2.net.minecraft.advancements.PlayerAdvancements;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/**
 * Vanilla's bred_animals trigger. Mods fire it themselves; it fires the modern trigger, whose listeners are the modern advancements. Listeners
 * added here are not used (vanilla advancements listen to the modern trigger).
 */
public class BredAnimalsTrigger implements ICriterionTrigger<BredAnimalsTrigger.Instance> {
    private static final ResourceLocation ID = new ResourceLocation("bred_animals");

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
            legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityAnimal parent,
            legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityAnimal partner,
            legacyconnector.v1_12_2.net.minecraft.entity.EntityAgeable child) {
        var sp = player == null ? null : player.mcc$serverPlayer();
        if (sp == null || parent == null || partner == null) return;
        if (parent.mcc$modern() instanceof net.minecraft.world.entity.animal.Animal a && partner.mcc$modern() instanceof net.minecraft.world.entity.animal.Animal b) {
            net.minecraft.world.entity.AgeableMob c = child != null && child.mcc$modern() instanceof net.minecraft.world.entity.AgeableMob m ? m : null;
            net.minecraft.advancements.CriteriaTriggers.BRED_ANIMALS.trigger(sp, a, b, c);
        }
    }

    public static class Instance extends AbstractCriterionInstance {
        public Instance() {
            super(ID);
        }
    }
}
