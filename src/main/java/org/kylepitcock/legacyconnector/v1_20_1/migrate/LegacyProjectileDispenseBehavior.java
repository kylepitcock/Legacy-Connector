package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;

/**
 * Stands in for {@code net.minecraft.core.dispenser.AbstractProjectileDispenseBehavior} (1.20.1): 1.21.1 has {@code ProjectileDispenseBehavior}, which asks a
 * {@code ProjectileItem} instead of a {@code getProjectile} override. Mods that subclass the old class keep their override.
 */
public abstract class LegacyProjectileDispenseBehavior extends DefaultDispenseItemBehavior {
    @Override
    public ItemStack execute(BlockSource source, ItemStack stack) {
        Level level = source.level();
        Position position = DispenserBlock.getDispensePosition(source);
        Direction direction = source.state().getValue(DispenserBlock.FACING);
        Projectile projectile = getProjectile(level, position, stack);
        projectile.shoot(direction.getStepX(), direction.getStepY() + 0.1F, direction.getStepZ(), getPower(), getUncertainty());
        level.addFreshEntity(projectile);
        stack.shrink(1);
        return stack;
    }

    @Override
    protected void playSound(BlockSource source) {
        source.level().levelEvent(1002, source.pos(), 0);
    }

    protected abstract Projectile getProjectile(Level level, Position position, ItemStack stack);

    protected float getUncertainty() {
        return 6.0F;
    }

    protected float getPower() {
        return 1.1F;
    }
}
