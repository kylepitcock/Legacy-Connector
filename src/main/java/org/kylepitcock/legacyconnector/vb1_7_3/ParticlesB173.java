package org.kylepitcock.legacyconnector.vb1_7_3;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;

/** b1.7.3 particle names ({@code World.spawnParticle("smoke", ...)}) as modern particles; unknown names are skipped. */
public final class ParticlesB173 {
    private ParticlesB173() {}

    public static ParticleOptions of(String name) {
        if (name == null) return null;
        return switch (name) {
            case "smoke" -> ParticleTypes.SMOKE;
            case "largesmoke" -> ParticleTypes.LARGE_SMOKE;
            case "flame" -> ParticleTypes.FLAME;
            case "reddust" -> DustParticleOptions.REDSTONE;
            case "portal" -> ParticleTypes.PORTAL;
            case "explode" -> ParticleTypes.POOF;
            case "bubble" -> ParticleTypes.BUBBLE;
            case "splash" -> ParticleTypes.SPLASH;
            case "lava" -> ParticleTypes.LAVA;
            case "note" -> ParticleTypes.NOTE;
            case "heart" -> ParticleTypes.HEART;
            case "snowballpoof", "snowshovel" -> ParticleTypes.ITEM_SNOWBALL;
            case "slime" -> ParticleTypes.ITEM_SLIME;
            case "dripWater" -> ParticleTypes.DRIPPING_WATER;
            case "dripLava" -> ParticleTypes.DRIPPING_LAVA;
            case "footstep" -> ParticleTypes.CRIT;
            default -> null;
        };
    }
}
