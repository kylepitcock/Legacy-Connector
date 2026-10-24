package legacyconnector.v1_7_10.net.minecraft.dispenser;

import legacyconnector.v1_7_10.net.minecraft.block.BlockDispenser;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.IProjectile;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.util.EnumFacing;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** A dispenser behaviour that launches a projectile (arrows, snowballs, mods' grenades). */
public abstract class BehaviorProjectileDispense extends BehaviorDefaultDispenseItem {
    @Override
    public ItemStack dispenseStack(IBlockSource source, ItemStack stack) {
        World world = source.getWorld();
        IPosition position = BlockDispenser.getIPositionFromBlockSource(source);
        EnumFacing facing = BlockDispenser.getFacing(source.getBlockMetadata());
        IProjectile projectile = getProjectileEntity(world, position);
        projectile.setThrowableHeading(facing.getFrontOffsetX(), facing.getFrontOffsetY() + 0.1F, facing.getFrontOffsetZ(), func_82500_b(), func_82498_a());
        world.spawnEntityInWorld((Entity) projectile);
        stack.splitStack(1);
        return stack;
    }

    @Override
    protected void playDispenseSound(IBlockSource source) {
        source.getWorld().playAuxSFX(1002, source.getXInt(), source.getYInt(), source.getZInt(), 0);
    }

    protected abstract IProjectile getProjectileEntity(World world, IPosition position);

    protected float func_82498_a() {
        return 6.0F;
    }

    protected float func_82500_b() {
        return 1.1F;
    }
}
