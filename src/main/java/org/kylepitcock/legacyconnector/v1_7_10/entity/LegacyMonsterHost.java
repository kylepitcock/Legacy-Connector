package org.kylepitcock.legacyconnector.v1_7_10.entity;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;

/** Host of a legacy hostile mob ({@code IMob}): an {@link Enemy}, removed on Peaceful like vanilla monsters. */
public class LegacyMonsterHost extends LegacyMobHost implements Enemy {
    public LegacyMonsterHost(EntityType<? extends LegacyMonsterHost> type, Level level, Entity adopted) {
        super(type, level, adopted);
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return true;
    }
}
