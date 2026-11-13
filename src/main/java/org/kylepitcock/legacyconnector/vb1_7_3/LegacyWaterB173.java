package org.kylepitcock.legacyconnector.vb1_7_3;

import legacyconnector.vb1_7_3.net.minecraft.block.LiquidBlock;
import legacyconnector.vb1_7_3.net.minecraft.block.material.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Beta decided water physics by material: any fluid block of {@code Material.WATER} (BuildCraft's oil is one) slowed and buoyed
 * whatever stood in it. Such a block has no native fluid state, so the native water predicates do not see it; this applies Beta's
 * water motion (a fifth of the speed lost, a small sink, swim up while jumping, no fall damage) to entities inside one.
 */
public final class LegacyWaterB173 {
    private LegacyWaterB173() {}
    private static boolean installed;

    public static synchronized void install() {
        if (installed) return;
        installed = true;
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(LegacyWaterB173::onTick);
    }

    private static void onTick(EntityTickEvent.Post event) {
        var entity = event.getEntity();
        if (entity.isInWater() || entity.isSpectator() || entity.noPhysics) return;
        var level = entity.level();
        // Beta tested the entity's box shrunk by 0.4 at both ends: the fluid must reach 0.4 above its feet.
        double probeY = entity.getY() + 0.4;
        var pos = BlockPos.containing(entity.getX(), probeY, entity.getZ());
        var state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof LegacyB173Block hosted) || !(hosted.legacy() instanceof LiquidBlock)
                || hosted.legacy().material != Material.WATER) return;
        if (probeY > pos.getY() + 1 - LiquidBlock.getFluidHeightFromMeta(BlocksB173.legacyMeta(state))) return;
        Vec3 motion = entity.getDeltaMovement();
        double y = motion.y * 0.8 - 0.02;
        boolean jumping = entity instanceof Player player ? EntityFieldsB173.jumpInput(player) : false;
        if (jumping) y += 0.04;
        entity.setDeltaMovement(motion.x * 0.8, y, motion.z * 0.8);
        entity.fallDistance = 0;
    }
}
