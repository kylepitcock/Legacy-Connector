package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Records native provenance without running modern entity initialization on original Beta hosts. */
@Mixin(Mob.class)
public interface BetaSpawnMobAccess {
    @Accessor("spawnType") void legacyconnector$spawnType(MobSpawnType reason);
}
