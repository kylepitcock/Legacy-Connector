package legacyconnector.v1_12_2.net.minecraftforge.fluids;

import legacyconnector.v1_12_2.net.minecraft.init.Items;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.FluidTankProperties;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.IFluidHandler;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.IFluidHandlerItem;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.IFluidTankProperties;

/** Forge's fluid container helpers: an item's fluid handler (its own capability, or vanilla's buckets) and filled buckets. */
public class FluidUtil {
    /** The fluid handler of one item of the stack (a copy of count 1), or null when the item holds no fluid. */
    public static IFluidHandlerItem getFluidHandler(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        ItemStack copy = stack.copy();
        copy.setCount(1);
        var provider = copy.getItem().initCapabilities(copy, copy.getTagCompound());
        var cap = CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY;
        if (provider != null && cap != null && provider.hasCapability(cap, null)) {
            IFluidHandlerItem handler = provider.getCapability(cap, null);
            if (handler != null) return handler;
        }
        if (copy.getItem() == Items.BUCKET || copy.getItem() == Items.WATER_BUCKET || copy.getItem() == Items.LAVA_BUCKET) return new VanillaBucket(copy);
        return null;
    }

    /** The fluid handler a legacy tile entity at the position offers on a side (its fluid capability), or null. */
    public static IFluidHandler getFluidHandler(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos,
            legacyconnector.v1_12_2.net.minecraft.util.EnumFacing side) {
        if (world == null || pos == null) return null;
        var te = world.getTileEntity(pos);
        var cap = CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY;
        if (te != null && cap != null) {
            try {
                if (te.hasCapability(cap, side)) return te.getCapability(cap, side);
            } catch (RuntimeException | LinkageError e) {
                return null;
            }
        }
        return null;
    }

    /** Moves up to maxAmount from source to destination (simulated first, as Forge did); the moved fluid or null. */
    public static FluidStack tryFluidTransfer(IFluidHandler fluidDestination, IFluidHandler fluidSource, int maxAmount, boolean doTransfer) {
        FluidStack drainable = fluidSource.drain(maxAmount, false);
        if (drainable == null || drainable.amount <= 0) return null;
        int fillable = fluidDestination.fill(drainable, false);
        if (fillable <= 0) return null;
        FluidStack drained = fluidSource.drain(fillable, doTransfer);
        if (drained == null) return null;
        if (doTransfer) drained.amount = fluidDestination.fill(drained, true);
        return drained;
    }

    public static FluidStack tryFluidTransfer(IFluidHandler fluidDestination, IFluidHandler fluidSource, FluidStack resource, boolean doTransfer) {
        FluidStack drainable = fluidSource.drain(resource, false);
        if (drainable == null || drainable.amount <= 0 || !resource.isFluidEqual(drainable)) return null;
        int fillable = fluidDestination.fill(drainable, false);
        if (fillable <= 0) return null;
        FluidStack toDrain = new FluidStack(drainable, fillable);
        FluidStack drained = fluidSource.drain(toDrain, doTransfer);
        if (drained == null) return null;
        if (doTransfer) drained.amount = fluidDestination.fill(drained, true);
        return drained;
    }

    /** Fills one item of the container stack from the handler; the filled container (count 1) on success. */
    public static FluidActionResult tryFillContainer(ItemStack container, IFluidHandler fluidSource, int maxAmount,
            legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player, boolean doFill) {
        IFluidHandlerItem containerHandler = getFluidHandler(container);
        if (containerHandler == null || fluidSource == null) return FluidActionResult.FAILURE;
        FluidStack simulated = tryFluidTransfer(containerHandler, fluidSource, maxAmount, false);
        if (simulated == null) return FluidActionResult.FAILURE;
        if (doFill) {
            tryFluidTransfer(containerHandler, fluidSource, maxAmount, true);
        } else {
            containerHandler.fill(simulated, true);
        }
        return new FluidActionResult(containerHandler.getContainer());
    }

    /** Empties one item of the container stack into the handler; the emptied container (count 1) on success. */
    public static FluidActionResult tryEmptyContainer(ItemStack container, IFluidHandler fluidDestination, int maxAmount,
            legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player, boolean doDrain) {
        IFluidHandlerItem containerHandler = getFluidHandler(container);
        if (containerHandler == null || fluidDestination == null) return FluidActionResult.FAILURE;
        FluidStack transfer = tryFluidTransfer(fluidDestination, containerHandler, maxAmount, doDrain);
        if (transfer == null) return FluidActionResult.FAILURE;
        if (!doDrain) containerHandler.drain(transfer, true);
        return new FluidActionResult(containerHandler.getContainer());
    }

    /** Fills or empties the container and puts the result into the inventory (or the player's), one item at a time. */
    public static FluidActionResult tryFillContainerAndStow(ItemStack container, IFluidHandler fluidSource,
            legacyconnector.v1_12_2.net.minecraftforge.items.IItemHandler inventory, int maxAmount,
            legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player, boolean doFill) {
        return stow(container, tryFillContainer(container, fluidSource, maxAmount, player, false), inventory, player, doFill,
                () -> tryFillContainer(container, fluidSource, maxAmount, player, true));
    }

    public static FluidActionResult tryEmptyContainerAndStow(ItemStack container, IFluidHandler fluidDestination,
            legacyconnector.v1_12_2.net.minecraftforge.items.IItemHandler inventory, int maxAmount,
            legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player, boolean doDrain) {
        return stow(container, tryEmptyContainer(container, fluidDestination, maxAmount, player, false), inventory, player, doDrain,
                () -> tryEmptyContainer(container, fluidDestination, maxAmount, player, true));
    }

    public static FluidActionResult tryEmptyContainerAndStow(ItemStack container, IFluidHandler fluidDestination,
            legacyconnector.v1_12_2.net.minecraftforge.items.IItemHandler inventory, int maxAmount,
            legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player) {
        return tryEmptyContainerAndStow(container, fluidDestination, inventory, maxAmount, player, true);
    }

    private static FluidActionResult stow(ItemStack container, FluidActionResult simulated,
            legacyconnector.v1_12_2.net.minecraftforge.items.IItemHandler inventory, legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player,
            boolean doIt, java.util.function.Supplier<FluidActionResult> real) {
        if (container.isEmpty() || !simulated.isSuccess()) return FluidActionResult.FAILURE;
        boolean creative = player != null && player.mcc$entity instanceof net.minecraft.world.entity.player.Player p && p.getAbilities().instabuild;
        if (creative) {
            if (doIt) real.get();
            return new FluidActionResult(container);
        }
        if (container.getCount() == 1) {
            return doIt ? real.get() : simulated;
        }
        ItemStack result = simulated.getResult();
        ItemStack rest = inventory == null ? result : legacyconnector.v1_12_2.net.minecraftforge.items.ItemHandlerHelper.insertItemStacked(inventory, result, true);
        if (!rest.isEmpty() && player == null) return FluidActionResult.FAILURE;
        if (!doIt) {
            ItemStack left = container.copy();
            left.shrink(1);
            return new FluidActionResult(left);
        }
        FluidActionResult done = real.get();
        ItemStack out = done.getResult();
        ItemStack leftOver = inventory == null ? out : legacyconnector.v1_12_2.net.minecraftforge.items.ItemHandlerHelper.insertItemStacked(inventory, out, false);
        if (!leftOver.isEmpty()) legacyconnector.v1_12_2.net.minecraftforge.items.ItemHandlerHelper.giveItemToPlayer(player, leftOver);
        ItemStack left = container.copy();
        left.shrink(1);
        return new FluidActionResult(left);
    }

    /** A player using the held container on a fluid handler (a tank): empties it into the handler, or fills it from it. */
    public static boolean interactWithFluidHandler(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player,
            legacyconnector.v1_12_2.net.minecraft.util.EnumHand hand, IFluidHandler handler) {
        if (player == null || handler == null || !(player.mcc$entity instanceof net.minecraft.world.entity.player.Player p)) return false;
        var modernHand = hand == legacyconnector.v1_12_2.net.minecraft.util.EnumHand.OFF_HAND ? net.minecraft.world.InteractionHand.OFF_HAND
                : net.minecraft.world.InteractionHand.MAIN_HAND;
        ItemStack held = org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.fromModern(p.getItemInHand(modernHand));
        if (held.isEmpty()) return false;
        FluidActionResult result = tryFillContainerAndStow(held, handler, null, Integer.MAX_VALUE, player, true);
        if (!result.isSuccess()) result = tryEmptyContainerAndStow(held, handler, null, Integer.MAX_VALUE, player, true);
        if (!result.isSuccess()) return false;
        p.setItemInHand(modernHand, org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.toModern(result.getResult()));
        return true;
    }

    public static boolean interactWithFluidHandler(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player,
            legacyconnector.v1_12_2.net.minecraft.util.EnumHand hand, legacyconnector.v1_12_2.net.minecraft.world.World world,
            legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.util.EnumFacing side) {
        return interactWithFluidHandler(player, hand, getFluidHandler(world, pos, side));
    }

    public static FluidStack getFluidContained(ItemStack container) {
        IFluidHandlerItem handler = getFluidHandler(container);
        return handler == null ? null : handler.drain(Integer.MAX_VALUE, false);
    }

    /** A bucket of the fluid: vanilla's water and lava buckets; other fluids have no bucket item here. */
    public static ItemStack getFilledBucket(FluidStack fluidStack) {
        if (fluidStack == null || fluidStack.getFluid() == null) return ItemStack.EMPTY;
        Fluid fluid = fluidStack.getFluid();
        if (fluid == FluidRegistry.WATER || "water".equals(fluid.getName())) return new ItemStack(Items.WATER_BUCKET);
        if (fluid == FluidRegistry.LAVA || "lava".equals(fluid.getName())) return new ItemStack(Items.LAVA_BUCKET);
        return ItemStack.EMPTY;
    }

    /** Forge's FluidBucketWrapper for the vanilla buckets: empty, water or lava, a bucket (1000 mB) at a time. */
    private static final class VanillaBucket implements IFluidHandlerItem {
        private ItemStack container;

        VanillaBucket(ItemStack container) {
            this.container = container;
        }

        private FluidStack contents() {
            if (container.getItem() == Items.WATER_BUCKET) return new FluidStack(FluidRegistry.WATER, 1000);
            if (container.getItem() == Items.LAVA_BUCKET) return new FluidStack(FluidRegistry.LAVA, 1000);
            return null;
        }

        @Override
        public ItemStack getContainer() {
            return container;
        }

        @Override
        public IFluidTankProperties[] getTankProperties() {
            return new IFluidTankProperties[] {new FluidTankProperties(contents(), 1000)};
        }

        @Override
        public int fill(FluidStack resource, boolean doFill) {
            if (container.getCount() != 1 || resource == null || resource.amount < 1000 || contents() != null) return 0;
            ItemStack filled = getFilledBucket(resource);
            if (filled.isEmpty()) return 0;
            if (doFill) container = filled;
            return 1000;
        }

        @Override
        public FluidStack drain(FluidStack resource, boolean doDrain) {
            FluidStack contents = contents();
            if (resource == null || contents == null || !contents.isFluidEqual(resource)) return null;
            return drain(resource.amount, doDrain);
        }

        @Override
        public FluidStack drain(int maxDrain, boolean doDrain) {
            FluidStack contents = contents();
            if (container.getCount() != 1 || maxDrain < 1000 || contents == null) return null;
            if (doDrain) container = new ItemStack(Items.BUCKET);
            return contents;
        }
    }

    /**
     * Places fluid from a handler into the world (a bucket's worth through the fluid's block: a Forge fluid block, vanilla water or lava,
     * or a plain block); fluids that vaporize do so where water does.
     */
    public static boolean tryPlaceFluid(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player, legacyconnector.v1_12_2.net.minecraft.world.World world,
            legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, IFluidHandler fluidSource, FluidStack resource) {
        if (world == null || resource == null || pos == null || fluidSource == null) return false;
        Fluid fluid = resource.getFluid();
        if (fluid == null || !fluid.canBePlacedInWorld()) return false;
        if (fluidSource.drain(resource, false) == null) return false;
        var state = world.getBlockState(pos);
        var material = state.getMaterial();
        boolean nonSolid = !material.isSolid();
        boolean replaceable = state.getBlock().isReplaceable(world, pos);
        if (!world.isAirBlock(pos) && !nonSolid && !replaceable) return false;
        var level = world.mcc$level();
        if (level != null && level.dimensionType().ultraWarm() && fluid.doesVaporize(resource)) {
            FluidStack result = fluidSource.drain(resource, true);
            if (result != null) {
                result.getFluid().vaporize(player, world, pos, result);
                return true;
            }
            return false;
        }
        destroyBlockOnFluidPlacement(world, pos);
        FluidStack result = tryFluidTransfer(mcc$blockHandler(fluid, world, pos), fluidSource, resource, true);
        if (result == null) return false;
        world.playSound(player, pos, fluid.getEmptySound(resource), legacyconnector.v1_12_2.net.minecraft.util.SoundCategory.BLOCKS, 1.0F, 1.0F);
        return true;
    }

    private static IFluidHandler mcc$blockHandler(Fluid fluid, legacyconnector.v1_12_2.net.minecraft.world.World world,
            legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        var block = fluid.getBlock();
        if (block instanceof IFluidBlock fluidBlock) return new legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.wrappers.FluidBlockWrapper(fluidBlock, world, pos);
        if (block instanceof legacyconnector.v1_12_2.net.minecraft.block.BlockLiquid liquid) {
            return new legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.wrappers.BlockLiquidWrapper(liquid, world, pos);
        }
        return new legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.wrappers.BlockWrapper(block, world, pos);
    }

    /** Breaks a non-solid, non-liquid block (a flower, grass) where fluid is placed, dropping it. */
    public static void destroyBlockOnFluidPlacement(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        if (world == null || world.isRemote) return;
        var state = world.getBlockState(pos);
        var material = state.getMaterial();
        if (!material.isSolid() && !material.isLiquid()) world.destroyBlock(pos, true);
    }

    /** Fills an empty container from the fluid block at a position (a source block of a mod fluid, vanilla water or lava). */
    public static FluidActionResult tryPickUpFluid(ItemStack emptyContainer, legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer playerIn,
            legacyconnector.v1_12_2.net.minecraft.world.World worldIn, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos,
            legacyconnector.v1_12_2.net.minecraft.util.EnumFacing side) {
        if (emptyContainer == null || emptyContainer.isEmpty() || worldIn == null || pos == null) return FluidActionResult.FAILURE;
        var block = worldIn.getBlockState(pos).getBlock();
        IFluidHandler target = null;
        if (block instanceof IFluidBlock fluidBlock) {
            target = new legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.wrappers.FluidBlockWrapper(fluidBlock, worldIn, pos);
        } else {
            var level = worldIn.mcc$level();
            var p = new net.minecraft.core.BlockPos(pos.getX(), pos.getY(), pos.getZ());
            if (level != null && !level.getFluidState(p).isEmpty()) target = new legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.wrappers.BlockWrapper.ModernLiquid(level, p);
        }
        if (target == null) return FluidActionResult.FAILURE;
        return tryFillContainer(emptyContainer, target, Integer.MAX_VALUE, playerIn, true);
    }
}
