package legacyconnector.v1_12_2.net.minecraftforge.fluids;

import legacyconnector.v1_12_2.net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import legacyconnector.v1_12_2.net.minecraft.dispenser.BlockSourceImpl;
import legacyconnector.v1_12_2.net.minecraft.dispenser.IBlockSource;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntityDispenser;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.IFluidHandlerItem;

/**
 * Forge's dispenser behaviour for fluid containers: a full one places its fluid in front of the dispenser, an empty one picks up the
 * fluid there (the filled container goes back into the dispenser, or is dropped when it is full).
 */
public class DispenseFluidContainer extends BehaviorDefaultDispenseItem {
    private static final DispenseFluidContainer INSTANCE = new DispenseFluidContainer();
    private final BehaviorDefaultDispenseItem dispenseBehavior = new BehaviorDefaultDispenseItem();

    public static DispenseFluidContainer getInstance() {
        return INSTANCE;
    }

    protected DispenseFluidContainer() {
    }

    @Override
    public ItemStack dispenseStack(IBlockSource source, ItemStack stack) {
        return FluidUtil.getFluidContained(stack) != null ? dumpContainer(source, stack) : fillContainer(source, stack);
    }

    private static EnumFacing facing(IBlockSource source) {
        return source instanceof BlockSourceImpl impl ? impl.mcc$facing() : new BlockSourceImpl(source.getWorld(), source.getBlockPos()).mcc$facing();
    }

    private ItemStack fillContainer(IBlockSource source, ItemStack stack) {
        World world = source.getWorld();
        EnumFacing facing = facing(source);
        BlockPos front = source.getBlockPos().offset(facing);
        FluidActionResult result = FluidUtil.tryPickUpFluid(stack.copy().splitStack(1), null, world, front, facing.getOpposite());
        if (!result.isSuccess()) return dispenseBehavior.dispense(source, stack);
        ItemStack filled = result.getResult();
        stack.shrink(1);
        if (stack.isEmpty()) return filled;
        TileEntityDispenser te = source.<legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity>getBlockTileEntity() instanceof TileEntityDispenser d ? d : null;
        if (te == null || te.addItemStack(filled) < 0) dispenseBehavior.dispense(source, filled);
        return stack;
    }

    private ItemStack dumpContainer(IBlockSource source, ItemStack stack) {
        ItemStack single = stack.copy();
        single.setCount(1);
        IFluidHandlerItem handler = FluidUtil.getFluidHandler(single);
        if (handler == null) return dispenseBehavior.dispense(source, stack);
        FluidStack contained = handler.drain(Fluid.BUCKET_VOLUME, false);
        World world = source.getWorld();
        BlockPos front = source.getBlockPos().offset(facing(source));
        if (contained == null || contained.amount < Fluid.BUCKET_VOLUME || !place(world, front, contained)) {
            return dispenseBehavior.dispense(source, stack);
        }
        handler.drain(Fluid.BUCKET_VOLUME, true);
        ItemStack emptied = handler.getContainer();
        if (stack.getCount() == 1) return emptied;
        stack.shrink(1);
        TileEntityDispenser te = source.<legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity>getBlockTileEntity() instanceof TileEntityDispenser d ? d : null;
        if (!emptied.isEmpty() && (te == null || te.addItemStack(emptied) < 0)) dispenseBehavior.dispense(source, emptied);
        return stack;
    }

    /** Places a bucket of the fluid where the block can be replaced: vanilla water and lava as the modern fluids, others as their block. */
    private static boolean place(World world, BlockPos pos, FluidStack fluid) {
        var level = world == null ? null : world.mcc$level();
        if (level == null || fluid.getFluid() == null) return false;
        var mp = new net.minecraft.core.BlockPos(pos.getX(), pos.getY(), pos.getZ());
        var here = level.getBlockState(mp);
        if (!here.canBeReplaced() && !here.isAir()) return false;
        String name = fluid.getFluid().getName();
        if ("water".equals(name) || "lava".equals(name)) {
            if ("water".equals(name) && level.dimensionType().ultraWarm()) {
                level.playSound(null, mp, net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH, net.minecraft.sounds.SoundSource.BLOCKS, 0.5F, 2.6F);
                return true;
            }
            if (!here.isAir() && !here.liquid()) level.destroyBlock(mp, true);
            level.setBlock(mp, ("water".equals(name) ? net.minecraft.world.level.block.Blocks.WATER : net.minecraft.world.level.block.Blocks.LAVA)
                    .defaultBlockState(), 11);
            return true;
        }
        var block = fluid.getFluid().getBlock();
        if (block == null || !fluid.getFluid().canBePlacedInWorld()) return false;
        FluidUtil.destroyBlockOnFluidPlacement(world, pos);
        return world.setBlockState(pos, block.getDefaultState(), 11);
    }
}
