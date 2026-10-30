package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import org.kylepitcock.legacyconnector.core.synth.Synth;

/**
 * Methods of hand-written 1.12.2 shim classes (advancements, events, ...) whose legacy signatures name classes only the synthesizer
 * provides, answered with their real behaviour through {@link Synth#addInstanceMethod} / {@link Synth#addStaticMethod}.
 */
public final class CommonSynth1122 {
    private CommonSynth1122() {}

    private static final java.lang.reflect.Field BEACON_LEVELS = beaconLevels();

    private static java.lang.reflect.Field beaconLevels() {
        try {
            return net.neoforged.fml.util.ObfuscationReflectionHelper.findField(net.minecraft.world.level.block.entity.BeaconBlockEntity.class, "levels");
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static void register() {
        // CriteriaTriggers.CONSTRUCT_BEACON.trigger(player, beacon): the beacon is a synthesized TileEntityBeacon; its levels are the modern beacon's
        Synth.addInstanceMethod("net.minecraft.advancements.critereon.ConstructBeaconTrigger.trigger", (self, args) -> {
            if (!(self instanceof legacyconnector.v1_12_2.net.minecraft.advancements.critereon.ConstructBeaconTrigger trigger) || args.length < 2
                    || !(args[0] instanceof legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP player)) {
                return Synth.NONE;
            }
            int levels = 0;
            if (args[1] instanceof legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity te && te.getWorld() != null && te.getPos() != null
                    && te.getWorld().mcc$level() != null
                    && te.getWorld().mcc$level().getBlockEntity(te.getPos().mcc$modern()) instanceof net.minecraft.world.level.block.entity.BeaconBlockEntity beacon
                    && BEACON_LEVELS != null) {
                try {
                    levels = BEACON_LEVELS.getInt(beacon);
                } catch (IllegalAccessException e) {
                    // no levels
                }
            }
            trigger.mcc$trigger(player, levels);
            return null;
        });
    }
}
