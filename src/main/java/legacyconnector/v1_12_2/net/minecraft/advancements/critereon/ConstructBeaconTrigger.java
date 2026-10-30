package legacyconnector.v1_12_2.net.minecraft.advancements.critereon;

import legacyconnector.v1_12_2.net.minecraft.advancements.ICriterionTrigger;
import legacyconnector.v1_12_2.net.minecraft.advancements.PlayerAdvancements;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/**
 * Vanilla's construct_beacon trigger. Mods fire it ({@code trigger(player, beacon)}, answered by {@code CommonSynth1122} because the
 * beacon class is only synthesized); it fires the modern trigger with the beacon's pyramid levels.
 */
public class ConstructBeaconTrigger implements ICriterionTrigger<ConstructBeaconTrigger.Instance> {
    private static final ResourceLocation ID = new ResourceLocation("construct_beacon");

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

    /** Fires the modern trigger for a beacon of {@code levels} pyramid levels. */
    public void mcc$trigger(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP player, int levels) {
        var sp = player == null ? null : player.mcc$serverPlayer();
        if (sp != null) net.minecraft.advancements.CriteriaTriggers.CONSTRUCT_BEACON.trigger(sp, levels);
    }

    public static class Instance extends AbstractCriterionInstance {
        public Instance() {
            super(ID);
        }
    }
}
