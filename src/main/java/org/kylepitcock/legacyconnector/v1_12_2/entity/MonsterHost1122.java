package org.kylepitcock.legacyconnector.v1_12_2.entity;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;

/** Host of a 1.12.2 hostile mob ({@code IMob}): an {@link Enemy}, removed on Peaceful like vanilla monsters. */
public class MonsterHost1122 extends MobHost1122 implements Enemy {
    public MonsterHost1122(EntityType<? extends MonsterHost1122> type, Level level, Entity adopted) {
        super(type, level, adopted);
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return true;
    }
}
