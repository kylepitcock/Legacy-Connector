package legacyconnector.v1_12_2.net.minecraft.dispenser;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.IProjectile;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** A dispenser behaviour that shoots the projectile the subclass makes from the stack, as 1.12.2's. */
public abstract class BehaviorProjectileDispense extends BehaviorDefaultDispenseItem {
    @Override
    public ItemStack dispenseStack(IBlockSource source, ItemStack stack) {
        World world = source.getWorld();
        EnumFacing facing = mcc$facing(source);
        IPosition position = new PositionImpl(source.getX() + 0.7D * facing.getXOffset(), source.getY() + 0.7D * facing.getYOffset(),
                source.getZ() + 0.7D * facing.getZOffset());
        IProjectile projectile = getProjectileEntity(world, position, stack);
        if (projectile == null) return stack;
        projectile.shoot(facing.getXOffset(), facing.getYOffset() + 0.1F, facing.getZOffset(), getProjectileVelocity(), getProjectileInaccuracy());
        if (projectile instanceof Entity e) world.spawnEntity(e);
        stack.shrink(1);
        return stack;
    }

    /** The facing of the dispenser (BlockDispenser.FACING of its state). */
    static EnumFacing mcc$facing(IBlockSource source) {
        try {
            var p = source.getBlockPos();
            var state = source.getWorld().mcc$level().getBlockState(new net.minecraft.core.BlockPos(p.getX(), p.getY(), p.getZ()));
            if (state.hasProperty(net.minecraft.world.level.block.DispenserBlock.FACING)) {
                return EnumFacing.mcc$of(state.getValue(net.minecraft.world.level.block.DispenserBlock.FACING));
            }
        } catch (RuntimeException e) {
            // below
        }
        return EnumFacing.NORTH;
    }

    @Override
    protected void playDispenseSound(IBlockSource source) {
        var p = source.getBlockPos();
        source.getWorld().mcc$level().levelEvent(1002, new net.minecraft.core.BlockPos(p.getX(), p.getY(), p.getZ()), 0);
    }

    protected abstract IProjectile getProjectileEntity(World worldIn, IPosition position, ItemStack stackIn);

    protected float getProjectileInaccuracy() {
        return 6.0F;
    }

    protected float getProjectileVelocity() {
        return 1.1F;
    }
}
