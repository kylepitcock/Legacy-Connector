package legacyconnector.v1_12_2.net.minecraft.dispenser;

import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** Drops one of the item in front of the dispenser, with the click sound and the smoke. */
public class BehaviorDefaultDispenseItem implements IBehaviorDispenseItem {
    public BehaviorDefaultDispenseItem() {
    }

    @Override
    public final ItemStack dispense(IBlockSource source, ItemStack stack) {
        ItemStack itemstack = dispenseStack(source, stack);
        playDispenseSound(source);
        spawnDispenseParticles(source, facing(source));
        return itemstack;
    }

    private static EnumFacing facing(IBlockSource source) {
        if (source instanceof BlockSourceImpl impl) return impl.mcc$facing();
        var state = source.getWorld().mcc$level().getBlockState(new net.minecraft.core.BlockPos(source.getBlockPos().getX(), source.getBlockPos().getY(),
                source.getBlockPos().getZ()));
        return state.hasProperty(net.minecraft.world.level.block.DispenserBlock.FACING)
                ? EnumFacing.mcc$of(state.getValue(net.minecraft.world.level.block.DispenserBlock.FACING)) : EnumFacing.NORTH;
    }

    private static IPosition position(IBlockSource source) {
        EnumFacing f = facing(source);
        return new PositionImpl(source.getX() + 0.7D * f.getXOffset(), source.getY() + 0.7D * f.getYOffset(), source.getZ() + 0.7D * f.getZOffset());
    }

    protected ItemStack dispenseStack(IBlockSource source, ItemStack stack) {
        ItemStack itemstack = stack.splitStack(1);
        doDispense(source.getWorld(), itemstack, 6, facing(source), position(source));
        return stack;
    }

    /** Throws the stack out of the dispenser at {@code position}, moving towards {@code facing}. */
    public static void doDispense(World worldIn, ItemStack stack, int speed, EnumFacing facing, IPosition position) {
        if (worldIn == null || worldIn.mcc$level() == null || stack == null || stack.isEmpty()) return;
        net.minecraft.core.dispenser.DefaultDispenseItemBehavior.spawnItem(worldIn.mcc$level(),
                org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.toModern(stack), speed, facing.mcc$modern(),
                new net.minecraft.world.phys.Vec3(position.getX(), position.getY(), position.getZ()));
    }

    protected void playDispenseSound(IBlockSource source) {
        var p = source.getBlockPos();
        source.getWorld().mcc$level().levelEvent(1000, new net.minecraft.core.BlockPos(p.getX(), p.getY(), p.getZ()), 0);
    }

    protected void spawnDispenseParticles(IBlockSource source, EnumFacing facingIn) {
        var p = source.getBlockPos();
        source.getWorld().mcc$level().levelEvent(2000, new net.minecraft.core.BlockPos(p.getX(), p.getY(), p.getZ()), facingIn.mcc$modern().get3DDataValue());
    }

    private int getWorldEventDataFrom(EnumFacing facingIn) {
        return facingIn.getXOffset() + 1 + (facingIn.getZOffset() + 1) * 3;
    }
}
