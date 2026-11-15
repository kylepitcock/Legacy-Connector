package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;

/**
 * Stands in for the static helper of {@code FrostWalkerEnchantment} (1.20.1): {@code onEntityMoved(entity, level, pos, enchantmentLevel)} turned still water
 * under the entity into frosted ice. In 1.21.1 the enchantment is data (an effect component), so the class is gone; the same effect is applied here.
 */
public final class LegacyFrostWalker {
    private LegacyFrostWalker() {}

    public static void onEntityMoved(LivingEntity living, Level level, BlockPos pos, int enchantmentLevel) {
        if (!living.onGround()) return;
        BlockState frosted = Blocks.FROSTED_ICE.defaultBlockState();
        int radius = Math.min(16, 2 + enchantmentLevel);
        BlockPos.MutableBlockPos above = new BlockPos.MutableBlockPos();
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-radius, -1, -radius), pos.offset(radius, -1, radius))) {
            if (!p.closerToCenterThan(living.position(), radius)) continue;
            above.set(p.getX(), p.getY() + 1, p.getZ());
            if (!level.getBlockState(above).isAir()) continue;
            BlockState state = level.getBlockState(p);
            boolean sourceWater = state.is(Blocks.WATER) && state.getValue(LiquidBlock.LEVEL) == 0;
            if (sourceWater && frosted.canSurvive(level, p) && level.isUnobstructed(frosted, p, CollisionContext.empty())) {
                level.setBlockAndUpdate(p, frosted);
                level.scheduleTick(p, Blocks.FROSTED_ICE, Mth.nextInt(living.getRandom(), 60, 120));
            }
        }
    }
}
