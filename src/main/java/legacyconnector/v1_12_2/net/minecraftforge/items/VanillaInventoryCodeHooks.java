package legacyconnector.v1_12_2.net.minecraftforge.items;

import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntityDispenser;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** Forge's item handler hooks into vanilla's hoppers and droppers (mods' droppers call them to insert into item handlers). */
public class VanillaInventoryCodeHooks {
    /**
     * A dropper pushing one item of a slot into the item handler it faces: false when there is one (the item went in or stayed),
     * true when there is none and the dropper should drop the item instead.
     */
    public static boolean dropperInsertHook(World world, BlockPos pos, TileEntityDispenser dropper, int slot, ItemStack stack) {
        EnumFacing facing = EnumFacing.byIndex(dropper.getBlockMetadata() & 7);
        BlockPos targetPos = pos.offset(facing);
        TileEntity target = world.getTileEntity(targetPos);
        if (target == null) return true;
        EnumFacing side = facing.getOpposite();
        IItemHandler handler = target.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side)
                ? target.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side) : null;
        if (handler == null) return true;
        ItemStack dispensed = stack.copy();
        dispensed.setCount(1);
        ItemStack remainder = ItemHandlerHelper.insertItem(handler, dispensed, false);
        if (remainder.isEmpty()) {
            remainder = stack.copy();
            remainder.shrink(1);
        } else {
            remainder = stack.copy();
        }
        dropper.setInventorySlotContents(slot, remainder);
        return false;
    }

    /** The item handler (and its tile entity) of the block at a position on the given side, or null when it has none. */
    public static org.apache.commons.lang3.tuple.Pair<IItemHandler, Object> getItemHandler(World worldIn, double x, double y, double z, EnumFacing side) {
        if (worldIn == null) return null;
        TileEntity te = worldIn.getTileEntity(new BlockPos(x, y, z));
        if (te == null || !te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side)) return null;
        IItemHandler handler = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side);
        return handler == null ? null : org.apache.commons.lang3.tuple.ImmutablePair.of(handler, te);
    }
}
