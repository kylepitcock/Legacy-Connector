package legacyconnector.v1_12_2.net.minecraft.advancements.critereon;

import legacyconnector.v1_12_2.net.minecraft.advancements.ICriterionTrigger;
import legacyconnector.v1_12_2.net.minecraft.advancements.PlayerAdvancements;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/**
 * Vanilla's player_killed_entity / entity_killed_player triggers. Listeners added here are not used: the modern advancements listen to the modern trigger, which {@code trigger} fires.
 */
public class KilledTrigger implements ICriterionTrigger<KilledTrigger.Instance> {
    private static final ResourceLocation ID = new ResourceLocation("player_killed_entity");

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
        return new Instance(ID, EntityPredicate.ANY, DamageSourcePredicate.ANY);
    }

    public void trigger(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP player,
            legacyconnector.v1_12_2.net.minecraft.entity.Entity entity, legacyconnector.v1_12_2.net.minecraft.util.DamageSource source) {
        var sp = player == null ? null : player.mcc$serverPlayer();
        if (sp == null || entity == null || source == null || !(entity.mcc$modern() instanceof net.minecraft.world.entity.Entity e)) return;
        net.minecraft.advancements.CriteriaTriggers.PLAYER_KILLED_ENTITY.trigger(sp, e, source.mcc$modern(sp.level()));
    }

    public static class Instance extends AbstractCriterionInstance {
        private final EntityPredicate entity;
        private final DamageSourcePredicate killingBlow;

        public Instance(ResourceLocation criterion, EntityPredicate entity, DamageSourcePredicate killingBlow) {
            super(criterion);
            this.entity = entity;
            this.killingBlow = killingBlow;
        }

        public boolean test(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP player,
                legacyconnector.v1_12_2.net.minecraft.entity.Entity entityIn, legacyconnector.v1_12_2.net.minecraft.util.DamageSource source) {
            return killingBlow.test(player, source) && entity.test(player, entityIn);
        }
    }
}
