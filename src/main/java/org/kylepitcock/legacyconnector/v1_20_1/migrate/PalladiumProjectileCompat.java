package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.Vec3;

/**
 * Rebuilds Palladium 4.5.10's legacy explosion packet with metadata from the
 * same Explosion object. The 1.21.1 packet requires block interaction,
 * particles and sound in addition to the legacy position, radius, affected
 * blocks and player knockback.
 */
public final class PalladiumProjectileCompat {
    private PalladiumProjectileCompat() {}

    public static ClientboundExplodePacket createExplodePacket(
            double x,
            double y,
            double z,
            float radius,
            List<BlockPos> affectedBlocks,
            Vec3 playerVelocity,
            Explosion explosion) {
        return new ClientboundExplodePacket(
                x,
                y,
                z,
                radius,
                affectedBlocks,
                playerVelocity,
                explosion.getBlockInteraction(),
                explosion.getSmallExplosionParticles(),
                explosion.getLargeExplosionParticles(),
                explosion.getExplosionSound());
    }
}
