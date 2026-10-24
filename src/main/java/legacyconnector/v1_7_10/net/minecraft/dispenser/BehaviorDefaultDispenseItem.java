package legacyconnector.v1_7_10.net.minecraft.dispenser;

import legacyconnector.v1_7_10.net.minecraft.block.BlockDispenser;
import legacyconnector.v1_7_10.net.minecraft.entity.item.EntityItem;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.util.EnumFacing;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** The dispenser's default: drops one item in front of it. Mods subclass it and override {@link #dispenseStack}. */
public class BehaviorDefaultDispenseItem implements IBehaviorDispenseItem {
    @Override
    public final ItemStack dispense(IBlockSource source, ItemStack stack) {
        ItemStack rest = dispenseStack(source, stack);
        playDispenseSound(source);
        spawnDispenseParticles(source, BlockDispenser.getFacing(source.getBlockMetadata()));
        return rest;
    }

    protected ItemStack dispenseStack(IBlockSource source, ItemStack stack) {
        EnumFacing facing = BlockDispenser.getFacing(source.getBlockMetadata());
        IPosition position = BlockDispenser.getIPositionFromBlockSource(source);
        ItemStack one = stack.splitStack(1);
        doDispense(source.getWorld(), one, 6, facing, position);
        return stack;
    }

    public static void doDispense(World world, ItemStack stack, int speed, EnumFacing facing, IPosition position) {
        double x = position.getX();
        double y = position.getY();
        double z = position.getZ();
        EntityItem item = new EntityItem(world, x, y - 0.3D, z, stack);
        double power = world.rand.nextDouble() * 0.1D + 0.2D;
        mcc$set$motionX(item, facing.getFrontOffsetX() * power);
        mcc$set$motionY(item, 0.20000000298023224D);
        mcc$set$motionZ(item, facing.getFrontOffsetZ() * power);
        mcc$set$motionX(item, mcc$get$motionX(item) + (world.rand.nextGaussian() * 0.007499999832361937D * speed));
        mcc$set$motionY(item, mcc$get$motionY(item) + (world.rand.nextGaussian() * 0.007499999832361937D * speed));
        mcc$set$motionZ(item, mcc$get$motionZ(item) + (world.rand.nextGaussian() * 0.007499999832361937D * speed));
        world.spawnEntityInWorld(item);
    }

    protected void playDispenseSound(IBlockSource source) {
        source.getWorld().playAuxSFX(1000, source.getXInt(), source.getYInt(), source.getZInt(), 0);
    }

    protected void spawnDispenseParticles(IBlockSource source, EnumFacing facing) {
        source.getWorld().playAuxSFX(2000, source.getXInt(), source.getYInt(), source.getZInt(), facing.getFrontOffsetX() + 1 + (facing.getFrontOffsetZ() + 1) * 3);
    }
}
